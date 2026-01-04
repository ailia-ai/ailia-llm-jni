package axip.ailia_llm

/**
 * Represents a multimodal chat message with media attachments.
 * Corresponds to the AILIALLMMultimodalChatMessage C structure.
 *
 * The content field can include <__media__> placeholders which will be
 * replaced with the corresponding media data from the mediaData array.
 *
 * @property role The role of the message sender (system, user, or assistant)
 * @property content The content of the message with <__media__> placeholders for media
 * @property mediaData Array of media data (images, audio) referenced by <__media__> markers
 */
data class AiliaLLMMultimodalChatMessage @JvmOverloads constructor(
    @JvmField val role: String,
    @JvmField val content: String,
    @JvmField val mediaData: Array<AiliaLLMMediaData>? = null
) {
    /**
     * Creates a new multimodal chat message with a single media attachment.
     *
     * @param role The role of the message sender (system, user, or assistant)
     * @param content The content with <__media__> placeholder
     * @param media Single media data
     */
    constructor(role: String, content: String, media: AiliaLLMMediaData) :
            this(role, content, arrayOf(media))

    override fun toString(): String {
        val mediaCount = mediaData?.size ?: 0
        return "[$role]: $content (media: $mediaCount)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AiliaLLMMultimodalChatMessage

        if (role != other.role) return false
        if (content != other.content) return false
        if (mediaData != null) {
            if (other.mediaData == null) return false
            if (!mediaData.contentEquals(other.mediaData)) return false
        } else if (other.mediaData != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = role.hashCode()
        result = 31 * result + content.hashCode()
        result = 31 * result + (mediaData?.contentHashCode() ?: 0)
        return result
    }
}
