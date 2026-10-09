package com.example.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TtsManager(context: Context) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking = _isSpeaking.asStateFlow()

    private val _currentSpeakingId = MutableStateFlow<String?>(null)
    val currentSpeakingId = _currentSpeakingId.asStateFlow()

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                isInitialized = true
            }
        }

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
                _currentSpeakingId.value = utteranceId
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
                _currentSpeakingId.value = null
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
                _currentSpeakingId.value = null
            }
        })
    }

    fun speak(text: String, id: String) {
        if (!isInitialized || tts == null) return

        if (_isSpeaking.value && _currentSpeakingId.value == id) {
            stop()
            return
        }

        stop()
        // Strip out markdown code blocks and markdown asterisks for natural reading
        val cleanText = text
            .replace(Regex("```[a-zA-Z]*\\n[\\s\\S]*?```"), "Code snippet omitted.")
            .replace(Regex("[*#_`~]"), "")
            .trim()

        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, id)
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
        _currentSpeakingId.value = null
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
