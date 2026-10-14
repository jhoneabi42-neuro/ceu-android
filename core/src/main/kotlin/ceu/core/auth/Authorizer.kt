package ceu.core.auth

import ceu.core.action.ActionId

sealed interface AuthorizationDecision {
    data object Allowed : AuthorizationDecision

    data class Denied(val code: String) : AuthorizationDecision
}

fun interface Authorizer {
    fun authorize(action: ActionId): AuthorizationDecision
}

/** Nega toda ação. É o autorizador da Fase 1. */
class DenyAllAuthorizer : Authorizer {
    override fun authorize(action: ActionId): AuthorizationDecision =
        AuthorizationDecision.Denied("DENIED_BY_DEFAULT")
}
