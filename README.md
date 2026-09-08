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

### Tool Use (Function Calling)

With models whose chat template supports tool calling (e.g. Gemma 4), pass OpenAI-compatible tool definitions with `setTools` and convert the raw output into tool calls with `parseResponse`. Keep the raw output as the `assistant` content of the history and return tool results as the content of `tool` messages (matched to the tool calls by order).

```kotlin
llm.setTools("""[{"type":"function","function":{"name":"get_weather","description":"Get the current weather of a city.","parameters":{"type":"object","properties":{"city":{"type":"string"}},"required":["city"]}}}]""")

val question = "What is the weather in Tokyo?"
llm.setPrompt(arrayOf(AiliaLLMChatMessage("user", question)))
val raw = StringBuilder()
while (!llm.generate()) { raw.append(llm.getDeltaText()) }
val response = llm.parseResponse(raw.toString())
// {"role":"assistant","content":"","tool_calls":[{"id":"call_0","type":"function","function":{"name":"get_weather","arguments":"{\"city\":\"Tokyo\"}"}}]}

// execute the tool, then return the result
llm.setPrompt(arrayOf(
    AiliaLLMChatMessage("user", question),
    AiliaLLMChatMessage("assistant", raw.toString()), // raw output as is
    AiliaLLMChatMessage("tool", "Sunny, 25C") // result as is, matched to the tool calls by order
))
while (!llm.generate()) { print(llm.getDeltaText()) }
```

## API specification

https://github.com/ailia-ai/ailia-sdk
