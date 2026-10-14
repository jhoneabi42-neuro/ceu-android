package ceu.core.interpret

import ceu.core.action.ActionId

sealed interface Interpreted {
    data object Help : Interpreted

    data object Status : Interpreted

    data class Echo(val normalizedRemainder: String) : Interpreted

    data class ActionProposal(val action: ActionId) : Interpreted

    data class Rejected(val code: String) : Interpreted
}
