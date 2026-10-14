package ceu.core

import ceu.core.action.ActionId
import ceu.core.auth.AuthorizationDecision
import ceu.core.auth.DenyAllAuthorizer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DenyAllAuthorizerTest {
    @Test
    fun `nega toda acao por padrao`() {
        val authorizer = DenyAllAuthorizer()
        for (action in ActionId.entries) {
            assertEquals(
                AuthorizationDecision.Denied("DENIED_BY_DEFAULT"),
                authorizer.authorize(action),
            )
        }
    }
}
