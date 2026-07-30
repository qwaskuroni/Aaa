package com.example.repository

import android.content.Context
import com.example.model.GlobalSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences("auto_clicker_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<GlobalSettings> = _settings.asStateFlow()

    private fun loadSettings(): GlobalSettings {
        return GlobalSettings(
            defaultDelayMs = prefs.getLong("default_delay_ms", 500L),
            defaultDurationMs = prefs.getLong("default_duration_ms", 100L),
            targetSizeDp = prefs.getInt("target_size_dp", 48),
            targetOpacityPercent = prefs.getInt("target_opacity_percent", 90),
            antiDetectionEnabled = prefs.getBoolean("anti_detection_enabled", false),
            randomDelayMaxMs = prefs.getLong("random_delay_max_ms", 100L),
            randomOffsetMaxPx = prefs.getInt("random_offset_max_px", 10),
            soundFeedbackEnabled = prefs.getBoolean("sound_feedback_enabled", false),
            vibrationFeedbackEnabled = prefs.getBoolean("vibration_feedback_enabled", true),
            isDarkMode = prefs.getBoolean("is_dark_mode", true)
        )
    }

    fun updateSettings(newSettings: GlobalSettings) {
        prefs.edit()
            .putLong("default_delay_ms", newSettings.defaultDelayMs)
            .putLong("default_duration_ms", newSettings.defaultDurationMs)
            .putInt("target_size_dp", newSettings.targetSizeDp)
            .putInt("target_opacity_percent", newSettings.targetOpacityPercent)
            .putBoolean("anti_detection_enabled", newSettings.antiDetectionEnabled)
            .putLong("random_delay_max_ms", newSettings.randomDelayMaxMs)
            .putInt("random_offset_max_px", newSettings.randomOffsetMaxPx)
            .putBoolean("sound_feedback_enabled", newSettings.soundFeedbackEnabled)
            .putBoolean("vibration_feedback_enabled", newSettings.vibrationFeedbackEnabled)
            .putBoolean("is_dark_mode", newSettings.isDarkMode)
            .apply()

        _settings.value = newSettings
    }
}
