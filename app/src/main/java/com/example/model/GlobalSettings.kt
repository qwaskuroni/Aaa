package com.example.model

data class GlobalSettings(
    val defaultDelayMs: Long = 500L,
    val defaultDurationMs: Long = 100L,
    val targetSizeDp: Int = 48,
    val targetOpacityPercent: Int = 90,
    val antiDetectionEnabled: Boolean = false,
    val randomDelayMaxMs: Long = 100L,
    val randomOffsetMaxPx: Int = 10,
    val soundFeedbackEnabled: Boolean = false,
    val vibrationFeedbackEnabled: Boolean = true,
    val isDarkMode: Boolean = true,
    val openAiApiKey: String = "",
    val geminiApiKey: String = ""
)
