package ai.ailia.llm;

/**
 * Represents the multimodal capabilities of a loaded LLM model.
 */
public class AiliaLLMMultimodalCapabilities {
    /** Whether image/vision processing is supported */
    public boolean visionSupport;

    /** Whether audio processing is supported */
    public boolean audioSupport;

    /**
     * Creates a new capabilities object.
     *
     * @param visionSupport Whether vision is supported
     * @param audioSupport Whether audio is supported
     */
    public AiliaLLMMultimodalCapabilities(boolean visionSupport, boolean audioSupport) {
        this.visionSupport = visionSupport;
        this.audioSupport = audioSupport;
    }

    @Override
    public String toString() {
        return String.format("AiliaLLMMultimodalCapabilities[vision=%s, audio=%s]",
            visionSupport, audioSupport);
    }
}
