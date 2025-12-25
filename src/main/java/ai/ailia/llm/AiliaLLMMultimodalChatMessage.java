package ai.ailia.llm;

/**
 * Represents a multimodal chat message with media attachments.
 * Corresponds to the AILIALLMMultimodalChatMessage C structure.
 *
 * The content field can include <__media__> placeholders which will be
 * replaced with the corresponding media data from the mediaData array.
 */
public class AiliaLLMMultimodalChatMessage {
    /** The role of the message sender (system, user, or assistant) */
    public String role;

    /** The content of the message with <__media__> placeholders for media */
    public String content;

    /** Array of media data (images, audio) referenced by <__media__> markers */
    public AiliaLLMMediaData[] mediaData;

    /**
     * Creates a new multimodal chat message.
     *
     * @param role The role of the message sender (system, user, or assistant)
     * @param content The content with <__media__> placeholders
     * @param mediaData Array of media data referenced by placeholders
     */
    public AiliaLLMMultimodalChatMessage(String role, String content, AiliaLLMMediaData[] mediaData) {
        this.role = role;
        this.content = content;
        this.mediaData = mediaData;
    }

    /**
     * Creates a new multimodal chat message with a single media attachment.
     *
     * @param role The role of the message sender (system, user, or assistant)
     * @param content The content with <__media__> placeholder
     * @param media Single media data
     */
    public AiliaLLMMultimodalChatMessage(String role, String content, AiliaLLMMediaData media) {
        this.role = role;
        this.content = content;
        this.mediaData = new AiliaLLMMediaData[] { media };
    }

    /**
     * Creates a new text-only multimodal chat message.
     *
     * @param role The role of the message sender (system, user, or assistant)
     * @param content The content of the message
     */
    public AiliaLLMMultimodalChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
        this.mediaData = null;
    }

    @Override
    public String toString() {
        int mediaCount = (mediaData != null) ? mediaData.length : 0;
        return String.format("[%s]: %s (media: %d)", role, content, mediaCount);
    }
}
