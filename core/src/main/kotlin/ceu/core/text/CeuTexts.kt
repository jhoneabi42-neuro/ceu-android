package ceu.core.text

import ceu.core.action.ActionId
import ceu.core.state.CeuState

object CeuTexts {
    val HELP: String = """
        Comandos da Fase 1 — somente texto, ações simuladas:
        • ajuda
        • status
        • eco <texto>
        • simular voltar
        • simular inicio
        • simular configuracoes
        • simular notificacoes
        • simular recentes
        Confirmação vinculada à ação: 15 s, uso único. Autorização nega por padrão.
    """.trimIndent()

    fun status(state: CeuState, pendingAction: String?, remainingSeconds: Long?): String {
        val pending = when {
            pendingAction == null -> "nenhuma"
            remainingSeconds == null || remainingSeconds <= 0L -> "$pendingAction (expirada)"
            else -> "$pendingAction (${remainingSeconds}s)"
        }
        return "Estado: ${state.name}. Confirmação pendente: $pending."
    }

    fun echo(normalizedRemainder: String): String =
        if (normalizedRemainder.isEmpty()) "Eco: (vazio)" else "Eco: $normalizedRemainder"

    fun empty(): String = "Entrada vazia. Envie \"ajuda\"."

    fun unknown(): String = "Comando não reconhecido. Envie \"ajuda\"."

    fun actionRequired(): String = "Informe a ação simulada. Exemplo: simular voltar."

    fun unknownAction(): String = "Ação simulada desconhecida. Envie \"ajuda\"."

    fun proposed(action: ActionId, tokenPrefix: String): String =
        "Ação simulada preparada: ${action.name} (ref $tokenPrefix). " +
            "Nada foi executado. Confirme em até 15 s ou cancele. " +
            "A autorização desta fase nega por padrão."

    fun denied(action: ActionId): String =
        "Confirmação aceita para ${action.name} e consumida. " +
            "Autorização: negada por padrão (DENIED_BY_DEFAULT). " +
            "Nenhuma ação do sistema foi executada."

    fun simulated(action: ActionId): String =
        "Simulação concluída: ${action.name}. Nenhuma ação real do Android foi executada."

    fun expired(): String = "Confirmação expirada (15 s). Nada foi executado."

    fun tokenMismatch(): String = "Confirmação rejeitada (token inválido). Nada foi executado."

    fun actionMismatch(): String = "Confirmação rejeitada (ação diferente). Nada foi executado."

    fun hashMismatch(): String = "Confirmação rejeitada (hash da ação). Nada foi executado."

    fun noPending(): String = "Não há confirmação pendente."

    fun cancelled(): String = "Confirmação cancelada. Nada foi executado."

    fun nothingToCancel(): String = "Não há confirmação para cancelar."

    fun rejected(code: String): String = when (code) {
        "EMPTY" -> empty()
        "UNKNOWN" -> unknown()
        "ACTION_REQUIRED" -> actionRequired()
        "UNKNOWN_ACTION" -> unknownAction()
        "EXPIRED" -> expired()
        "TOKEN_MISMATCH" -> tokenMismatch()
        "ACTION_MISMATCH" -> actionMismatch()
        "HASH_MISMATCH" -> hashMismatch()
        "NO_PENDING" -> noPending()
        else -> unknown()
    }
}
