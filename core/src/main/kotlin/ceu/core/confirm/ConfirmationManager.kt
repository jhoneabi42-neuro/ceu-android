package ceu.core.confirm

import ceu.core.action.ActionId
import ceu.core.clock.MonotonicClock
import ceu.core.digest.ActionDigest
import ceu.core.digest.toHex
import java.security.MessageDigest

class PendingConfirmation(
    val token: ByteArray,
    val action: ActionId,
    val actionHash: ByteArray,
    val issuedAtNanos: Long,
    val expiresAtNanos: Long,
) {
    fun tokenHex(): String = token.toHex()

    fun tokenPrefix(): String = tokenHex().take(8)

    fun copyOf(): PendingConfirmation = PendingConfirmation(
        token = token.copyOf(),
        action = action,
        actionHash = actionHash.copyOf(),
        issuedAtNanos = issuedAtNanos,
        expiresAtNanos = expiresAtNanos,
    )
}

sealed interface ConfirmOutcome {
    data class Accepted(val action: ActionId) : ConfirmOutcome

    data class Rejected(val code: String) : ConfirmOutcome
}

/**
 * Confirmação vinculada à ação canônica.
 * Token de 128 bits, hash SHA-256, validade de 15 s no relógio monotônico,
 * uso único, invalidação no cancelamento ou na troca/reemissão da ação.
 */
class ConfirmationManager(
    private val clock: MonotonicClock,
    private val tokens: TokenSource,
    private val ttlNanos: Long = TTL_NANOS,
) {
    private var pending: PendingConfirmation? = null

    fun peek(): PendingConfirmation? = pending?.copyOf()

    fun issue(action: ActionId): PendingConfirmation {
        val token = tokens.next128()
        require(token.size == TOKEN_BYTES) { "token deve ter 128 bits" }
        val hash = ActionDigest.sha256(action.canonical)
        require(hash.size == 32) { "hash SHA-256 deve ter 32 bytes" }
        val issuedAt = clock.nanoTime()
        val created = PendingConfirmation(
            token = token.copyOf(),
            action = action,
            actionHash = hash,
            issuedAtNanos = issuedAt,
            expiresAtNanos = issuedAt + ttlNanos,
        )
        pending = created.copyOf()
        return created.copyOf()
    }

    fun confirm(token: ByteArray, action: ActionId): ConfirmOutcome {
        val current = pending ?: return ConfirmOutcome.Rejected("NO_PENDING")
        if (clock.nanoTime() >= current.expiresAtNanos) {
            pending = null
            return ConfirmOutcome.Rejected("EXPIRED")
        }
        if (token.size != TOKEN_BYTES || !MessageDigest.isEqual(token, current.token)) {
            return ConfirmOutcome.Rejected("TOKEN_MISMATCH")
        }
        if (action != current.action) {
            return ConfirmOutcome.Rejected("ACTION_MISMATCH")
        }
        val recomputed = ActionDigest.sha256(action.canonical)
        if (!MessageDigest.isEqual(recomputed, current.actionHash)) {
            return ConfirmOutcome.Rejected("HASH_MISMATCH")
        }
        pending = null
        return ConfirmOutcome.Accepted(action)
    }

    fun clear(): Boolean {
        if (pending == null) return false
        pending = null
        return true
    }

    /**
     * Invalida a confirmação pendente quando a ação deixa de ser a mesma.
     * Retorna true se havia pendência e ela foi descartada.
     */
    fun invalidateIfActionChanged(action: ActionId): Boolean {
        val current = pending ?: return false
        if (current.action == action) return false
        pending = null
        return true
    }

    companion object {
        const val TOKEN_BYTES: Int = 16
        const val TTL_NANOS: Long = 15L * 1_000_000_000L
    }
}
