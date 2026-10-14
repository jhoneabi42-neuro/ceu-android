package ceu.core.audit

import ceu.core.clock.MonotonicClock
import ceu.core.state.CeuState

enum class AuditKind {
    TRANSITION,
    COMMAND_INTERPRETED,
    CONFIRMATION_ISSUED,
    CONFIRMATION_ACCEPTED,
    CONFIRMATION_REJECTED,
    CONFIRMATION_CANCELLED,
    CONFIRMATION_INVALIDATED,
    AUTHORIZATION_DENIED,
    AUTHORIZATION_ALLOWED,
    SIMULATION_COMPLETED,
}

/**
 * Registro só em memória. A API não aceita texto do usuário — apenas estado,
 * id de ação e código de máquina.
 */
data class AuditRecord(
    val sequence: Long,
    val atNanos: Long,
    val kind: AuditKind,
    val fromState: CeuState?,
    val toState: CeuState?,
    val actionId: String?,
    val code: String,
)

class AuditAgent(
    private val clock: MonotonicClock,
) {
    private val records = mutableListOf<AuditRecord>()
    private var sequence: Long = 0

    fun record(
        kind: AuditKind,
        fromState: CeuState?,
        toState: CeuState?,
        actionId: String?,
        code: String,
    ): AuditRecord {
        sequence += 1
        val entry = AuditRecord(
            sequence = sequence,
            atNanos = clock.nanoTime(),
            kind = kind,
            fromState = fromState,
            toState = toState,
            actionId = actionId,
            code = code,
        )
        records += entry
        return entry
    }

    fun snapshot(): List<AuditRecord> = records.toList()

    fun serialized(): String = snapshot().joinToString(separator = "\n") { record ->
        listOf(
            record.sequence.toString(),
            record.atNanos.toString(),
            record.kind.name,
            record.fromState?.name ?: "-",
            record.toState?.name ?: "-",
            record.actionId ?: "-",
            record.code,
        ).joinToString("|")
    }
}
