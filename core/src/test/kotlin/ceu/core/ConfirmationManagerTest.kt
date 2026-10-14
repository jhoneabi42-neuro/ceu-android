package ceu.core

import ceu.core.action.ActionId
import ceu.core.confirm.ConfirmOutcome
import ceu.core.confirm.ConfirmationManager
import ceu.core.digest.ActionDigest
import ceu.core.support.FakeClock
import ceu.core.support.SequenceTokenSource
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.security.MessageDigest

class ConfirmationManagerTest {
    private val clock = FakeClock()
    private val manager = ConfirmationManager(clock, SequenceTokenSource())

    @Test
    fun `emite token de 128 bits e hash sha-256 da acao canonica`() {
        val pending = manager.issue(ActionId.BACK)
        assertEquals(16, pending.token.size)
        assertArrayEquals(ActionDigest.sha256("v1|BACK"), pending.actionHash)
        assertEquals(
            "472400d54bfada07f0b9e459b9a81bc3b99e8eab2e57a4cbd1d488a3df518299",
            pending.actionHash.joinToString("") { "%02x".format(it.toInt() and 0xff) },
        )
        assertEquals(ConfirmationManager.TTL_NANOS, pending.expiresAtNanos - pending.issuedAtNanos)
    }

    @Test
    fun `uso unico dentro da janela`() {
        val pending = manager.issue(ActionId.HOME)
        clock.advanceMillis(14_999)
        val first = manager.confirm(pending.token, ActionId.HOME)
        assertTrue(first is ConfirmOutcome.Accepted)
        val second = manager.confirm(pending.token, ActionId.HOME)
        assertEquals(ConfirmOutcome.Rejected("NO_PENDING"), second)
    }

    @Test
    fun `expira aos 15 segundos exatos`() {
        val pending = manager.issue(ActionId.RECENTS)
        clock.now = pending.expiresAtNanos
        assertEquals(ConfirmOutcome.Rejected("EXPIRED"), manager.confirm(pending.token, ActionId.RECENTS))
        assertEquals(null, manager.peek())
    }

    @Test
    fun `token errado nao consome`() {
        val pending = manager.issue(ActionId.BACK)
        val wrong = pending.token.copyOf().also { it[0] = (it[0].toInt() xor 0xff).toByte() }
        assertEquals(ConfirmOutcome.Rejected("TOKEN_MISMATCH"), manager.confirm(wrong, ActionId.BACK))
        assertTrue(manager.confirm(pending.token, ActionId.BACK) is ConfirmOutcome.Accepted)
    }

    @Test
    fun `acao diferente nao consome e invalidateIfActionChanged descarta`() {
        val pending = manager.issue(ActionId.BACK)
        assertEquals(
            ConfirmOutcome.Rejected("ACTION_MISMATCH"),
            manager.confirm(pending.token, ActionId.HOME),
        )
        assertFalse(manager.invalidateIfActionChanged(ActionId.BACK))
        assertTrue(manager.invalidateIfActionChanged(ActionId.HOME))
        assertEquals(ConfirmOutcome.Rejected("NO_PENDING"), manager.confirm(pending.token, ActionId.BACK))
    }

    @Test
    fun `cancelamento e reemissao invalidam o token anterior`() {
        val first = manager.issue(ActionId.OPEN_SETTINGS)
        assertTrue(manager.clear())
        assertEquals(ConfirmOutcome.Rejected("NO_PENDING"), manager.confirm(first.token, ActionId.OPEN_SETTINGS))

        val again = manager.issue(ActionId.OPEN_NOTIFICATIONS)
        val replaced = manager.issue(ActionId.OPEN_NOTIFICATIONS)
        assertFalse(MessageDigest.isEqual(again.token, replaced.token))
        assertNotEquals(again.tokenPrefix(), replaced.tokenPrefix())
        assertEquals(
            ConfirmOutcome.Rejected("TOKEN_MISMATCH"),
            manager.confirm(again.token, ActionId.OPEN_NOTIFICATIONS),
        )
        assertTrue(manager.confirm(replaced.token, ActionId.OPEN_NOTIFICATIONS) is ConfirmOutcome.Accepted)
    }
}
