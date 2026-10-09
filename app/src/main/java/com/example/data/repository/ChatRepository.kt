package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.ChatConversation
import com.example.data.model.ChatMessage
import com.example.data.model.MessageRole
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class ChatRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("gemini_chat_storage", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CONVERSATIONS = "conversations_list"
        private const val KEY_CUSTOM_API_KEY = "custom_gemini_api_key"
        private const val KEY_CURRENT_MODEL = "selected_gemini_model"
        private const val KEY_DARK_THEME = "is_dark_theme"
    }

    init {
        // Seed default initial conversations if empty
        if (getConversations().isEmpty()) {
            seedDefaultConversations()
        }
    }

    fun getCustomApiKey(): String {
        return prefs.getString(KEY_CUSTOM_API_KEY, "") ?: ""
    }

    fun setCustomApiKey(key: String) {
        prefs.edit().putString(KEY_CUSTOM_API_KEY, key.trim()).apply()
    }

    fun getSelectedModelId(): String {
        return prefs.getString(KEY_CURRENT_MODEL, "gemini-2.5-flash") ?: "gemini-2.5-flash"
    }

    fun setSelectedModelId(modelId: String) {
        prefs.edit().putString(KEY_CURRENT_MODEL, modelId).apply()
    }

    fun isDarkTheme(): Boolean {
        return prefs.getBoolean(KEY_DARK_THEME, true)
    }

    fun setDarkTheme(isDark: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_THEME, isDark).apply()
    }

    fun getConversations(): List<ChatConversation> {
        val jsonStr = prefs.getString(KEY_CONVERSATIONS, null) ?: return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<ChatConversation>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ChatConversation(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        preview = obj.optString("preview", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                        isPinned = obj.optBoolean("isPinned", false),
                        modelId = obj.optString("modelId", "gemini-2.5-flash")
                    )
                )
            }
            list.sortedByDescending { it.updatedAt }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveConversations(conversations: List<ChatConversation>) {
        val array = JSONArray()
        for (conv in conversations) {
            val obj = JSONObject().apply {
                put("id", conv.id)
                put("title", conv.title)
                put("preview", conv.preview)
                put("createdAt", conv.createdAt)
                put("updatedAt", conv.updatedAt)
                put("isPinned", conv.isPinned)
                put("modelId", conv.modelId)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_CONVERSATIONS, array.toString()).apply()
    }

    fun createConversation(title: String = "New conversation", modelId: String = "gemini-2.5-flash"): ChatConversation {
        val newConv = ChatConversation(
            id = UUID.randomUUID().toString(),
            title = title,
            preview = "",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            modelId = modelId
        )
        val current = getConversations().toMutableList()
        current.add(0, newConv)
        saveConversations(current)
        return newConv
    }

    fun deleteConversation(id: String) {
        val current = getConversations().filterNot { it.id == id }
        saveConversations(current)
        prefs.edit().remove("messages_$id").apply()
    }

    fun togglePinConversation(id: String) {
        val current = getConversations().map {
            if (it.id == id) it.copy(isPinned = !it.isPinned) else it
        }
        saveConversations(current)
    }

    fun getMessages(conversationId: String): List<ChatMessage> {
        val jsonStr = prefs.getString("messages_$conversationId", null) ?: return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<ChatMessage>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val roleStr = obj.getString("role")
                val role = if (roleStr == "USER") MessageRole.USER else MessageRole.GEMINI
                val likedObj = if (obj.has("liked")) obj.optBoolean("liked") else null
                list.add(
                    ChatMessage(
                        id = obj.getString("id"),
                        role = role,
                        text = obj.getString("text"),
                        imageBase64 = if (obj.has("imageBase64")) obj.optString("imageBase64") else null,
                        imageUri = if (obj.has("imageUri")) obj.optString("imageUri") else null,
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        liked = likedObj
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveMessages(conversationId: String, messages: List<ChatMessage>) {
        val array = JSONArray()
        for (msg in messages) {
            val obj = JSONObject().apply {
                put("id", msg.id)
                put("role", msg.role.name)
                put("text", msg.text)
                if (msg.imageBase64 != null) put("imageBase64", msg.imageBase64)
                if (msg.imageUri != null) put("imageUri", msg.imageUri)
                put("timestamp", msg.timestamp)
                if (msg.liked != null) put("liked", msg.liked)
            }
            array.put(obj)
        }
        prefs.edit().putString("messages_$conversationId", array.toString()).apply()

        // Update conversation preview and updatedAt
        val lastMsg = messages.lastOrNull()
        if (lastMsg != null) {
            val convs = getConversations().map {
                if (it.id == conversationId) {
                    val preview = lastMsg.text.take(60)
                    val title = if (it.title == "New conversation" && messages.isNotEmpty()) {
                        val firstUserMsg = messages.firstOrNull { m -> m.role == MessageRole.USER }?.text
                        firstUserMsg?.take(32) ?: it.title
                    } else {
                        it.title
                    }
                    it.copy(preview = preview, title = title, updatedAt = System.currentTimeMillis())
                } else {
                    it
                }
            }
            saveConversations(convs)
        }
    }

    fun clearAllConversations() {
        val convs = getConversations()
        val editor = prefs.edit()
        for (c in convs) {
            editor.remove("messages_${c.id}")
        }
        editor.remove(KEY_CONVERSATIONS)
        editor.apply()
    }

    private fun seedDefaultConversations() {
        val conv1Id = UUID.randomUUID().toString()
        val conv1 = ChatConversation(
            id = conv1Id,
            title = "Welcome to Gemini",
            preview = "Hi! I am Gemini, your AI assistant built by Google...",
            createdAt = System.currentTimeMillis() - 86400000,
            updatedAt = System.currentTimeMillis() - 86400000,
            isPinned = true,
            modelId = "gemini-2.5-flash"
        )

        val messages1 = listOf(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                role = MessageRole.GEMINI,
                text = "✨ **Welcome to Google Gemini!**\n\nI can help you collaborate on ideas, write code, plan trips, analyze images, and explore new concepts. Tap the **Gemini Live** button at the bottom anytime to have a real-time conversation!",
                timestamp = System.currentTimeMillis() - 86400000
            )
        )

        saveConversations(listOf(conv1))
        saveMessages(conv1Id, messages1)
    }
}
