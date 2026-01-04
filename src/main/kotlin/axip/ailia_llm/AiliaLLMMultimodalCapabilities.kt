package axip.ailia_llm

/**
 * Represents the multimodal capabilities of a loaded LLM model.
 *
 * @property visionSupport Whether image/vision processing is supported
 * @property audioSupport Whether audio processing is supported
 */
data class AiliaLLMMultimodalCapabilities(
    @JvmField val visionSupport: Boolean,
    @JvmField val audioSupport: Boolean
) {
    override fun toString(): String =
        "AiliaLLMMultimodalCapabilities[vision=$visionSupport, audio=$audioSupport]"
}
