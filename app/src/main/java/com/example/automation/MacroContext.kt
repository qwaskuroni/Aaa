package com.example.automation

import android.util.Log

object MacroContext {
    private const val TAG = "MacroContext"

    @Volatile
    var scannedMessage: String = ""

    @Volatile
    var autoReply: String = ""

    @Volatile
    var excelFilePath: String = ""

    private val customVariables = mutableMapOf<String, String>()

    fun reset() {
        scannedMessage = ""
        autoReply = ""
        customVariables.clear()
        Log.d(TAG, "MacroContext variables reset")
    }

    fun setVariable(key: String, value: String) {
        customVariables[key] = value
    }

    fun getVariable(key: String): String {
        return when (key) {
            "SCANNED_MESSAGE" -> scannedMessage
            "AUTO_REPLY" -> autoReply
            else -> customVariables[key] ?: ""
        }
    }

    fun resolveVariables(input: String): String {
        if (input.isBlank()) return ""
        var resolved = input
        resolved = resolved.replace("{{SCANNED_MESSAGE}}", scannedMessage)
        resolved = resolved.replace("{{AUTO_REPLY}}", autoReply)
        for ((key, value) in customVariables) {
            resolved = resolved.replace("{{$key}}", value)
        }
        return resolved
    }
}
