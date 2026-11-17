package com.example.jarvis

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class SpeechController(private val context: Context) {
    var onText: ((String) -> Unit)? = null
    var onStatus: ((String) -> Unit)? = null
    var onWakeOnly: (() -> Unit)? = null

    private var recognizer: SpeechRecognizer? = null
    private var wakeMode = false
    private var onDevice = false
    private var languageTag = "ru-RU"
    private var active = false
    private val handler = Handler(Looper.getMainLooper())

    fun start(onDeviceOnly: Boolean, keepWakeListening: Boolean, language: String = "ru-RU") {
        onDevice = onDeviceOnly
        wakeMode = keepWakeListening
        languageTag = language
        active = true
        startRecognition()
    }

    fun stop() {
        active = false
        wakeMode = false
        handler.removeCallbacksAndMessages(null)
        try { recognizer?.cancel() } catch (_: Exception) {}
        onStatus?.invoke("МИКРОФОН ВЫКЛЮЧЕН")
    }

    fun destroy() {
        stop()
        try { recognizer?.destroy() } catch (_: Exception) {}
        recognizer = null
    }

    private fun startRecognition() {
        if (!active) return
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onStatus?.invoke("СЕРВИС РАСПОЗНАВАНИЯ НЕДОСТУПЕН")
            active = false
            return
        }
        try {
            recognizer?.destroy()
            recognizer = if (onDevice && Build.VERSION.SDK_INT >= 31 &&
                SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
                SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            } else if (onDevice) {
                onStatus?.invoke("ОФЛАЙН-РАСПОЗНАВАНИЕ НЕ ПОДДЕРЖИВАЕТСЯ")
                active = false
                return
            } else {
                SpeechRecognizer.createSpeechRecognizer(context)
            }
            recognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: android.os.Bundle?) {
                    onStatus?.invoke(if (wakeMode) "ОЖИДАНИЕ: СКАЖИТЕ «ДЖАРВИС»" else "СЛУШАЮ")
                }
                override fun onBeginningOfSpeech() { onStatus?.invoke("РАСПОЗНАЮ РЕЧЬ") }
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { onStatus?.invoke("ОБРАБОТКА") }
                override fun onError(error: Int) {
                    if (!active) return
                    onStatus?.invoke("ОЖИДАНИЕ РЕЧИ")
                    if (wakeMode && error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS &&
                        error != SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                        handler.postDelayed({ startRecognition() }, 700)
                    } else if (!wakeMode) {
                        active = false
                    }
                }
                override fun onResults(results: android.os.Bundle?) {
                    val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull().orEmpty().trim()
                    if (text.isNotBlank()) {
                        if (wakeMode) {
                            val normalized = text.lowercase(Locale.forLanguageTag(languageTag))
                            val wakeIndex = normalized.indexOf("джарвис")
                            if (wakeIndex >= 0) {
                                val command = text.substring(wakeIndex + "джарвис".length).trim(' ', ',', '.', '!', '?')
                                if (command.isBlank()) onWakeOnly?.invoke() else onText?.invoke(command)
                            }
                        } else {
                            onText?.invoke(text)
                            active = false
                        }
                    }
                    if (active && wakeMode) handler.postDelayed({ startRecognition() }, 650)
                    else if (!wakeMode) onStatus?.invoke("ГОТОВ")
                }
                override fun onPartialResults(partialResults: android.os.Bundle?) {}
                override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
            })
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, onDevice)
            }
            recognizer?.startListening(intent)
        } catch (e: Exception) {
            onStatus?.invoke("ОШИБКА МИКРОФОНА: ${e.localizedMessage ?: "неизвестно"}")
            if (!wakeMode) active = false
        }
    }
}
