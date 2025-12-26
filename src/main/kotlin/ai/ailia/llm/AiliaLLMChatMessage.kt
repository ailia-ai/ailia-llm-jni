package ai.ailia.llm

/**
 * Represents a chat message for AiliaLLM.
 * Each message has a role (system, user, or assistant) and content.
 *
 * @property role The role of the message sender (system, user, or assistant)
 * @property content The content of the message
 */
data class AiliaLLMChatMessage(
    @JvmField val role: String,
    @JvmField val content: String
) {
    override fun toString(): String = "[$role]: $content"
}
