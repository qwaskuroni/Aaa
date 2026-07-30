package com.example.model

data class MasterModeSettings(
    val openAiApiKey: String = "",
    val systemPrompt: String = "You are a helpful, brief, and polite assistant replying on IMO chat. Reply concisely in the same language as the incoming message.",
    val isEnabled: Boolean = false,
    val notificationX: Int = 540,
    val notificationY: Int = 220,
    val scannerX: Int = 540,
    val scannerY: Int = 900,
    val audioToTextX: Int = 850,
    val audioToTextY: Int = 1300,
    val textInputX: Int = 400,
    val textInputY: Int = 2100,
    val sendX: Int = 980,
    val sendY: Int = 2100,
    val backX: Int = 100,
    val backY: Int = 150
)
