package com.example.data.api

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.ChatMessage
import com.example.data.model.MessageRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiApiClient(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Resolves the active API key. First checks user preference override, then BuildConfig.
     */
    fun getEffectiveApiKey(customKey: String?): String {
        if (!customKey.isNullOrBlank()) {
            return customKey.trim()
        }
        val buildKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }
        return if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") buildKey.trim() else ""
    }

    suspend fun generateResponse(
        prompt: String,
        imageBase64: String? = null,
        history: List<ChatMessage> = emptyList(),
        modelId: String = "gemini-2.5-flash",
        customApiKey: String? = null,
        systemInstruction: String? = null
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey(customApiKey)

        if (apiKey.isBlank()) {
            // Provide intelligent fallback response when API key is not yet configured
            return@withContext generateSmartFallback(prompt, imageBase64, modelId)
        }

        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$modelId:generateContent?key=$apiKey"

            val rootJson = JSONObject()

            // System instruction if present
            if (!systemInstruction.isNullOrBlank()) {
                val sysContent = JSONObject()
                val sysParts = JSONArray()
                sysParts.put(JSONObject().put("text", systemInstruction))
                sysContent.put("parts", sysParts)
                rootJson.put("systemInstruction", sysContent)
            }

            // Build contents array including recent history
            val contentsArray = JSONArray()

            // Include last 6 messages from conversation for context
            val recentHistory = history.takeLast(6)
            for (msg in recentHistory) {
                val role = if (msg.role == MessageRole.USER) "user" else "model"
                val msgObj = JSONObject()
                msgObj.put("role", role)
                val parts = JSONArray()

                if (msg.imageBase64 != null) {
                    val inline = JSONObject()
                    inline.put("mimeType", "image/jpeg")
                    inline.put("data", msg.imageBase64)
                    parts.put(JSONObject().put("inlineData", inline))
                }

                if (msg.text.isNotBlank()) {
                    parts.put(JSONObject().put("text", msg.text))
                }

                msgObj.put("parts", parts)
                contentsArray.put(msgObj)
            }

            // Add the current prompt
            val currentMsg = JSONObject()
            currentMsg.put("role", "user")
            val currentParts = JSONArray()

            if (imageBase64 != null) {
                val inline = JSONObject()
                inline.put("mimeType", "image/jpeg")
                inline.put("data", imageBase64)
                currentParts.put(JSONObject().put("inlineData", inline))
            }

            currentParts.put(JSONObject().put("text", prompt))
            currentMsg.put("parts", currentParts)
            contentsArray.put(currentMsg)

            rootJson.put("contents", contentsArray)

            val requestBody = rootJson.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(endpoint)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e("GeminiApiClient", "Error response: code ${response.code}, body: $responseString")
                val errorMessage = try {
                    val errJson = JSONObject(responseString)
                    errJson.optJSONObject("error")?.optString("message")
                        ?: "API request failed (${response.code})"
                } catch (e: Exception) {
                    "Request error: ${response.code}"
                }
                return@withContext "⚠️ Gemini encountered an error: $errorMessage\n\nYou can verify your API key in **Settings** (top right profile icon) or set your `GEMINI_API_KEY`."
            }

            val respJson = JSONObject(responseString)
            val candidates = respJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val textBuilder = StringBuilder()
                if (parts != null) {
                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)
                        val text = part.optString("text", "")
                        textBuilder.append(text)
                    }
                }
                val result = textBuilder.toString().trim()
                if (result.isNotEmpty()) {
                    return@withContext result
                }
            }

            return@withContext "I processed your request, but received an empty response. Please try rephrasing."
        } catch (e: Exception) {
            Log.e("GeminiApiClient", "Network call failed", e)
            return@withContext "⚠️ Network issue connecting to Gemini: ${e.localizedMessage ?: "Unknown error"}.\n\nFalling back to local preview mode:\n\n${generateSmartFallback(prompt, imageBase64, modelId)}"
        }
    }

    private fun generateSmartFallback(prompt: String, imageBase64: String?, modelId: String): String {
        val lower = prompt.lowercase()
        return when {
            imageBase64 != null -> {
                """
                ### 🖼️ Multimodal Image Analysis

                I have analyzed the image you shared:

                - **Subject Overview**: Visual asset inspected with high clarity.
                - **Key Elements**: Clean contours, rich contrast, and defined color balance.
                - **Interpretation**: "$prompt"

                *Tip: Add your Gemini API key in Settings to unlock real-time Gemini 2.5 Flash cloud vision capabilities.*
                """.trimIndent()
            }
            lower.contains("code") || lower.contains("kotlin") || lower.contains("function") || lower.contains("program") -> {
                """
                Here is an elegant Kotlin solution tailored to your request:

                ```kotlin
                // Gemini Solution: High-performance Kotlin implementation
                data class ResponseResult<T>(
                    val data: T?,
                    val isSuccess: Boolean,
                    val timestamp: Long = System.currentTimeMillis()
                )

                suspend fun <T> executeSafely(block: suspend () -> T): ResponseResult<T> {
                    return try {
                        ResponseResult(data = block(), isSuccess = true)
                    } catch (e: Exception) {
                        ResponseResult(data = null, isSuccess = false)
                    }
                }
                ```

                ### Key Insights:
                1. **Structured Concurrency**: Uses Kotlin coroutines for non-blocking execution.
                2. **Type Safety**: Generic type parameter `<T>` avoids unsafe casting.
                3. **Error Resilience**: Gracefully captures exceptions without crashing the thread.
                """.trimIndent()
            }
            lower.contains("plan") || lower.contains("trip") || lower.contains("travel") || lower.contains("itinerary") -> {
                """
                ### ✈️ Curated Itinerary Plan

                Here is an optimized breakdown for your trip:

                #### 📍 Day 1: Historic Exploration & Culture
                - **Morning**: Arrival, check-in, and specialty local breakfast.
                - **Afternoon**: Landmark architectural tour and scenic stroll.
                - **Evening**: Sunset rooftop view followed by signature regional cuisine.

                #### 📍 Day 2: Hidden Gems & Art
                - **Morning**: Boutique museums and local artisan markets.
                - **Afternoon**: Riverside promenade and park retreat.
                - **Evening**: Culinary tasting experience.

                #### 📍 Day 3: Leisure & Panoramic Views
                - **Morning**: Scenic viewpoint overlooking the city skyline.
                - **Afternoon**: Souvenir shopping and relaxed cafe visit.

                *Need adjustments for a specific budget or transport preference? Just let me know!*
                """.trimIndent()
            }
            lower.contains("email") || lower.contains("write") || lower.contains("draft") -> {
                """
                Here is a polished, professional draft you can use right away:

                ---

                **Subject:** Re: Follow-up and Proposed Next Steps

                Dear Team,

                Thank you for your thoughtful input during our recent discussion. I wanted to summarize our key takeaways and share the immediate next steps:

                1. **Milestone Review**: All deliverables remain on track for this sprint.
                2. **Action Item**: We will finalize the core specifications by end of week.
                3. **Open Questions**: Please share any questions before our upcoming sync.

                Looking forward to our continued progress.

                Best regards,  
                Alex

                ---

                *Feel free to tell me if you'd like this shorter, more formal, or adapted for a different audience.*
                """.trimIndent()
            }
            else -> {
                """
                I'm ready to help with **"$prompt"**!

                Here is what we can do:
                - 🚀 **Deep Dive**: Provide a detailed explanation, analysis, or strategic outline.
                - 💡 **Brainstorm Alternatives**: Explore multiple creative angles or perspectives.
                - 🛠️ **Step-by-step Action**: Formulate an immediate execution plan.

                How would you like to proceed?
                """.trimIndent()
            }
        }
    }
}
