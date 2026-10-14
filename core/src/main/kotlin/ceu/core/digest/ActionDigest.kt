package ceu.core.digest

import java.security.MessageDigest

object ActionDigest {
    fun sha256(canonical: String): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(canonical.toByteArray(Charsets.UTF_8))
    }
}

fun ByteArray.toHex(): String = joinToString(separator = "") { byte ->
    "%02x".format(byte.toInt() and 0xff)
}
