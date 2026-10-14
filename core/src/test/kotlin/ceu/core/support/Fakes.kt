package ceu.core.support

import ceu.core.clock.MonotonicClock
import ceu.core.confirm.TokenSource

class FakeClock(var now: Long = 0L) : MonotonicClock {
    override fun nanoTime(): Long = now

    fun advanceMillis(millis: Long) {
        now += millis * 1_000_000L
    }
}

class SequenceTokenSource : TokenSource {
    private var n: Int = 0

    override fun next128(): ByteArray {
        n += 1
        val token = ByteArray(16)
        token[0] = (n ushr 24).toByte()
        token[1] = (n ushr 16).toByte()
        token[2] = (n ushr 8).toByte()
        token[3] = n.toByte()
        token[15] = n.toByte()
        return token
    }
}
