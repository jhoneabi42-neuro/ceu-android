package ceu.core.voice

/** Entrada de voz da Fase 1: somente texto enfileirado pela interface. */
interface VoiceInput {
    fun poll(): String?
}

/** Saída de voz da Fase 1: somente texto apresentado na tela. */
interface VoiceOutput {
    fun present(message: String)
    fun transcript(): String
}

class TextVoiceInput : VoiceInput {
    private val queue = ArrayDeque<String>()

    fun offer(text: String) {
        queue.addLast(text)
    }

    override fun poll(): String? = if (queue.isEmpty()) null else queue.removeFirst()
}

class ScreenVoiceOutput(
    private val capacity: Int = 12,
) : VoiceOutput {
    private val lines = ArrayDeque<String>()

    override fun present(message: String) {
        lines.addLast(message)
        while (lines.size > capacity) {
            lines.removeFirst()
        }
    }

    override fun transcript(): String = lines.joinToString(separator = "\n\n")

    fun lines(): List<String> = lines.toList()
}
