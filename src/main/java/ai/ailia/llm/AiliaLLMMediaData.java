package ai.ailia.llm;

/**
 * Represents media data (image or audio) for multimodal LLM processing.
 * Corresponds to the AILIALLMMediaData C structure.
 */
public class AiliaLLMMediaData {
    /** Media type: "image" or "audio" (audio is reserved for future use) */
    public String mediaType;

    /** Path to the media file (UTF-8) */
    public String filePath;

    /** Raw media data (currently unsupported, reserved for future use) */
    public byte[] data;

    /** Size of the raw data */
    public int dataSize;

    /** Width for images (pixels), sample count for audio */
    public int width;

    /** Height for images (pixels), unused for audio (set to 0) */
    public int height;

    /**
     * Creates a new media data object for file-based input.
     *
     * @param mediaType The media type ("image" or "audio")
     * @param filePath The path to the media file
     * @param width Width for images (pixels), sample count for audio
     * @param height Height for images (pixels), 0 for audio
     */
    public AiliaLLMMediaData(String mediaType, String filePath, int width, int height) {
        this.mediaType = mediaType;
        this.filePath = filePath;
        this.data = null;
        this.dataSize = 0;
        this.width = width;
        this.height = height;
    }

    /**
     * Creates a new media data object for file-based input with default dimensions.
     * The dimensions will be determined automatically from the file.
     *
     * @param mediaType The media type ("image" or "audio")
     * @param filePath The path to the media file
     */
    public AiliaLLMMediaData(String mediaType, String filePath) {
        this(mediaType, filePath, 0, 0);
    }

    @Override
    public String toString() {
        return String.format("AiliaLLMMediaData[type=%s, path=%s, size=%dx%d]",
            mediaType, filePath, width, height);
    }
}
