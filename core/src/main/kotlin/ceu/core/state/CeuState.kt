package ceu.core.state

enum class CeuState {
    IDLE,
    INTERPRETING,
    AWAITING_CONFIRMATION,
    AUTHORIZING,
    SIMULATING,
    RESPONDING,
}
