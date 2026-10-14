package ceu.app

import ceu.core.audit.AuditAgent
import ceu.core.auth.DenyAllAuthorizer
import ceu.core.clock.MonotonicClock
import ceu.core.clock.SystemMonotonicClock
import ceu.core.confirm.ConfirmationManager
import ceu.core.confirm.SecureTokenSource
import ceu.core.interpret.Interpreter
import ceu.core.orchestrate.CeuOrchestrator
import ceu.core.state.StateMachine
import ceu.core.voice.ScreenVoiceOutput
import ceu.core.voice.TextVoiceInput

class CeuSession(
    val orchestrator: CeuOrchestrator,
    val input: TextVoiceInput,
    val output: ScreenVoiceOutput,
    val audit: AuditAgent,
    val clock: MonotonicClock,
)

object CeuRuntime {
    fun session(): CeuSession {
        val clock = SystemMonotonicClock()
        val audit = AuditAgent(clock)
        val input = TextVoiceInput()
        val output = ScreenVoiceOutput()
        val orchestrator = CeuOrchestrator(
            machine = StateMachine(),
            interpreter = Interpreter(),
            confirmations = ConfirmationManager(clock, SecureTokenSource()),
            authorizer = DenyAllAuthorizer(),
            audit = audit,
            output = output,
            input = input,
            clock = clock,
        )
        return CeuSession(
            orchestrator = orchestrator,
            input = input,
            output = output,
            audit = audit,
            clock = clock,
        )
    }
}
