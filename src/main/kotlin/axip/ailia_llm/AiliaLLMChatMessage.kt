package axip.ailia_llm

/**
 * Represents a chat message for AiliaLLM.
 * Each message has a role (system, user, assistant, or tool) and content.
 * For tool use, a "tool" message holds the tool result as its content, see [AiliaLLM.setTools].
 *
 * @property role The role of the message sender (system, user, assistant, or tool)
 * @property content The content of the message
 */
data class AiliaLLMChatMessage(
    @JvmField val role: String,
    @JvmField val content: String
) {
    override fun toString(): String = "[$role]: $content"
}
