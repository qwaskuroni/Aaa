package com.example.automation

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class OpenAiService {

    suspend fun generateReply(
        apiKey: String,
        systemPrompt: String,
        userMessage: String,
        modelName: String = "gpt-4o-mini"
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("OpenAI API Key is empty. Please set your API key in Master Mode Setup.")
            )
        }

        try {
            val url = URL("https://api.openai.com/v1/chat/completions")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer ${apiKey.trim()}")
            conn.doOutput = true
            conn.connectTimeout = 15000
            conn.readTimeout = 15000

            val formattedSystemPrompt = systemPrompt.ifBlank {
                "আপনি একজন স্বাভাবিক চ্যাট সহকারী। মানুষের মত সংক্ষেপে ১-২ লাইনে উত্তর দিন।"
            } + " (Important Instruction: Keep the response human-like, polite, natural, and strictly within 1-2 sentences in the same language as the user)."

            val jsonBody = JSONObject().apply {
                put("model", "gpt-4o-mini") // Strictly enforcement of gpt-4o-mini model
                val messages = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", formattedSystemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userMessage)
                    })
                }
                put("messages", messages)
                put("max_tokens", 120)
                put("temperature", 0.7)
            }

            OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                writer.write(jsonBody.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                val responseJson = JSONObject(responseStr)
                val choices = responseJson.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val firstChoice = choices.getJSONObject(0)
                    val messageObj = firstChoice.optJSONObject("message")
                    val content = messageObj?.optString("content")?.trim() ?: ""
                    if (content.isNotBlank()) {
                        return@withContext Result.success(content)
                    }
                }
                return@withContext Result.failure(Exception("AI returned empty response"))
            } else {
                val errorStr = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.e(TAG, "OpenAI API Error ($responseCode): $errorStr")
                return@withContext Result.failure(Exception("OpenAI API returned HTTP $responseCode: $errorStr"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to call OpenAI API", e)
            return@withContext Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "OpenAiService"
    }
}

