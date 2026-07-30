package com.example.utils

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class FeedbackUtils(private val context: Context) {

    private var vibrator: Vibrator? = null
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibrator = vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 60)
        } catch (e: Exception) {
            // Fallback gracefully if hardware audio/vibration fails
        }
    }

    fun vibrate(durationMs: Long = 40L) {
        try {
            vibrator?.let { v ->
                if (v.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        v.vibrate(durationMs)
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun playClickSound() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 30)
        } catch (e: Exception) {
            // Ignore
        }
    }
}
