package com.example.model

data class MasterModeSettings(
    val openAiApiKey: String = "",
    val systemPrompt: String = "আপনি একজন অত্যন্ত বিনয়ী ও বাস্তবসম্মত চ্যাট সহকারী। ব্যবহারকারীর বার্তা বিশ্লেষণ করে ট্রেনিং প্রম্পট অনুসারেই মানুষের মতো ১-২ লাইনের মধ্যে স্বাভাবিক ও সংক্ষিপ্ত উত্তর দিবেন। অতিরিক্ত দীর্ঘ উত্তর দেওয়া নিষেধ।",
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
