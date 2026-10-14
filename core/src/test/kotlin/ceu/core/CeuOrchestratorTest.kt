package ceu.core

import ceu.core.action.ActionId
import ceu.core.audit.AuditAgent
import ceu.core.audit.AuditKind
import ceu.core.auth.AuthorizationDecision
import ceu.core.auth.Authorizer
import ceu.core.auth.DenyAllAuthorizer
import ceu.core.confirm.ConfirmationManager
import ceu.core.confirm.ConfirmOutcome
import ceu.core.interpret.Interpreter
import ceu.core.orchestrate.CeuOrchestrator
import ceu.core.state.CeuState
import ceu.core.state.StateMachine
import ceu.core.support.FakeClock
import ceu.core.support.SequenceTokenSource
import ceu.core.voice.ScreenVoiceOutput
import ceu.core.voice.TextVoiceInput
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CeuOrchestratorTest {
    @Test
    fun `ajuda termina em idle e a auditoria nao leva o texto`() = runBlocking {
        val secret = "segredo-do-usuario-8841"
        val harness = Harness()
        harness.input.offer("eco $secret")
        val echoed = harness.orchestrator.consumeInput()
        assertTrue(echoed.response.contains(secret))
        assertEquals(CeuState.IDLE, echoed.state)
        assertFalse(harness.audit.serialized().contains(secret))

        val help = harness.orchestrator.submit("ajuda")
        assertTrue(help.response.contains("simular voltar"))
        assertEquals(CeuState.IDLE, help.state)
        assertFalse(harness.audit.serialized().contains(secret))
    }

    @Test
    fun `confirmacao aceita ainda e negada por padrao`() = runBlocking {
        val harness = Harness()
        val proposed = harness.orchestrator.submit("simular voltar")
        assertEquals(CeuState.AWAITING_CONFIRMATION, proposed.state)
        assertEquals("BACK", proposed.pending?.actionName)
        assertTrue(proposed.response.contains("DENIED").not())
        assertTrue(proposed.response.contains("nega por padrão"))

        val decided = harness.orchestrator.confirm()
        assertEquals(CeuState.IDLE, decided.state)
        assertNull(decided.pending)
        assertTrue(decided.response.contains("DENIED_BY_DEFAULT"))
        assertTrue(decided.response.contains("Nenhuma ação do sistema foi executada"))
        assertTrue(harness.audit.snapshot().any { it.kind == AuditKind.AUTHORIZATION_DENIED && it.actionId == "BACK" })
        assertFalse(harness.audit.snapshot().any { it.kind == AuditKind.SIMULATION_COMPLETED })

        val again = harness.orchestrator.confirm()
        assertEquals("Não há confirmação pendente.", again.response)
        assertEquals(CeuState.IDLE, again.state)
    }

    @Test
    fun `cancelar e expirar nao executam nada`() = runBlocking {
        val harness = Harness()
        harness.orchestrator.submit("Simular  Início")
        val cancelled = harness.orchestrator.cancel()
        assertEquals("Confirmação cancelada. Nada foi executado.", cancelled.response)
        assertEquals(CeuState.IDLE, cancelled.state)
        assertEquals("Não há confirmação para cancelar.", harness.orchestrator.cancel().response)

        harness.orchestrator.submit("simular recents")
        harness.clock.advanceMillis(15_000)
        val expired = harness.orchestrator.confirm()
        assertEquals("Confirmação expirada (15 s). Nada foi executado.", expired.response)
        assertEquals(CeuState.IDLE, expired.state)
    }

    @Test
    fun `comando informativo nao invalida a confirmacao pendente`() = runBlocking {
        val harness = Harness()
        harness.orchestrator.submit("simular configuracoes")
        val before = harness.confirmations.peek()
        val status = harness.orchestrator.submit("status")
        assertEquals(CeuState.AWAITING_CONFIRMATION, status.state)
        assertTrue(status.response.contains("AWAITING_CONFIRMATION"))
        assertTrue(status.response.contains("OPEN_SETTINGS"))
        assertTrue(before!!.token.contentEquals(harness.confirmations.peek()!!.token))
        val decided = harness.orchestrator.confirm()
        assertTrue(decided.response.contains("DENIED_BY_DEFAULT"))
    }

    @Test
    fun `troca de acao invalida o token anterior`() = runBlocking {
        val harness = Harness()
        harness.orchestrator.submit("simular voltar")
        val first = harness.confirmations.peek()!!
        harness.orchestrator.submit("simular home")
        val stale = harness.confirmations.confirm(first.token, ActionId.BACK)
        assertEquals(ConfirmOutcome.Rejected("TOKEN_MISMATCH"), stale)
        assertEquals(ActionId.HOME, harness.confirmations.peek()?.action)
        assertTrue(harness.audit.snapshot().any { it.code == "ACTION_CHANGED" })
    }

    @Test
    fun `autorizador que permite so simula`() = runBlocking {
        val harness = Harness(authorizer = Authorizer { AuthorizationDecision.Allowed })
        harness.orchestrator.submit("simular notificacoes")
        val done = harness.orchestrator.confirm()
        assertEquals(CeuState.IDLE, done.state)
        assertTrue(done.response.contains("Simulação concluída: OPEN_NOTIFICATIONS"))
        assertTrue(done.response.contains("Nenhuma ação real"))
        assertTrue(harness.audit.snapshot().any { it.code == "SIMULATED_ONLY" })
        assertFalse(harness.audit.serialized().contains("notificacoes"))
    }

    private class Harness(
        authorizer: Authorizer = DenyAllAuthorizer(),
    ) {
        val clock = FakeClock()
        val audit = AuditAgent(clock)
        val input = TextVoiceInput()
        val confirmations = ConfirmationManager(clock, SequenceTokenSource())
        val orchestrator = CeuOrchestrator(
            machine = StateMachine(),
            interpreter = Interpreter(),
            confirmations = confirmations,
            authorizer = authorizer,
            audit = audit,
            output = ScreenVoiceOutput(),
            input = input,
            clock = clock,
        )
    }
}
