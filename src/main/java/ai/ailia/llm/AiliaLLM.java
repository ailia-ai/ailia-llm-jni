package ai.ailia.llm;

/**
 * Java binding for ailia LLM library.
 * Provides Large Language Model inference capabilities.
 */
public class AiliaLLM {
    // Status codes
    public static final int AILIA_LLM_STATUS_SUCCESS = 0;
    public static final int AILIA_LLM_STATUS_INVALID_ARGUMENT = -1;
    public static final int AILIA_LLM_STATUS_ERROR_FILE_API = -2;
    public static final int AILIA_LLM_STATUS_INVALID_VERSION = -3;
    public static final int AILIA_LLM_STATUS_BROKEN = -4;
    public static final int AILIA_LLM_STATUS_MEMORY_INSUFFICIENT = -5;
    public static final int AILIA_LLM_STATUS_THREAD_ERROR = -6;
    public static final int AILIA_LLM_STATUS_INVALID_STATE = -7;
    public static final int AILIA_LLM_STATUS_CONTEXT_FULL = -8;
    public static final int AILIA_LLM_STATUS_UNIMPLEMENTED = -15;
    public static final int AILIA_LLM_STATUS_OTHER_ERROR = -128;

    // Native library loading
    static {
        try {
            // JNI bindings are integrated into the main ailia_llm library
            System.loadLibrary("ailia_llm");

            // Test that JNI is working
            int result = testJNI();
            System.err.println("JNI test result: " + result);
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Failed to load ailia_llm library: " + e.getMessage());
            throw e;
        }
    }

    // Test function
    private static native int testJNI();

    // Native pointer to AILIALLM instance
    private long nativeHandle;

    // State tracking to prevent invalid API calls
    private boolean modelLoaded = false;
    private boolean promptSet = false;
    private boolean multimodalProjectorLoaded = false;

    /**
     * Creates a new AiliaLLM instance.
     *
     * @throws RuntimeException if creation fails
     */
    public AiliaLLM() {
        long[] handleArray = new long[1];
        int status = ailiaLLMCreate(handleArray);
        if (status != AILIA_LLM_STATUS_SUCCESS) {
            throw new RuntimeException("Failed to create AiliaLLM instance. Status: " + status);
        }
        this.nativeHandle = handleArray[0];
    }

    /**
     * Gets the number of available backends (CPU, GPU).
     *
     * @return The number of backends
     * @throws RuntimeException if the operation fails
     */
    public static int getBackendCount() {
        int[] count = new int[1];
        int status = ailiaLLMGetBackendCount(count);
        if (status != AILIA_LLM_STATUS_SUCCESS) {
            throw new RuntimeException("Failed to get backend count. Status: " + status);
        }
        return count[0];
    }

    /**
     * Gets the name of a backend by index.
     *
     * @param index The backend index
     * @return The backend name
     * @throws RuntimeException if the operation fails
     */
    public static String getBackendName(int index) {
        String[] name = new String[1];
        int status = ailiaLLMGetBackendName(name, index);
        if (status != AILIA_LLM_STATUS_SUCCESS) {
            throw new RuntimeException("Failed to get backend name. Status: " + status);
        }
        return name[0];
    }

    /**
     * Opens a model file.
     *
     * @param path The path to the GGUF model file
     * @param nCtx The context length (0 for model default)
     * @throws RuntimeException if the operation fails
     */
    public void openModelFile(String path, int nCtx) {
        int status = ailiaLLMOpenModelFileA(nativeHandle, path, nCtx);
        if (status != AILIA_LLM_STATUS_SUCCESS) {
            throw new RuntimeException("Failed to open model file: " + path + ". Status: " + status);
        }
        modelLoaded = true;
        promptSet = false; // Reset prompt state when loading a new model
    }

    /**
     * Gets the context size of the model.
     *
     * @return The context size
     * @throws RuntimeException if the operation fails or model not loaded
     */
    public int getContextSize() {
        if (!modelLoaded) {
            throw new RuntimeException("Model not loaded. Call openModelFile() first.");
        }
        int[] size = new int[1];
        int status = ailiaLLMGetContextSize(nativeHandle, size);
        if (status != AILIA_LLM_STATUS_SUCCESS) {
            throw new RuntimeException("Failed to get context size. Status: " + status);
        }
        return size[0];
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
    public void setSamplingParams(int topK, float topP, float temp, int seed) {
        if (!modelLoaded) {
            throw new RuntimeException("Model not loaded. Call openModelFile() first.");
        }
        int status = ailiaLLMSetSamplingParams(nativeHandle, topK, topP, temp, seed);
        if (status != AILIA_LLM_STATUS_SUCCESS) {
            throw new RuntimeException("Failed to set sampling params. Status: " + status);
        }
    }

    /**
     * Sets the prompt for generation.
     *
     * @param messages Array of chat messages
     * @throws RuntimeException if the operation fails or model not loaded
     */
    public void setPrompt(AiliaLLMChatMessage[] messages) {
        if (!modelLoaded) {
            throw new RuntimeException("Model not loaded. Call openModelFile() first.");
        }
        int status = ailiaLLMSetPrompt(nativeHandle, messages, messages.length);
        if (status != AILIA_LLM_STATUS_SUCCESS) {
            throw new RuntimeException("Failed to set prompt. Status: " + status);
        }
        promptSet = true;
    }

    /**
     * Generates one token.
     *
     * @return true if generation is done, false otherwise
     * @throws RuntimeException if the operation fails or prompt not set
     */
    public boolean generate() {
        if (!promptSet) {
            throw new RuntimeException("Prompt not set. Call setPrompt() first.");
        }
        int[] done = new int[1];
        int status = ailiaLLMGenerate(nativeHandle, done);
        if (status != AILIA_LLM_STATUS_SUCCESS) {
            throw new RuntimeException("Failed to generate. Status: " + status);
        }
        return done[0] != 0;
    }

    /**
     * Gets the generated text (delta).
     *
     * @return The generated text
     * @throws RuntimeException if the operation fails
     */
    public String getDeltaText() {
        int[] size = new int[1];
        int status = ailiaLLMGetDeltaTextSize(nativeHandle, size);
        if (status != AILIA_LLM_STATUS_SUCCESS) {
            throw new RuntimeException("Failed to get delta text size. Status: " + status);
        }

        byte[] buffer = new byte[size[0]];
        status = ailiaLLMGetDeltaText(nativeHandle, buffer, size[0]);
        if (status != AILIA_LLM_STATUS_SUCCESS) {
            throw new RuntimeException("Failed to get delta text. Status: " + status);
        }

        try {
            // Convert byte array to string (UTF-8)
            return new String(buffer, 0, size[0] - 1, "UTF-8"); // -1 to exclude null terminator
        } catch (java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 encoding not supported", e);
        }
    }

    /**
     * Gets the token count for a text.
     *
     * @param text The text to tokenize
     * @return The number of tokens
     * @throws RuntimeException if the operation fails or model not loaded
     */
    public int getTokenCount(String text) {
        if (!modelLoaded) {
            throw new RuntimeException("Model not loaded. Call openModelFile() first.");
        }
        int[] count = new int[1];
        int status = ailiaLLMGetTokenCount(nativeHandle, count, text);
        if (status != AILIA_LLM_STATUS_SUCCESS) {
            throw new RuntimeException("Failed to get token count. Status: " + status);
        }
        return count[0];
    }

    /**
     * Gets the prompt token count.
     *
     * @return The number of prompt tokens
     * @throws RuntimeException if the operation fails or prompt not set
     */
    public int getPromptTokenCount() {
        if (!promptSet) {
            throw new RuntimeException("Prompt not set. Call setPrompt() first.");
        }
        int[] count = new int[1];
        int status = ailiaLLMGetPromptTokenCount(nativeHandle, count);
        if (status != AILIA_LLM_STATUS_SUCCESS) {
            throw new RuntimeException("Failed to get prompt token count. Status: " + status);
        }
        return count[0];
    }

    /**
     * Opens a multimodal projector file for vision/audio support.
     * Must be called after openModelFile() to enable multimodal capabilities.
     *
     * @param path The path to the MMPROJ file (GGUF format)
     * @throws RuntimeException if the operation fails or model not loaded
     */
    public void openMultimodalProjectorFile(String path) {
        if (!modelLoaded) {
            throw new RuntimeException("Model not loaded. Call openModelFile() first.");
        }
        int status = ailiaLLMOpenMultimodalProjectorFileA(nativeHandle, path);
        if (status != AILIA_LLM_STATUS_SUCCESS) {
            throw new RuntimeException("Failed to open multimodal projector file: " + path + ". Status: " + status);
        }
        multimodalProjectorLoaded = true;
    }

    /**
     * Gets the multimodal capabilities of the loaded model.
     *
     * @return AiliaLLMMultimodalCapabilities object with vision and audio support flags
     * @throws RuntimeException if the operation fails or projector not loaded
     */
    public AiliaLLMMultimodalCapabilities getMultimodalCapabilities() {
        if (!multimodalProjectorLoaded) {
            throw new RuntimeException("Multimodal projector not loaded. Call openMultimodalProjectorFile() first.");
        }
        int[] visionSupport = new int[1];
        int[] audioSupport = new int[1];
        int status = ailiaLLMGetMultimodalCapabilities(nativeHandle, visionSupport, audioSupport);
        if (status != AILIA_LLM_STATUS_SUCCESS) {
            throw new RuntimeException("Failed to get multimodal capabilities. Status: " + status);
        }
        return new AiliaLLMMultimodalCapabilities(
            visionSupport[0] != 0,
            audioSupport[0] != 0
        );
    }

    /**
     * Sets the multimodal prompt for generation with media attachments.
     * Use <__media__> placeholders in message content to reference media files.
     *
     * @param messages Array of multimodal chat messages
     * @throws RuntimeException if the operation fails or projector not loaded
     */
    public void setMultimodalPrompt(AiliaLLMMultimodalChatMessage[] messages) {
        if (!multimodalProjectorLoaded) {
            throw new RuntimeException("Multimodal projector not loaded. Call openMultimodalProjectorFile() first.");
        }
        int status = ailiaLLMSetMultimodalPrompt(nativeHandle, messages, messages.length);
        if (status != AILIA_LLM_STATUS_SUCCESS) {
            throw new RuntimeException("Failed to set multimodal prompt. Status: " + status);
        }
        promptSet = true;
    }

    /**
     * Destroys the AiliaLLM instance and releases resources.
     */
    public void destroy() {
        if (nativeHandle != 0) {
            ailiaLLMDestroy(nativeHandle);
            nativeHandle = 0;
            modelLoaded = false;
            promptSet = false;
            multimodalProjectorLoaded = false;
        }
    }

    /**
     * Ensures the native resources are released when the object is garbage collected.
     */
    @Override
    protected void finalize() throws Throwable {
        try {
            destroy();
        } finally {
            super.finalize();
        }
    }

    // Native methods
    private static native int ailiaLLMGetBackendCount(int[] count);
    private static native int ailiaLLMGetBackendName(String[] name, int index);
    private static native int ailiaLLMCreate(long[] handle);
    private native int ailiaLLMOpenModelFileA(long handle, String path, int nCtx);
    private native int ailiaLLMGetContextSize(long handle, int[] size);
    private native int ailiaLLMSetSamplingParams(long handle, int topK, float topP, float temp, int seed);
    private native int ailiaLLMSetPrompt(long handle, AiliaLLMChatMessage[] messages, int messageCount);
    private native int ailiaLLMGenerate(long handle, int[] done);
    private native int ailiaLLMGetDeltaTextSize(long handle, int[] size);
    private native int ailiaLLMGetDeltaText(long handle, byte[] buffer, int bufSize);
    private native int ailiaLLMGetTokenCount(long handle, int[] count, String text);
    private native int ailiaLLMGetPromptTokenCount(long handle, int[] count);
    private native void ailiaLLMDestroy(long handle);
    private native int ailiaLLMOpenMultimodalProjectorFileA(long handle, String path);
    private native int ailiaLLMGetMultimodalCapabilities(long handle, int[] visionSupport, int[] audioSupport);
    private native int ailiaLLMSetMultimodalPrompt(long handle, AiliaLLMMultimodalChatMessage[] messages, int messageCount);
}
