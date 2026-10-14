package ceu.core.clock

/**
 * Relógio monotônico injetável. Produção usa [System.nanoTime], nunca relógio de parede.
 */
fun interface MonotonicClock {
    fun nanoTime(): Long
}

class SystemMonotonicClock : MonotonicClock {
    override fun nanoTime(): Long = System.nanoTime()
}
