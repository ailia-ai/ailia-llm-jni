package axip.ailia_llm

import java.io.Closeable

/**
 * Kotlin binding for ailia LLM library.
 * Provides Large Language Model inference capabilities.
 *
 * Calls JNI directly from Kotlin without going through Java wrappers.
 */
class AiliaLLM : Closeable {
    // Native pointer to AILIALLM instance
    private var nativeHandle: Long = 0

    // State tracking to prevent invalid API calls
    private var modelLoaded = false
    private var promptSet = false
    private var multimodalProjectorLoaded = false

    init {
        val handleArray = LongArray(1)
        val status = ailiaLLMCreate(handleArray)
        if (status != AILIA_LLM_STATUS_SUCCESS) {
            throw RuntimeException("Failed to create AiliaLLM instance. Status: $status")
        }
        nativeHandle = handleArray[0]
    }

    /**
     * Opens a model file.
     *
     * @param path The path to the GGUF model file
     * @param nCtx The context length (0 for model default)
     * @throws RuntimeException if the operation fails
     */
    fun openModelFile(path: String, nCtx: Int) {
        val status = ailiaLLMOpenModelFileA(nativeHandle, path, nCtx)
        if (status != AILIA_LLM_STATUS_SUCCESS) {
            throw RuntimeException("Failed to open model file: $path. Status: $status")
        }
        modelLoaded = true
        promptSet = false // Reset prompt state when loading a new model
    }

    /**
     * Gets the context size of the model.
     *
     * @return The context size
     * @throws RuntimeException if the operation fails or model not loaded
     */
    fun getContextSize(): Int {
        checkModelLoaded()
        val size = IntArray(1)
        val status = ailiaLLMGetContextSize(nativeHandle, size)
        if (status != AILIA_LLM_STATUS_SUCCESS) {
            throw RuntimeException("Failed to get context size. Status: $status")
        }
        return size[0]
    }

    /**
     * Sets sampling parameters.
     *
     * @param topK The top-k sampling parameter (default: 40)
     * @param topP The top-p sampling parameter (default: 0.9)
     * @param temp The temperature parameter (default: 0.4)
     * @param seed The random seed (default: 1234)
     * @throws RuntimeException if the operation fails or model not loaded
     */
    fun setSamplingParams(topK: Int, topP: Float, temp: Float, seed: Int) {
        checkModelLoaded()
        val status = ailiaLLMSetSamplingParams(nativeHandle, topK, topP, temp, seed)
        if (status != AILIA_LLM_STATUS_SUCCESS) {
            throw RuntimeException("Failed to set sampling params. Status: $status")
        }
    }

    /**
     * Enable or disable thinking (reasoning output).
     * Controls whether thinking models (e.g. Gemma4) output their reasoning process.
     * Must be called before setPrompt.
     *
     * @param enable true to enable thinking, false to disable (default: disabled)
     * @throws RuntimeException if the operation fails or model not loaded
     */
    fun setThinking(enable: Boolean) {
        val status = ailiaLLMSetThinking(nativeHandle, if (enable) 1 else 0)
        if (status != AILIA_LLM_STATUS_SUCCESS) {
            throw RuntimeException("Failed to set thinking. Status: $status")
        }
    }

    /**
     * Sets the prompt for generation.
     *
     * @param messages Array of chat messages
     * @throws RuntimeException if the operation fails or model not loaded
     */
    fun setPrompt(messages: Array<AiliaLLMChatMessage>) {
        val multimodalMessages = messages.map { msg ->
            AiliaLLMMultimodalChatMessage(msg.role, msg.content, null)
        }.toTypedArray()
        setPromptInternal(multimodalMessages)
    }

    /**
     * Sets the prompt for generation with multimodal support.
     * Automatically detects whether to use text-only or multimodal processing
     * based on the presence of media data.
     *
     * @param messages Array of multimodal chat messages
     * @throws RuntimeException if the operation fails or model not loaded
     */
    fun setPrompt(messages: Array<AiliaLLMMultimodalChatMessage>) {
        setPromptInternal(messages)
    }

    /**
     * Internal implementation for setting prompts.
     * Automatically routes to text-only or multimodal processing based on media data presence.
     */
    private fun setPromptInternal(messages: Array<AiliaLLMMultimodalChatMessage>) {
        checkModelLoaded()
        val hasMediaData = messages.any { it.mediaData?.isNotEmpty() == true }

        if (hasMediaData) {
            checkMultimodalProjectorLoaded()
            val status = ailiaLLMSetMultimodalPrompt(nativeHandle, messages, messages.size)
            if (status != AILIA_LLM_STATUS_SUCCESS) {
                throw RuntimeException("Failed to set multimodal prompt. Status: $status")
            }
        } else {
            val textMessages = messages.map { AiliaLLMChatMessage(it.role, it.content) }.toTypedArray()
            val status = ailiaLLMSetPrompt(nativeHandle, textMessages, textMessages.size)
            if (status != AILIA_LLM_STATUS_SUCCESS) {
                throw RuntimeException("Failed to set prompt. Status: $status")
            }
        }
        promptSet = true
    }

    /**
     * Sets the tool (function) definitions for tool use (function calling).
     * The tools are rendered into the prompt through the chat template on the next
     * setPrompt call, the output is constrained to the tool call syntax, and the
     * raw output can be converted into tool calls with parseResponse().
     *
     * While tools are set, the messages passed to setPrompt are interpreted as follows:
     * - role "assistant": the raw model output (concatenation of getDeltaText()) as is. It is parsed
     *   internally; a RuntimeException is thrown if it does not match the tool call syntax (PARSE_ERROR).
     * - role "tool": the tool result as content (text or a JSON string), matched to the tool calls of
     *   the preceding assistant message by order
     * A "tool" message while no tools are set throws a RuntimeException (INVALID_STATE).
     *
     * Available for models whose chat template supports tool calling (e.g. Gemma 4).
     *
     * @param toolsJson OpenAI-compatible JSON array of tool definitions, e.g.
     *   [{"type":"function","function":{"name":"get_weather","description":"...","parameters":{...}}}]
     *   Pass null or an empty string to clear the tools.
     * @throws RuntimeException if the JSON is invalid or the operation fails
     */
    fun setTools(toolsJson: String?) {
        val status = ailiaLLMSetTools(nativeHandle, toolsJson)
        if (status != AILIA_LLM_STATUS_SUCCESS) {
            throw RuntimeException("Failed to set tools. Status: $status")
        }
    }

    /**
     * Parses the raw model output into an OpenAI-compatible assistant message JSON:
     * {"role":"assistant","content":"...","reasoning_content":"...","tool_calls":[{"id":"call_0","type":"function","function":{"name":"...","arguments":"{...}"}}]}
     * reasoning_content is present only when the text contains thinking output, and tool_calls only when
     * it contains tool calls. arguments is a JSON string.
     * The parser is built by setPrompt(): calling this before setPrompt(), or after setTools() / setThinking()
     * without a new setPrompt(), fails with INVALID_STATE. A text that does not match the tool call syntax
     * (e.g. an unfinished output) fails with PARSE_ERROR.
     *
     * @param text The raw model output, i.e. the concatenation of getDeltaText() until the generation is complete
     * @return The assistant message JSON
     * @throws RuntimeException if the operation fails or model not loaded
     */
    fun parseResponse(text: String): String {
        checkModelLoaded()
        val size = IntArray(1)
        var status = ailiaLLMParseResponseSize(nativeHandle, text, size)
        if (status != AILIA_LLM_STATUS_SUCCESS) {
            throw RuntimeException("Failed to get parsed response size. Status: $status")
        }

        val buffer = ByteArray(size[0])
        status = ailiaLLMParseResponse(nativeHandle, text, buffer, size[0])
        if (status != AILIA_LLM_STATUS_SUCCESS) {
            throw RuntimeException("Failed to parse response. Status: $status")
        }

        // Convert byte array to string (UTF-8), excluding null terminator
        return String(buffer, 0, size[0] - 1, Charsets.UTF_8)
    }

    /**
     * Generates one token.
     *
     * @return true if generation is done, false otherwise
     * @throws RuntimeException if the operation fails or prompt not set
     */
    fun generate(): Boolean {
        checkPromptSet()
        val done = IntArray(1)
        val status = ailiaLLMGenerate(nativeHandle, done)
        if (status != AILIA_LLM_STATUS_SUCCESS) {
            throw RuntimeException("Failed to generate. Status: $status")
        }
        return done[0] != 0
    }

    /**
     * Gets the generated text (delta).
     *
     * @return The generated text
     * @throws RuntimeException if the operation fails
     */
    fun getDeltaText(): String {
        val size = IntArray(1)
        var status = ailiaLLMGetDeltaTextSize(nativeHandle, size)
        if (status != AILIA_LLM_STATUS_SUCCESS) {
            throw RuntimeException("Failed to get delta text size. Status: $status")
        }

        val buffer = ByteArray(size[0])
        status = ailiaLLMGetDeltaText(nativeHandle, buffer, size[0])
        if (status != AILIA_LLM_STATUS_SUCCESS) {
            throw RuntimeException("Failed to get delta text. Status: $status")
        }

        // Convert byte array to string (UTF-8), excluding null terminator
        return String(buffer, 0, size[0] - 1, Charsets.UTF_8)
    }

    /**
     * Gets the token count for a text.
     *
     * @param text The text to tokenize
     * @return The number of tokens
     * @throws RuntimeException if the operation fails or model not loaded
     */
    fun getTokenCount(text: String): Int {
        checkModelLoaded()
        val count = IntArray(1)
        val status = ailiaLLMGetTokenCount(nativeHandle, count, text)
        if (status != AILIA_LLM_STATUS_SUCCESS) {
            throw RuntimeException("Failed to get token count. Status: $status")
        }
        return count[0]
    }

    /**
     * Gets the prompt token count.
     *
     * @return The number of prompt tokens
     * @throws RuntimeException if the operation fails or prompt not set
     */
    fun getPromptTokenCount(): Int {
        checkPromptSet()
        val count = IntArray(1)
        val status = ailiaLLMGetPromptTokenCount(nativeHandle, count)
        if (status != AILIA_LLM_STATUS_SUCCESS) {
            throw RuntimeException("Failed to get prompt token count. Status: $status")
        }
        return count[0]
    }

    /**
     * Opens a multimodal projector file for vision/audio support.
     * Must be called after openModelFile() to enable multimodal capabilities.
     *
     * @param path The path to the MMPROJ file (GGUF format)
     * @throws RuntimeException if the operation fails or model not loaded
     */
    fun openMultimodalProjectorFile(path: String) {
        checkModelLoaded()
        val status = ailiaLLMOpenMultimodalProjectorFileA(nativeHandle, path)
        if (status != AILIA_LLM_STATUS_SUCCESS) {
            throw RuntimeException("Failed to open multimodal projector file: $path. Status: $status")
        }
        multimodalProjectorLoaded = true
    }

    /**
     * Gets the multimodal capabilities of the loaded model.
     *
     * @return AiliaLLMMultimodalCapabilities object with vision and audio support flags
     * @throws RuntimeException if the operation fails or projector not loaded
     */
    fun getMultimodalCapabilities(): AiliaLLMMultimodalCapabilities {
        checkMultimodalProjectorLoaded()
        val visionSupport = IntArray(1)
        val audioSupport = IntArray(1)
        val status = ailiaLLMGetMultimodalCapabilities(nativeHandle, visionSupport, audioSupport)
        if (status != AILIA_LLM_STATUS_SUCCESS) {
            throw RuntimeException("Failed to get multimodal capabilities. Status: $status")
        }
        return AiliaLLMMultimodalCapabilities(
            visionSupport = visionSupport[0] != 0,
            audioSupport = audioSupport[0] != 0
        )
    }

    /**
     * Destroys the AiliaLLM instance and releases resources.
     */
    fun destroy() {
        if (nativeHandle != 0L) {
            ailiaLLMDestroy(nativeHandle)
            nativeHandle = 0
            modelLoaded = false
            promptSet = false
            multimodalProjectorLoaded = false
        }
    }

    /**
     * Implements Closeable for use with use { } blocks.
     */
    override fun close() {
        destroy()
    }

    /**
     * Ensures the native resources are released when the object is garbage collected.
     */
    @Suppress("deprecation")
    protected fun finalize() {
        destroy()
    }

    private fun checkModelLoaded() {
        if (!modelLoaded) {
            throw RuntimeException("Model not loaded. Call openModelFile() first.")
        }
    }

    private fun checkPromptSet() {
        if (!promptSet) {
            throw RuntimeException("Prompt not set. Call setPrompt() first.")
        }
    }

    private fun checkMultimodalProjectorLoaded() {
        if (!multimodalProjectorLoaded) {
            throw RuntimeException("Multimodal projector not loaded. Call openMultimodalProjectorFile() first.")
        }
    }

    companion object {
        // Status codes
        const val AILIA_LLM_STATUS_SUCCESS = 0
        const val AILIA_LLM_STATUS_INVALID_ARGUMENT = -1
        const val AILIA_LLM_STATUS_ERROR_FILE_API = -2
        const val AILIA_LLM_STATUS_INVALID_VERSION = -3
        const val AILIA_LLM_STATUS_BROKEN = -4
        const val AILIA_LLM_STATUS_MEMORY_INSUFFICIENT = -5
        const val AILIA_LLM_STATUS_THREAD_ERROR = -6
        const val AILIA_LLM_STATUS_INVALID_STATE = -7
        const val AILIA_LLM_STATUS_CONTEXT_FULL = -8
        const val AILIA_LLM_STATUS_ERROR_BUFFER_API = -9
        const val AILIA_LLM_STATUS_PARSE_ERROR = -10
        const val AILIA_LLM_STATUS_UNIMPLEMENTED = -15
        const val AILIA_LLM_STATUS_OTHER_ERROR = -128

        init {
            try {
                // JNI bindings are integrated into the main ailia_llm library
                System.loadLibrary("ailia_llm")

                // Test that JNI is working
                val result = testJNI()
                System.err.println("JNI test result: $result")
            } catch (e: UnsatisfiedLinkError) {
                System.err.println("Failed to load ailia_llm library: ${e.message}")
                throw e
            }
        }

        /**
         * Gets the number of available backends (CPU, GPU).
         *
         * @return The number of backends
         * @throws RuntimeException if the operation fails
         */
        @JvmStatic
        fun getBackendCount(): Int {
            val count = IntArray(1)
            val status = ailiaLLMGetBackendCount(count)
            if (status != AILIA_LLM_STATUS_SUCCESS) {
                throw RuntimeException("Failed to get backend count. Status: $status")
            }
            return count[0]
        }

        /**
         * Gets the name of a backend by index.
         *
         * @param index The backend index
         * @return The backend name
         * @throws RuntimeException if the operation fails
         */
        @JvmStatic
        fun getBackendName(index: Int): String {
            val name = arrayOfNulls<String>(1)
            val status = ailiaLLMGetBackendName(name, index)
            if (status != AILIA_LLM_STATUS_SUCCESS) {
                throw RuntimeException("Failed to get backend name. Status: $status")
            }
            return name[0] ?: throw RuntimeException("Backend name is null")
        }

        // Native methods - called directly from Kotlin
        @JvmStatic
        private external fun testJNI(): Int

        @JvmStatic
        private external fun ailiaLLMGetBackendCount(count: IntArray): Int

        @JvmStatic
        private external fun ailiaLLMGetBackendName(name: Array<String?>, index: Int): Int

        @JvmStatic
        private external fun ailiaLLMCreate(handle: LongArray): Int
    }

    // Instance native methods
    private external fun ailiaLLMOpenModelFileA(handle: Long, path: String, nCtx: Int): Int
    private external fun ailiaLLMGetContextSize(handle: Long, size: IntArray): Int
    private external fun ailiaLLMSetSamplingParams(handle: Long, topK: Int, topP: Float, temp: Float, seed: Int): Int
    private external fun ailiaLLMSetThinking(handle: Long, enable: Int): Int
    private external fun ailiaLLMSetPrompt(handle: Long, messages: Array<AiliaLLMChatMessage>, messageCount: Int): Int
    private external fun ailiaLLMSetTools(handle: Long, toolsJson: String?): Int
    private external fun ailiaLLMParseResponseSize(handle: Long, text: String, size: IntArray): Int
    private external fun ailiaLLMParseResponse(handle: Long, text: String, buffer: ByteArray, bufSize: Int): Int
    private external fun ailiaLLMGenerate(handle: Long, done: IntArray): Int
    private external fun ailiaLLMGetDeltaTextSize(handle: Long, size: IntArray): Int
    private external fun ailiaLLMGetDeltaText(handle: Long, buffer: ByteArray, bufSize: Int): Int
    private external fun ailiaLLMGetTokenCount(handle: Long, count: IntArray, text: String): Int
    private external fun ailiaLLMGetPromptTokenCount(handle: Long, count: IntArray): Int
    private external fun ailiaLLMDestroy(handle: Long)
    private external fun ailiaLLMOpenMultimodalProjectorFileA(handle: Long, path: String): Int
    private external fun ailiaLLMGetMultimodalCapabilities(handle: Long, visionSupport: IntArray, audioSupport: IntArray): Int
    private external fun ailiaLLMSetMultimodalPrompt(handle: Long, messages: Array<AiliaLLMMultimodalChatMessage>, messageCount: Int): Int
}
