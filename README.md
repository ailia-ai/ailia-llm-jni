# ailia LLM Kotlin/JNI Package

!! CAUTION !!
"ailia" IS NOT OPEN SOURCE SOFTWARE (OSS).
As long as user complies with the conditions stated in [License Document](https://ailia.ai/license/), user may use the Software for free of charge, but the Software is basically paid software.

## About ailia LLM

ailia LLM is a library to perform large language model inference using AI. It provides a C API for native applications, as well as bindings for various languages. Using ailia LLM, you can easily integrate AI powered text generation into your applications.

## Kotlin Bindings

This package provides Kotlin bindings for ailia LLM that call JNI directly from Kotlin without going through Java wrappers.

### Requirements

- Gradle 8.14
- Kotlin 1.8.22
- JDK 1.8

### Usage

```kotlin
import ai.ailia.llm.AiliaLLM
import ai.ailia.llm.AiliaLLMChatMessage

// Use Kotlin's use block for automatic cleanup
AiliaLLM().use { llm ->
    // Load model
    llm.openModelFile("path/to/model.gguf", 0)

    // Set sampling parameters
    llm.setSamplingParams(40, 0.9f, 0.4f, 1234)

    // Set prompt
    val messages = arrayOf(
        AiliaLLMChatMessage("system", "You are a helpful assistant."),
        AiliaLLMChatMessage("user", "Hello!")
    )
    llm.setPrompt(messages)

    // Generate response
    while (!llm.generate()) {
        print(llm.getDeltaText())
    }
    println()
}
```

## API specification

https://github.com/axinc-ai/ailia-sdk
