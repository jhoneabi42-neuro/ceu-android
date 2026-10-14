package ceu.core

import ceu.core.state.CeuState
import ceu.core.state.StateMachine
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StateMachineTest {
    @Test
    fun `aceita somente as transicoes listadas`() {
        val machine = StateMachine()
        assertEquals(CeuState.IDLE, machine.state)

        assertTrue(machine.transition(CeuState.INTERPRETING))
        assertTrue(machine.transition(CeuState.AWAITING_CONFIRMATION))
        assertTrue(machine.transition(CeuState.AUTHORIZING))
        assertTrue(machine.transition(CeuState.SIMULATING))
        assertTrue(machine.transition(CeuState.RESPONDING))
        assertTrue(machine.transition(CeuState.IDLE))
        assertEquals(CeuState.IDLE, machine.state)
        assertTrue(machine.rejections().isEmpty())
    }

    @Test
    fun `caminho de resposta e de negacao tambem e explicito`() {
        val info = StateMachine()
        assertTrue(info.transition(CeuState.INTERPRETING))
        assertTrue(info.transition(CeuState.RESPONDING))
        assertTrue(info.transition(CeuState.IDLE))

        val denied = StateMachine()
        assertTrue(denied.transition(CeuState.INTERPRETING))
        assertTrue(denied.transition(CeuState.AWAITING_CONFIRMATION))
        assertTrue(denied.transition(CeuState.INTERPRETING))
        assertTrue(denied.transition(CeuState.AWAITING_CONFIRMATION))
        assertTrue(denied.transition(CeuState.RESPONDING))
        assertTrue(denied.transition(CeuState.IDLE))

        val authDenied = StateMachine()
        assertTrue(authDenied.transition(CeuState.INTERPRETING))
        assertTrue(authDenied.transition(CeuState.AWAITING_CONFIRMATION))
        assertTrue(authDenied.transition(CeuState.AUTHORIZING))
        assertTrue(authDenied.transition(CeuState.RESPONDING))
        assertTrue(authDenied.transition(CeuState.IDLE))
    }

    @Test
    fun `rejeita transicao fora da lista e registra`() {
        val machine = StateMachine()
        assertFalse(machine.transition(CeuState.RESPONDING))
        assertEquals(CeuState.IDLE, machine.state)
        assertFalse(machine.transition(CeuState.IDLE))
        assertEquals(2, machine.rejections().size)
        assertEquals(CeuState.IDLE, machine.rejections()[0].from)
        assertEquals(CeuState.RESPONDING, machine.rejections()[0].to)
        assertEquals(CeuState.IDLE, machine.rejections()[1].to)
    }

    @Test
    fun `toda aresta ausente e rejeitada`() {
        val allowed = StateMachine.ALLOWED
        for (from in CeuState.entries) {
            for (to in CeuState.entries) {
                if (to in allowed.getValue(from)) continue
                val machine = StateMachine(from)
                assertFalse(machine.transition(to), "$from -> $to deveria ser rejeitada")
                assertEquals(from, machine.state)
                assertEquals(1, machine.rejections().size)
            }
        }
    }
}
