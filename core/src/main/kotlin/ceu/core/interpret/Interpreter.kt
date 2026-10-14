package ceu.core.interpret

import ceu.core.action.ActionId
import java.text.Normalizer
import java.util.Locale

/**
 * Gramática fechada e determinística. Sem fuzzy match e sem depender do locale do aparelho.
 */
class Interpreter {
    fun interpret(raw: String): Interpreted {
        val normalized = normalize(raw)
        if (normalized.isEmpty()) return Interpreted.Rejected("EMPTY")
        val space = normalized.indexOf(' ')
        val verb = if (space < 0) normalized else normalized.substring(0, space)
        val rest = if (space < 0) "" else normalized.substring(space + 1)
        return when (verb) {
            "ajuda", "help" -> if (rest.isEmpty()) Interpreted.Help else Interpreted.Rejected("UNKNOWN")
            "status" -> if (rest.isEmpty()) Interpreted.Status else Interpreted.Rejected("UNKNOWN")
            "eco", "echo" -> Interpreted.Echo(rest)
            "simular", "simulate" -> interpretAction(rest)
            else -> Interpreted.Rejected("UNKNOWN")
        }
    }

    private fun interpretAction(rest: String): Interpreted {
        if (rest.isEmpty() || rest.contains(' ')) {
            return Interpreted.Rejected(if (rest.isEmpty()) "ACTION_REQUIRED" else "UNKNOWN_ACTION")
        }
        val action = ACTIONS[rest] ?: return Interpreted.Rejected("UNKNOWN_ACTION")
        return Interpreted.ActionProposal(action)
    }

    companion object {
        private val ACTIONS: Map<String, ActionId> = mapOf(
            "voltar" to ActionId.BACK,
            "back" to ActionId.BACK,
            "inicio" to ActionId.HOME,
            "home" to ActionId.HOME,
            "configuracoes" to ActionId.OPEN_SETTINGS,
            "settings" to ActionId.OPEN_SETTINGS,
            "notificacoes" to ActionId.OPEN_NOTIFICATIONS,
            "notifications" to ActionId.OPEN_NOTIFICATIONS,
            "recentes" to ActionId.RECENTS,
            "recents" to ActionId.RECENTS,
        )

        fun normalize(raw: String): String {
            val decomposed = Normalizer.normalize(raw.trim(), Normalizer.Form.NFD)
            val stripped = buildString(decomposed.length) {
                for (ch in decomposed) {
                    if (Character.getType(ch) != Character.NON_SPACING_MARK.toInt()) {
                        append(ch)
                    }
                }
            }
            return stripped.lowercase(Locale.ROOT).replace(WHITESPACE, " ")
        }

        private val WHITESPACE = Regex("\\s+")
    }
}
