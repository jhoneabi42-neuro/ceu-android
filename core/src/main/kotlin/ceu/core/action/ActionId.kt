package ceu.core.action

/**
 * Ações simuláveis da Fase 1. [canonical] é a única forma aceita no hash.
 */
enum class ActionId {
    BACK,
    HOME,
    OPEN_SETTINGS,
    OPEN_NOTIFICATIONS,
    RECENTS,
    ;

    val canonical: String get() = "v1|$name"
}
