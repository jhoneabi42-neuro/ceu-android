package ceu.core

import ceu.core.audit.AuditAgent
import ceu.core.audit.AuditKind
import ceu.core.state.CeuState
import ceu.core.support.FakeClock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class AuditAgentTest {
    @Test
    fun `grava so codigo de maquina em memoria e avanca o relogio`() {
        val clock = FakeClock(10)
        val audit = AuditAgent(clock)
        audit.record(AuditKind.COMMAND_INTERPRETED, CeuState.IDLE, CeuState.IDLE, null, "ECHO")
        clock.advanceMillis(5)
        audit.record(AuditKind.AUTHORIZATION_DENIED, CeuState.AUTHORIZING, CeuState.AUTHORIZING, "BACK", "DENIED_BY_DEFAULT")
        val snapshot = audit.snapshot()
        assertEquals(2, snapshot.size)
        assertEquals(1L, snapshot[0].sequence)
        assertEquals(2L, snapshot[1].sequence)
        assertEquals(10L, snapshot[0].atNanos)
        assertEquals(10L + 5_000_000L, snapshot[1].atNanos)
        assertFalse(audit.serialized().contains("texto do usuario"))
    }
}
