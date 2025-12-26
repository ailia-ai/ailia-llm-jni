package ai.ailia.llm

/**
 * Represents media data (image or audio) for multimodal LLM processing.
 * Corresponds to the AILIALLMMediaData C structure.
 *
 * @property mediaType Media type: "image" or "audio" (audio is reserved for future use)
 * @property filePath Path to the media file (UTF-8)
 * @property data Raw media data (currently unsupported, reserved for future use)
 * @property dataSize Size of the raw data
 * @property width Width for images (pixels), sample count for audio
 * @property height Height for images (pixels), unused for audio (set to 0)
 */
data class AiliaLLMMediaData @JvmOverloads constructor(
    @JvmField val mediaType: String,
    @JvmField val filePath: String,
    @JvmField val width: Int = 0,
    @JvmField val height: Int = 0,
    @JvmField val data: ByteArray? = null,
    @JvmField val dataSize: Int = 0
) {
    override fun toString(): String =
        "AiliaLLMMediaData[type=$mediaType, path=$filePath, size=${width}x${height}]"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AiliaLLMMediaData

        if (mediaType != other.mediaType) return false
        if (filePath != other.filePath) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (data != null) {
            if (other.data == null) return false
            if (!data.contentEquals(other.data)) return false
        } else if (other.data != null) return false
        if (dataSize != other.dataSize) return false

        return true
    }

    override fun hashCode(): Int {
        var result = mediaType.hashCode()
        result = 31 * result + filePath.hashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + (data?.contentHashCode() ?: 0)
        result = 31 * result + dataSize
        return result
    }
}
