package ai.ailia.llm;

/**
 * Represents a chat message for AiliaLLM.
 * Each message has a role (system, user, or assistant) and content.
 */
public class AiliaLLMChatMessage {
    /** The role of the message sender (system, user, or assistant) */
    public String role;

    /** The content of the message */
    public String content;

    /**
     * Creates a new chat message.
     *
     * @param role The role of the message sender (system, user, or assistant)
     * @param content The content of the message
     */
    public AiliaLLMChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    @Override
    public String toString() {
        return String.format("[%s]: %s", role, content);
    }
}
