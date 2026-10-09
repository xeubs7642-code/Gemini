package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiApiClient
import com.example.data.model.ChatConversation
import com.example.data.model.ChatMessage
import com.example.data.model.GeminiGem
import com.example.data.model.GeminiModelOption
import com.example.data.model.MessageRole
import com.example.data.repository.ChatRepository
import com.example.util.TtsManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.UUID

enum class LiveState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}

data class GeminiUiState(
    val currentConversation: ChatConversation? = null,
    val conversations: List<ChatConversation> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val attachedImageUri: Uri? = null,
    val attachedImageBase64: String? = null,
    val selectedModel: GeminiModelOption = GeminiModelOption.ALL_MODELS[0],
    val isLoading: Boolean = false,
    val isStreaming: Boolean = false,
    val isGeminiLiveOpen: Boolean = false,
    val liveState: LiveState = LiveState.IDLE,
    val liveTranscript: String = "Tap or hold the mic to talk with Gemini",
    val liveAiResponse: String = "",
    val isDarkTheme: Boolean = true,
    val customApiKey: String = "",
    val snackbarMessage: String? = null,
    val showProfileDialog: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val showModelSheet: Boolean = false,
    val showGemsSheet: Boolean = false,
    val activeGem: GeminiGem? = null
)

class GeminiViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ChatRepository(application)
    private val apiClient = GeminiApiClient(application)
    val ttsManager = TtsManager(application)

    private val _uiState = MutableStateFlow(GeminiUiState())
    val uiState = _uiState.asStateFlow()

    private var streamingJob: Job? = null

    init {
        loadInitialState()
    }

    private fun loadInitialState() {
        val convs = repository.getConversations()
        val savedModelId = repository.getSelectedModelId()
        val model = GeminiModelOption.ALL_MODELS.firstOrNull { it.id == savedModelId }
            ?: GeminiModelOption.ALL_MODELS[0]
        val isDark = repository.isDarkTheme()
        val customKey = repository.getCustomApiKey()

        val activeConv = convs.firstOrNull() ?: repository.createConversation()
        val msgs = repository.getMessages(activeConv.id)

        _uiState.update {
            it.copy(
                conversations = convs,
                currentConversation = activeConv,
                messages = msgs,
                selectedModel = model,
                isDarkTheme = isDark,
                customApiKey = customKey
            )
        }
    }

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun setAttachedImage(uri: Uri?) {
        if (uri == null) {
            _uiState.update { it.copy(attachedImageUri = null, attachedImageBase64 = null) }
            return
        }

        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    val stream = ByteArrayOutputStream()
                    // Compress to max reasonable dimensions for vision API
                    val scaled = if (bitmap.width > 1024 || bitmap.height > 1024) {
                        val ratio = minOf(1024f / bitmap.width, 1024f / bitmap.height)
                        Bitmap.createScaledBitmap(
                            bitmap,
                            (bitmap.width * ratio).toInt(),
                            (bitmap.height * ratio).toInt(),
                            true
                        )
                    } else {
                        bitmap
                    }
                    scaled.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                    val base64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
                    _uiState.update {
                        it.copy(
                            attachedImageUri = uri,
                            attachedImageBase64 = base64
                        )
                    }
                }
            } catch (e: Exception) {
                showSnackbar("Could not load image: ${e.localizedMessage}")
            }
        }
    }

    fun removeAttachedImage() {
        _uiState.update { it.copy(attachedImageUri = null, attachedImageBase64 = null) }
    }

    fun sendMessage(customPrompt: String? = null) {
        val prompt = (customPrompt ?: _uiState.value.inputText).trim()
        val imageBase64 = _uiState.value.attachedImageBase64
        val imageUri = _uiState.value.attachedImageUri?.toString()

        if (prompt.isEmpty() && imageBase64 == null) return

        val conv = _uiState.value.currentConversation ?: repository.createConversation()

        val userMessage = ChatMessage(
            role = MessageRole.USER,
            text = prompt,
            imageBase64 = imageBase64,
            imageUri = imageUri
        )

        // Clear input state immediately
        _uiState.update {
            it.copy(
                inputText = "",
                attachedImageUri = null,
                attachedImageBase64 = null,
                isLoading = true,
                isStreaming = true,
                messages = it.messages + userMessage
            )
        }

        repository.saveMessages(conv.id, _uiState.value.messages)

        // Stream and generate assistant response
        val assistantMessageId = UUID.randomUUID().toString()
        val placeholderMessage = ChatMessage(
            id = assistantMessageId,
            role = MessageRole.GEMINI,
            text = "",
            isStreaming = true
        )

        _uiState.update {
            it.copy(messages = it.messages + placeholderMessage)
        }

        streamingJob?.cancel()
        streamingJob = viewModelScope.launch {
            val systemInstruction = _uiState.value.activeGem?.systemInstruction
            val fullResponse = apiClient.generateResponse(
                prompt = prompt,
                imageBase64 = imageBase64,
                history = _uiState.value.messages.dropLast(2),
                modelId = _uiState.value.selectedModel.id,
                customApiKey = _uiState.value.customApiKey,
                systemInstruction = systemInstruction
            )

            // Simulate realistic typing stream for smooth UI
            var currentLength = 0
            val chunkSize = 6
            while (currentLength < fullResponse.length) {
                currentLength = minOf(currentLength + chunkSize, fullResponse.length)
                val partial = fullResponse.substring(0, currentLength)
                _uiState.update { state ->
                    val updated = state.messages.map { msg ->
                        if (msg.id == assistantMessageId) msg.copy(text = partial) else msg
                    }
                    state.copy(messages = updated)
                }
                delay(20)
            }

            _uiState.update { state ->
                val finalMessages = state.messages.map { msg ->
                    if (msg.id == assistantMessageId) msg.copy(text = fullResponse, isStreaming = false) else msg
                }
                state.copy(
                    isLoading = false,
                    isStreaming = false,
                    messages = finalMessages,
                    conversations = repository.getConversations()
                )
            }

            repository.saveMessages(conv.id, _uiState.value.messages)
            _uiState.update { it.copy(conversations = repository.getConversations()) }
        }
    }

    fun startNewConversation() {
        val newConv = repository.createConversation(
            title = "New conversation",
            modelId = _uiState.value.selectedModel.id
        )
        _uiState.update {
            it.copy(
                currentConversation = newConv,
                messages = emptyList(),
                conversations = repository.getConversations(),
                activeGem = null,
                attachedImageUri = null,
                attachedImageBase64 = null,
                inputText = ""
            )
        }
    }

    fun selectConversation(conversationId: String) {
        val conv = _uiState.value.conversations.firstOrNull { it.id == conversationId } ?: return
        val msgs = repository.getMessages(conversationId)
        val model = GeminiModelOption.ALL_MODELS.firstOrNull { it.id == conv.modelId }
            ?: _uiState.value.selectedModel

        _uiState.update {
            it.copy(
                currentConversation = conv,
                messages = msgs,
                selectedModel = model,
                activeGem = null
            )
        }
    }

    fun deleteConversation(id: String) {
        repository.deleteConversation(id)
        val remaining = repository.getConversations()
        val newActive = if (remaining.isNotEmpty()) remaining[0] else repository.createConversation()
        _uiState.update {
            it.copy(
                conversations = repository.getConversations(),
                currentConversation = newActive,
                messages = repository.getMessages(newActive.id)
            )
        }
        showSnackbar("Conversation deleted")
    }

    fun togglePinConversation(id: String) {
        repository.togglePinConversation(id)
        _uiState.update {
            it.copy(conversations = repository.getConversations())
        }
    }

    fun selectModel(model: GeminiModelOption) {
        repository.setSelectedModelId(model.id)
        _uiState.update {
            it.copy(selectedModel = model, showModelSheet = false)
        }
        showSnackbar("Switched to ${model.name}")
    }

    fun likeMessage(id: String, isLiked: Boolean) {
        val convId = _uiState.value.currentConversation?.id ?: return
        val updated = _uiState.value.messages.map { msg ->
            if (msg.id == id) {
                val newLiked = if (msg.liked == isLiked) null else isLiked
                msg.copy(liked = newLiked)
            } else msg
        }
        _uiState.update { it.copy(messages = updated) }
        repository.saveMessages(convId, updated)
        showSnackbar(if (isLiked) "Feedback saved: Good response" else "Feedback saved: Bad response")
    }

    fun regenerateResponse(messageId: String) {
        val currentMsgs = _uiState.value.messages
        val index = currentMsgs.indexOfFirst { it.id == messageId }
        if (index <= 0) return

        val userMsg = currentMsgs[index - 1]
        if (userMsg.role != MessageRole.USER) return

        // Remove the assistant message and regenerate
        val newMsgs = currentMsgs.toMutableList()
        newMsgs.removeAt(index)
        _uiState.update { it.copy(messages = newMsgs) }

        sendMessage(userMsg.text)
    }

    fun modifyResponse(messageId: String, modificationType: String) {
        val currentMsgs = _uiState.value.messages
        val msg = currentMsgs.firstOrNull { it.id == messageId } ?: return

        val modifierPrompt = when (modificationType) {
            "Shorter" -> "Please summarize your previous response and make it significantly more concise and bulleted."
            "Longer" -> "Please expand on your previous response with more details, examples, and deep explanation."
            "More casual" -> "Please rewrite your previous response in a casual, conversational, friendly tone."
            "More professional" -> "Please rewrite your previous response in a formal, executive, professional tone."
            else -> "Please refine your previous response."
        }

        sendMessage(modifierPrompt)
    }

    fun speakMessage(message: ChatMessage) {
        ttsManager.speak(message.text, message.id)
    }

    // --- Gemini Live Voice Mode ---
    fun openGeminiLive() {
        _uiState.update {
            it.copy(
                isGeminiLiveOpen = true,
                liveState = LiveState.IDLE,
                liveTranscript = "Tap the mic or speak to Gemini",
                liveAiResponse = ""
            )
        }
    }

    fun closeGeminiLive() {
        ttsManager.stop()
        _uiState.update {
            it.copy(
                isGeminiLiveOpen = false,
                liveState = LiveState.IDLE
            )
        }
    }

    fun triggerLiveListening() {
        _uiState.update {
            it.copy(
                liveState = LiveState.LISTENING,
                liveTranscript = "Listening..."
            )
        }
    }

    fun processLiveQuery(spokenQuery: String) {
        if (spokenQuery.isBlank()) return

        _uiState.update {
            it.copy(
                liveState = LiveState.THINKING,
                liveTranscript = "\"$spokenQuery\""
            )
        }

        viewModelScope.launch {
            val response = apiClient.generateResponse(
                prompt = spokenQuery,
                modelId = _uiState.value.selectedModel.id,
                customApiKey = _uiState.value.customApiKey
            )

            _uiState.update {
                it.copy(
                    liveState = LiveState.SPEAKING,
                    liveAiResponse = response
                )
            }

            ttsManager.speak(response, "live_${System.currentTimeMillis()}")

            // Also record into current conversation
            val conv = _uiState.value.currentConversation ?: repository.createConversation()
            val userMsg = ChatMessage(role = MessageRole.USER, text = spokenQuery)
            val geminiMsg = ChatMessage(role = MessageRole.GEMINI, text = response)
            val updated = _uiState.value.messages + userMsg + geminiMsg
            _uiState.update { it.copy(messages = updated) }
            repository.saveMessages(conv.id, updated)
        }
    }

    fun startGem(gem: GeminiGem) {
        val newConv = repository.createConversation(
            title = gem.title,
            modelId = _uiState.value.selectedModel.id
        )
        _uiState.update {
            it.copy(
                currentConversation = newConv,
                activeGem = gem,
                showGemsSheet = false,
                messages = listOf(
                    ChatMessage(
                        role = MessageRole.GEMINI,
                        text = "💎 **Gem Active: ${gem.title}**\n\n${gem.description}\n\nAsk me anything in this domain!"
                    )
                ),
                conversations = repository.getConversations()
            )
        }
        repository.saveMessages(newConv.id, _uiState.value.messages)
    }

    fun toggleTheme() {
        val newTheme = !_uiState.value.isDarkTheme
        repository.setDarkTheme(newTheme)
        _uiState.update { it.copy(isDarkTheme = newTheme) }
    }

    fun saveCustomApiKey(key: String) {
        repository.setCustomApiKey(key)
        _uiState.update { it.copy(customApiKey = key, showSettingsDialog = false) }
        showSnackbar(if (key.isBlank()) "Custom API key cleared" else "Gemini API key saved")
    }

    fun clearAllHistory() {
        repository.clearAllConversations()
        loadInitialState()
        showSnackbar("All chat history cleared")
    }

    fun toggleProfileDialog(show: Boolean) {
        _uiState.update { it.copy(showProfileDialog = show) }
    }

    fun toggleSettingsDialog(show: Boolean) {
        _uiState.update { it.copy(showSettingsDialog = show) }
    }

    fun toggleModelSheet(show: Boolean) {
        _uiState.update { it.copy(showModelSheet = show) }
    }

    fun toggleGemsSheet(show: Boolean) {
        _uiState.update { it.copy(showGemsSheet = show) }
    }

    fun showSnackbar(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }
}
