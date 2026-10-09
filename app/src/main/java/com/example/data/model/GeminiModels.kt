package com.example.data.model

import java.util.UUID

enum class MessageRole {
    USER,
    GEMINI
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val text: String,
    val imageBase64: String? = null,
    val imageUri: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false,
    val liked: Boolean? = null // true for up, false for down, null for none
)

data class ChatConversation(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New conversation",
    val preview: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val modelId: String = "gemini-2.5-flash"
)

data class GeminiModelOption(
    val id: String,
    val name: String,
    val subtitle: String,
    val description: String,
    val isDefault: Boolean = false,
    val isPro: Boolean = false
) {
    companion object {
        val ALL_MODELS = listOf(
            GeminiModelOption(
                id = "gemini-2.5-flash",
                name = "Gemini 2.5 Flash",
                subtitle = "Fast & Multimodal",
                description = "Our standard model for everyday questions, writing, and image analysis.",
                isDefault = true
            ),
            GeminiModelOption(
                id = "gemini-2.5-pro",
                name = "Gemini 2.5 Pro",
                subtitle = "Complex reasoning",
                description = "Advanced reasoning, complex coding, and deep multi-step explanations.",
                isPro = true
            ),
            GeminiModelOption(
                id = "gemini-3.1-pro-preview",
                name = "Gemini Advanced",
                subtitle = "Ultra-capable next-gen",
                description = "Deepest logic, massive context window, and cutting-edge intelligence.",
                isPro = true
            )
        )
    }
}

data class QuickPrompt(
    val category: String,
    val title: String,
    val prompt: String,
    val iconName: String
)

data class GeminiGem(
    val id: String,
    val title: String,
    val description: String,
    val systemInstruction: String,
    val iconEmoji: String
)
