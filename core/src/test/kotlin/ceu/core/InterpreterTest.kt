package ceu.core

import ceu.core.action.ActionId
import ceu.core.interpret.Interpreted
import ceu.core.interpret.Interpreter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InterpreterTest {
    private val interpreter = Interpreter()

    @Test
    fun `o mesmo texto produz o mesmo resultado`() {
        val first = interpreter.interpret("  Simular   Voltar ")
        val second = interpreter.interpret("  Simular   Voltar ")
        assertEquals(first, second)
        assertEquals(Interpreted.ActionProposal(ActionId.BACK), first)
    }

    @Test
    fun `reconhece a gramatica fechada`() {
        assertEquals(Interpreted.Help, interpreter.interpret("ajuda"))
        assertEquals(Interpreted.Help, interpreter.interpret("HELP"))
        assertEquals(Interpreted.Status, interpreter.interpret("status"))
        assertEquals(Interpreted.Echo("ceu fase 1"), interpreter.interpret("Eco  CÉU   Fase 1"))
        assertEquals(Interpreted.Echo(""), interpreter.interpret("eco"))
        assertEquals(Interpreted.ActionProposal(ActionId.HOME), interpreter.interpret("simular início"))
        assertEquals(
            Interpreted.ActionProposal(ActionId.OPEN_SETTINGS),
            interpreter.interpret("simular configurações"),
        )
        assertEquals(
            Interpreted.ActionProposal(ActionId.OPEN_NOTIFICATIONS),
            interpreter.interpret("simular notificações"),
        )
        assertEquals(Interpreted.ActionProposal(ActionId.RECENTS), interpreter.interpret("simular recents"))
    }

    @Test
    fun `rejeita o que nao esta na gramatica`() {
        assertEquals(Interpreted.Rejected("EMPTY"), interpreter.interpret("   "))
        assertEquals(Interpreted.Rejected("UNKNOWN"), interpreter.interpret("abrir configuracoes"))
        assertEquals(Interpreted.Rejected("UNKNOWN"), interpreter.interpret("ajuda por favor"))
        assertEquals(Interpreted.Rejected("ACTION_REQUIRED"), interpreter.interpret("simular"))
        assertEquals(Interpreted.Rejected("UNKNOWN_ACTION"), interpreter.interpret("simular wifi"))
        assertEquals(Interpreted.Rejected("UNKNOWN_ACTION"), interpreter.interpret("simular voltar agora"))
    }
}
