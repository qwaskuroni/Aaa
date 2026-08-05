package com.example.automation

import android.content.Context
import android.util.Log
import com.example.accessibility.AutoClickerAccessibilityService
import com.example.repository.MasterModeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object AiIntentScannerEngine {

    private const val TAG = "AiIntentScannerEngine"

    suspend fun scanAndClassifyIntent(
        service: AutoClickerAccessibilityService?,
        customApiKey: String = ""
    ): Boolean {
        if (service == null) {
            Log.w(TAG, "Accessibility service is null. Defaulting intent to UNKNOWN")
            MacroContext.scannedMessage = "UNKNOWN"
            MacroContext.setVariable("INTENT_ID", "UNKNOWN")
            return true
        }

        // 1. Determine message source: Prioritize Voice-To-Text / existing scanned message, fallback to active screen scan
        var inputMessage = MacroContext.scannedMessage.trim()
        if (inputMessage.isBlank()) {
            Log.d(TAG, "No cached message in MacroContext. Scanning screen for latest customer message...")
            inputMessage = SmartMessageScanner.scanLatestCustomerMessage(service).trim()
        }

        if (inputMessage.isBlank()) {
            Log.d(TAG, "No customer message found on screen or in context. Returning UNKNOWN intent.")
            MacroContext.scannedMessage = "UNKNOWN"
            MacroContext.setVariable("INTENT_ID", "UNKNOWN")
            return true
        }

        MacroContext.setVariable("RAW_SCANNED_MESSAGE", inputMessage)
        Log.d(TAG, "Analyzing customer message for intent: '$inputMessage'")

        // 2. Resolve OpenAI API Key (Custom target key -> Master Mode Repository key)
        val apiKey = resolveApiKey(service.applicationContext, customApiKey)
        if (apiKey.isBlank()) {
            Log.w(TAG, "OpenAI API Key is missing. Defaulting intent to UNKNOWN.")
            MacroContext.scannedMessage = "UNKNOWN"
            MacroContext.setVariable("INTENT_ID", "UNKNOWN")
            return true
        }

        // 3. Query ChatGPT API for Intent Classification
        val intentId = queryIntentFromGpt(apiKey, inputMessage)

        // 4. Update MacroContext for downstream macro actions
        MacroContext.scannedMessage = intentId
        MacroContext.setVariable("INTENT_ID", intentId)

        Log.d(TAG, "AI Intent Scanner completed successfully. Classified Intent ID: '$intentId'")
        return true
    }

    private fun resolveApiKey(context: Context, customApiKey: String): String {
        if (customApiKey.isNotBlank()) return customApiKey.trim()
        val repository = MasterModeRepository(context)
        return repository.settings.value.openAiApiKey.trim()
    }

    private suspend fun queryIntentFromGpt(
        apiKey: String,
        userMessage: String
    ): String = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.openai.com/v1/chat/completions")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.doOutput = true
            conn.connectTimeout = 12000
            conn.readTimeout = 12000

            val systemPrompt = """
                You are a strict Intent Classification Engine for customer service messages.
                Analyze the customer message (which can be in Bengali, English, Banglish, or Mixed Language).
                You must classify the customer's intent into ONE single Intent ID.

                Examples of Intent IDs:
                - PAYMENT_NUMBER (asking for payment details, bkash, nagad, rocket, number, money transfer)
                - LOCATION (asking for address, shop location, map, place)
                - PRICE (asking for price, cost, rate, how much, total)
                - VIDEO_CALL (asking for video call, imo call)
                - NAME (asking for name, who are you)
                - GREETING (saying hi, hello, salam, hey)
                - UNKNOWN (if confidence is below 80%, ambiguous, or unlisted)

                CRITICAL OUTPUT RULES:
                1. Output EXACTLY ONE Intent ID in UPPERCASE (e.g., PAYMENT_NUMBER, LOCATION, PRICE, UNKNOWN).
                2. NEVER output explanations, markdown formatting, JSON, or punctuation.
                3. NEVER write a reply or sentence to the customer.
                4. If confidence is below 80%, output: UNKNOWN
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("model", "gpt-4o-mini")
                val messages = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userMessage)
                    })
                }
                put("messages", messages)
                put("max_tokens", 20)
                put("temperature", 0.0)
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
                    val rawContent = choices.getJSONObject(0)
                        .optJSONObject("message")
                        .optString("content") ?: ""
                    
                    return@withContext sanitizeIntentId(rawContent)
                }
            } else {
                val errorStr = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.e(TAG, "OpenAI API Error ($responseCode): $errorStr")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to call OpenAI API for Intent Scanner", e)
        }

        return@withContext "UNKNOWN"
    }

    private fun sanitizeIntentId(raw: String): String {
        var clean = raw.trim()
        // Strip markdown backticks or code blocks
        clean = clean.replace("```", "").replace("```json", "").replace("```text", "").trim()
        // Strip quotes and punctuation
        clean = clean.replace("\"", "").replace("'", "").replace(".", "").replace(",", "").trim()
        
        // Take the first line or word if multiple lines returned
        if (clean.contains("\n")) {
            clean = clean.substringBefore("\n").trim()
        }

        clean = clean.uppercase()

        // Match standard Intent ID pattern (letters, numbers, underscore)
        val matched = Regex("[A-Z0-9_]+").find(clean)?.value ?: "UNKNOWN"
        
        return if (matched.isNotBlank()) matched else "UNKNOWN"
    }
}
