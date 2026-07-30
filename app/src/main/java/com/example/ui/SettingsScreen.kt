package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GlobalSettings

@Composable
fun SettingsScreen(
    settings: GlobalSettings,
    onUpdateSettings: (GlobalSettings) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "Global & Anti-Detection Settings",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Anti-Detection Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Anti-Detection Randomization",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Applies slight random position jitter and interval delay variations to mimic human clicks.",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Switch(
                        checked = settings.antiDetectionEnabled,
                        onCheckedChange = {
                            onUpdateSettings(settings.copy(antiDetectionEnabled = it))
                        },
                        modifier = Modifier.testTag("switch_anti_detection"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF00E5FF),
                            checkedTrackColor = Color(0xFF0284C7)
                        )
                    )
                }

                if (settings.antiDetectionEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Max Random Offset: ${settings.randomOffsetMaxPx} px",
                        fontSize = 13.sp,
                        color = Color(0xFF38BDF8)
                    )
                    Slider(
                        value = settings.randomOffsetMaxPx.toFloat(),
                        onValueChange = {
                            onUpdateSettings(settings.copy(randomOffsetMaxPx = it.toInt()))
                        },
                        valueRange = 0f..50f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00E5FF),
                            activeTrackColor = Color(0xFF00E5FF)
                        ),
                        modifier = Modifier.testTag("slider_random_offset")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Max Delay Jitter: ${settings.randomDelayMaxMs} ms",
                        fontSize = 13.sp,
                        color = Color(0xFF38BDF8)
                    )
                    Slider(
                        value = settings.randomDelayMaxMs.toFloat(),
                        onValueChange = {
                            onUpdateSettings(settings.copy(randomDelayMaxMs = it.toLong()))
                        },
                        valueRange = 0f..500f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00E5FF),
                            activeTrackColor = Color(0xFF00E5FF)
                        ),
                        modifier = Modifier.testTag("slider_random_delay")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Appearance & Theme Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Appearance & Theme",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Dark Mode",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (settings.isDarkMode) "Enabled (Dark Slate Theme)" else "Disabled (Light Theme)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Switch(
                        checked = settings.isDarkMode,
                        onCheckedChange = {
                            onUpdateSettings(settings.copy(isDarkMode = it))
                        },
                        modifier = Modifier.testTag("switch_dark_mode"),
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Feedback Settings Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Feedback & Notifications",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Vibration Feedback on Tap",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = settings.vibrationFeedbackEnabled,
                        onCheckedChange = {
                            onUpdateSettings(settings.copy(vibrationFeedbackEnabled = it))
                        },
                        modifier = Modifier.testTag("switch_vibration"),
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Audio Beep Feedback",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = settings.soundFeedbackEnabled,
                        onCheckedChange = {
                            onUpdateSettings(settings.copy(soundFeedbackEnabled = it))
                        },
                        modifier = Modifier.testTag("switch_sound"),
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}
