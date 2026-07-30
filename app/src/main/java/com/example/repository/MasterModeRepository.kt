package com.example.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.model.MasterModeSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MasterModeRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<MasterModeSettings> = _settings.asStateFlow()

    fun loadSettings(): MasterModeSettings {
        return MasterModeSettings(
            openAiApiKey = prefs.getString(KEY_API_KEY, "") ?: "",
            systemPrompt = prefs.getString(KEY_SYSTEM_PROMPT, "You are a helpful, brief, and polite assistant replying on IMO chat. Reply concisely in the same language as the incoming message.") ?: "",
            isEnabled = prefs.getBoolean(KEY_IS_ENABLED, false),
            notificationX = prefs.getInt(KEY_NOTIF_X, 540),
            notificationY = prefs.getInt(KEY_NOTIF_Y, 220),
            scannerX = prefs.getInt(KEY_SCAN_X, 540),
            scannerY = prefs.getInt(KEY_SCAN_Y, 900),
            audioToTextX = prefs.getInt(KEY_AUDIO_X, 850),
            audioToTextY = prefs.getInt(KEY_AUDIO_Y, 1300),
            textInputX = prefs.getInt(KEY_INPUT_X, 400),
            textInputY = prefs.getInt(KEY_INPUT_Y, 2100),
            sendX = prefs.getInt(KEY_SEND_X, 980),
            sendY = prefs.getInt(KEY_SEND_Y, 2100),
            backX = prefs.getInt(KEY_BACK_X, 100),
            backY = prefs.getInt(KEY_BACK_Y, 150)
        )
    }

    fun saveSettings(newSettings: MasterModeSettings) {
        prefs.edit()
            .putString(KEY_API_KEY, newSettings.openAiApiKey)
            .putString(KEY_SYSTEM_PROMPT, newSettings.systemPrompt)
            .putBoolean(KEY_IS_ENABLED, newSettings.isEnabled)
            .putInt(KEY_NOTIF_X, newSettings.notificationX)
            .putInt(KEY_NOTIF_Y, newSettings.notificationY)
            .putInt(KEY_SCAN_X, newSettings.scannerX)
            .putInt(KEY_SCAN_Y, newSettings.scannerY)
            .putInt(KEY_AUDIO_X, newSettings.audioToTextX)
            .putInt(KEY_AUDIO_Y, newSettings.audioToTextY)
            .putInt(KEY_INPUT_X, newSettings.textInputX)
            .putInt(KEY_INPUT_Y, newSettings.textInputY)
            .putInt(KEY_SEND_X, newSettings.sendX)
            .putInt(KEY_SEND_Y, newSettings.sendY)
            .putInt(KEY_BACK_X, newSettings.backX)
            .putInt(KEY_BACK_Y, newSettings.backY)
            .apply()

        _settings.value = newSettings
    }

    fun updateApiKey(apiKey: String) {
        val current = _settings.value
        saveSettings(current.copy(openAiApiKey = apiKey))
    }

    fun updateSystemPrompt(prompt: String) {
        val current = _settings.value
        saveSettings(current.copy(systemPrompt = prompt))
    }

    fun updateTargetCoordinates(
        notificationX: Int, notificationY: Int,
        scannerX: Int, scannerY: Int,
        audioX: Int, audioY: Int,
        inputX: Int, inputY: Int,
        sendX: Int, sendY: Int,
        backX: Int, backY: Int
    ) {
        val current = _settings.value
        saveSettings(
            current.copy(
                notificationX = notificationX, notificationY = notificationY,
                scannerX = scannerX, scannerY = scannerY,
                audioToTextX = audioX, audioToTextY = audioY,
                textInputX = inputX, textInputY = inputY,
                sendX = sendX, sendY = sendY,
                backX = backX, backY = backY
            )
        )
    }

    companion object {
        private const val PREFS_NAME = "master_mode_settings_prefs"
        private const val KEY_API_KEY = "open_ai_api_key"
        private const val KEY_SYSTEM_PROMPT = "system_prompt"
        private const val KEY_IS_ENABLED = "is_enabled"
        private const val KEY_NOTIF_X = "notif_x"
        private const val KEY_NOTIF_Y = "notif_y"
        private const val KEY_SCAN_X = "scan_x"
        private const val KEY_SCAN_Y = "scan_y"
        private const val KEY_AUDIO_X = "audio_x"
        private const val KEY_AUDIO_Y = "audio_y"
        private const val KEY_INPUT_X = "input_x"
        private const val KEY_INPUT_Y = "input_y"
        private const val KEY_SEND_X = "send_x"
        private const val KEY_SEND_Y = "send_y"
        private const val KEY_BACK_X = "back_x"
        private const val KEY_BACK_Y = "back_y"
    }
}
