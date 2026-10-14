package ceu.core.confirm

import java.security.SecureRandom

fun interface TokenSource {
    /** Exatamente 16 bytes (128 bits). */
    fun next128(): ByteArray
}

class SecureTokenSource : TokenSource {
    private val random = SecureRandom()

    override fun next128(): ByteArray = ByteArray(TOKEN_BYTES).also(random::nextBytes)

    companion object {
        const val TOKEN_BYTES: Int = 16
    }
}
