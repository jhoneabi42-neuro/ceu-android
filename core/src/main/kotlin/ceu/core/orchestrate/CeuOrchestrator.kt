package ceu.core.orchestrate

import ceu.core.action.ActionId
import ceu.core.audit.AuditAgent
import ceu.core.audit.AuditKind
import ceu.core.auth.AuthorizationDecision
import ceu.core.auth.Authorizer
import ceu.core.clock.MonotonicClock
import ceu.core.confirm.ConfirmationManager
import ceu.core.confirm.ConfirmOutcome
import ceu.core.confirm.PendingConfirmation
import ceu.core.interpret.Interpreted
import ceu.core.interpret.Interpreter
import ceu.core.state.CeuState
import ceu.core.state.StateMachine
import ceu.core.text.CeuTexts
import ceu.core.voice.ScreenVoiceOutput
import ceu.core.voice.TextVoiceInput
import kotlinx.coroutines.yield

data class PendingView(
    val actionName: String,
    val expiresAtNanos: Long,
    val tokenPrefix: String,
)

data class TurnResult(
    val response: String,
    val state: CeuState,
    val pending: PendingView?,
    val transcript: String,
)

/**
 * Liga interpretação, confirmação, autorização (nega por padrão) e auditoria.
 * Nenhuma chamada sai do processo: ação autorizada só gera texto de simulação.
 */
class CeuOrchestrator(
    private val machine: StateMachine,
    private val interpreter: Interpreter,
    private val confirmations: ConfirmationManager,
    private val authorizer: Authorizer,
    private val audit: AuditAgent,
    private val output: ScreenVoiceOutput,
    private val input: TextVoiceInput,
    private val clock: MonotonicClock,
) {
    val state: CeuState get() = machine.state

    suspend fun consumeInput(): TurnResult {
        yield()
        val text = input.poll() ?: return presentStay(CeuTexts.empty())
        return performSubmit(text)
    }

    suspend fun submit(text: String): TurnResult {
        yield()
        return performSubmit(text)
    }

    suspend fun confirm(): TurnResult {
        yield()
        if (machine.state != CeuState.AWAITING_CONFIRMATION) {
            audit.record(
                kind = AuditKind.CONFIRMATION_REJECTED,
                fromState = machine.state,
                toState = machine.state,
                actionId = null,
                code = "NO_PENDING",
            )
            return presentStay(CeuTexts.noPending())
        }
        val pending = confirmations.peek()
        go(CeuState.AUTHORIZING)
        if (pending == null) {
            audit.record(
                kind = AuditKind.CONFIRMATION_REJECTED,
                fromState = machine.state,
                toState = machine.state,
                actionId = null,
                code = "NO_PENDING",
            )
            return respond(CeuTexts.noPending())
        }
        return when (val outcome = confirmations.confirm(pending.token, pending.action)) {
            is ConfirmOutcome.Rejected -> {
                audit.record(
                    kind = AuditKind.CONFIRMATION_REJECTED,
                    fromState = CeuState.AUTHORIZING,
                    toState = CeuState.AUTHORIZING,
                    actionId = pending.action.name,
                    code = outcome.code,
                )
                respond(CeuTexts.rejected(outcome.code))
            }
            is ConfirmOutcome.Accepted -> {
                audit.record(
                    kind = AuditKind.CONFIRMATION_ACCEPTED,
                    fromState = CeuState.AUTHORIZING,
                    toState = CeuState.AUTHORIZING,
                    actionId = outcome.action.name,
                    code = "ACCEPTED",
                )
                authorizeAndFinish(outcome.action)
            }
        }
    }

    suspend fun cancel(): TurnResult {
        yield()
        if (machine.state != CeuState.AWAITING_CONFIRMATION) {
            audit.record(
                kind = AuditKind.CONFIRMATION_REJECTED,
                fromState = machine.state,
                toState = machine.state,
                actionId = null,
                code = "NOTHING_TO_CANCEL",
            )
            return presentStay(CeuTexts.nothingToCancel())
        }
        val pending = confirmations.peek()
        val cleared = confirmations.clear()
        audit.record(
            kind = if (cleared) AuditKind.CONFIRMATION_CANCELLED else AuditKind.CONFIRMATION_REJECTED,
            fromState = CeuState.AWAITING_CONFIRMATION,
            toState = CeuState.AWAITING_CONFIRMATION,
            actionId = pending?.action?.name,
            code = if (cleared) "CANCELLED" else "NOTHING_TO_CANCEL",
        )
        go(CeuState.RESPONDING)
        val message = if (cleared) CeuTexts.cancelled() else CeuTexts.nothingToCancel()
        output.present(message)
        go(CeuState.IDLE)
        return snapshot(message)
    }

    fun pendingView(): PendingView? = confirmations.peek()?.toView()

    fun remainingSeconds(pending: PendingView, nowNanos: Long = clock.nanoTime()): Long {
        val left = pending.expiresAtNanos - nowNanos
        if (left <= 0L) return 0L
        return (left + 1_000_000_000L - 1L) / 1_000_000_000L
    }

    private fun performSubmit(text: String): TurnResult {
        check(machine.state == CeuState.IDLE || machine.state == CeuState.AWAITING_CONFIRMATION) {
            "submit fora de IDLE/AWAITING: ${machine.state}"
        }
        val interpreted = interpreter.interpret(text)
        auditCommand(interpreted)
        if (interpreted is Interpreted.ActionProposal) {
            return beginAction(interpreted.action)
        }
        val message = messageFor(interpreted)
        if (machine.state == CeuState.AWAITING_CONFIRMATION) {
            return presentStay(message)
        }
        go(CeuState.INTERPRETING)
        return respond(message)
    }

    private fun beginAction(action: ActionId): TurnResult {
        if (machine.state == CeuState.AWAITING_CONFIRMATION) {
            val current = confirmations.peek()
            if (current != null) {
                confirmations.clear()
                audit.record(
                    kind = AuditKind.CONFIRMATION_INVALIDATED,
                    fromState = machine.state,
                    toState = machine.state,
                    actionId = current.action.name,
                    code = if (current.action == action) "REISSUED" else "ACTION_CHANGED",
                )
            }
        }
        go(CeuState.INTERPRETING)
        val issued = confirmations.issue(action)
        audit.record(
            kind = AuditKind.CONFIRMATION_ISSUED,
            fromState = CeuState.INTERPRETING,
            toState = CeuState.INTERPRETING,
            actionId = action.name,
            code = "ISSUED",
        )
        go(CeuState.AWAITING_CONFIRMATION)
        val message = CeuTexts.proposed(action, issued.tokenPrefix())
        output.present(message)
        return snapshot(message)
    }

    private fun authorizeAndFinish(action: ActionId): TurnResult {
        return when (val decision = authorizer.authorize(action)) {
            is AuthorizationDecision.Denied -> {
                audit.record(
                    kind = AuditKind.AUTHORIZATION_DENIED,
                    fromState = CeuState.AUTHORIZING,
                    toState = CeuState.AUTHORIZING,
                    actionId = action.name,
                    code = decision.code,
                )
                respond(CeuTexts.denied(action))
            }
            AuthorizationDecision.Allowed -> {
                audit.record(
                    kind = AuditKind.AUTHORIZATION_ALLOWED,
                    fromState = CeuState.AUTHORIZING,
                    toState = CeuState.AUTHORIZING,
                    actionId = action.name,
                    code = "ALLOWED",
                )
                go(CeuState.SIMULATING)
                audit.record(
                    kind = AuditKind.SIMULATION_COMPLETED,
                    fromState = CeuState.SIMULATING,
                    toState = CeuState.SIMULATING,
                    actionId = action.name,
                    code = "SIMULATED_ONLY",
                )
                respond(CeuTexts.simulated(action))
            }
        }
    }

    private fun messageFor(interpreted: Interpreted): String = when (interpreted) {
        Interpreted.Help -> CeuTexts.HELP
        Interpreted.Status -> statusMessage()
        is Interpreted.Echo -> CeuTexts.echo(interpreted.normalizedRemainder)
        is Interpreted.Rejected -> CeuTexts.rejected(interpreted.code)
        is Interpreted.ActionProposal -> error("ação tratada à parte")
    }

    private fun statusMessage(): String {
        val pending = confirmations.peek()
        val remaining = pending?.let { item ->
            val left = item.expiresAtNanos - clock.nanoTime()
            if (left <= 0L) 0L else (left + 1_000_000_000L - 1L) / 1_000_000_000L
        }
        return CeuTexts.status(machine.state, pending?.action?.name, remaining)
    }

    private fun respond(message: String): TurnResult {
        when (machine.state) {
            CeuState.INTERPRETING, CeuState.AUTHORIZING, CeuState.SIMULATING -> go(CeuState.RESPONDING)
            else -> error("respond a partir de ${machine.state}")
        }
        output.present(message)
        go(CeuState.IDLE)
        return snapshot(message)
    }

    private fun presentStay(message: String): TurnResult {
        output.present(message)
        return snapshot(message)
    }

    private fun go(to: CeuState) {
        val from = machine.state
        val accepted = machine.transition(to)
        check(accepted) { "transição não listada: $from -> $to" }
        audit.record(
            kind = AuditKind.TRANSITION,
            fromState = from,
            toState = to,
            actionId = null,
            code = "${from.name}_TO_${to.name}",
        )
    }

    private fun auditCommand(interpreted: Interpreted) {
        val (code, actionId) = when (interpreted) {
            Interpreted.Help -> "HELP" to null
            Interpreted.Status -> "STATUS" to null
            is Interpreted.Echo -> "ECHO" to null
            is Interpreted.ActionProposal -> "ACTION" to interpreted.action.name
            is Interpreted.Rejected -> interpreted.code to null
        }
        audit.record(
            kind = AuditKind.COMMAND_INTERPRETED,
            fromState = machine.state,
            toState = machine.state,
            actionId = actionId,
            code = code,
        )
    }

    private fun snapshot(response: String): TurnResult = TurnResult(
        response = response,
        state = machine.state,
        pending = confirmations.peek()?.toView(),
        transcript = output.transcript(),
    )

    private fun PendingConfirmation.toView(): PendingView = PendingView(
        actionName = action.name,
        expiresAtNanos = expiresAtNanos,
        tokenPrefix = tokenPrefix(),
    )
}
