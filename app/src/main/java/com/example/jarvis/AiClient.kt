package com.example.jarvis

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun ask(baseUrl: String, message: String): String = withContext(Dispatchers.IO) {
        val root = baseUrl.trim().trimEnd('/')
        require(root.startsWith("https://")) { "Укажи HTTPS-адрес своего AI-сервера в настройках." }
        val json = JSONObject().put("message", message).toString()
        val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url("$root/chat").post(body).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("AI-сервер ответил: HTTP ${response.code}")
            val raw = response.body?.string().orEmpty()
            val parsed = JSONObject(raw)
            parsed.optString("reply").takeIf { it.isNotBlank() }
                ?: parsed.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content")
                ?: throw IllegalStateException("Сервер должен вернуть JSON с полем reply или OpenAI-совместимый ответ.")
        }
    }
}
