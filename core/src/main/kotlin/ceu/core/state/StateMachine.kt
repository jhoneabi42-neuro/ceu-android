package ceu.core.state

data class RejectedTransition(
    val from: CeuState,
    val to: CeuState,
)

/**
 * Máquina explícita. Transição fora da lista é rejeitada, o estado não muda
 * e a rejeição fica registrada.
 */
class StateMachine(
    initial: CeuState = CeuState.IDLE,
) {
    var state: CeuState = initial
        private set

    private val rejections = mutableListOf<RejectedTransition>()

    fun rejections(): List<RejectedTransition> = rejections.toList()

    fun transition(to: CeuState): Boolean {
        val from = state
        val allowed = ALLOWED[from].orEmpty()
        if (to !in allowed) {
            rejections += RejectedTransition(from, to)
            return false
        }
        state = to
        return true
    }

    companion object {
        val ALLOWED: Map<CeuState, Set<CeuState>> = mapOf(
            CeuState.IDLE to setOf(CeuState.INTERPRETING),
            CeuState.INTERPRETING to setOf(
                CeuState.AWAITING_CONFIRMATION,
                CeuState.RESPONDING,
            ),
            CeuState.AWAITING_CONFIRMATION to setOf(
                CeuState.AUTHORIZING,
                CeuState.RESPONDING,
                CeuState.INTERPRETING,
            ),
            CeuState.AUTHORIZING to setOf(
                CeuState.SIMULATING,
                CeuState.RESPONDING,
            ),
            CeuState.SIMULATING to setOf(CeuState.RESPONDING),
            CeuState.RESPONDING to setOf(CeuState.IDLE),
        )
    }
}
