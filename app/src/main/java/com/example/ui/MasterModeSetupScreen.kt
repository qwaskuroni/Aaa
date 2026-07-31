package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SmartButton
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.automation.OpenAiService
import com.example.model.MasterModeSettings
import com.example.permission.PermissionUtils
import com.example.repository.MasterModeRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterModeSetupScreen(
    repository: MasterModeRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var settings by remember { mutableStateOf(repository.loadSettings()) }
    var apiKeyText by remember { mutableStateOf(settings.openAiApiKey) }
    var systemPromptText by remember { mutableStateOf(settings.systemPrompt) }
    var isApiKeyVisible by remember { mutableStateOf(false) }

    var isTestingConnection by remember { mutableStateOf(false) }
    var testResultText by remember { mutableStateOf<String?>(null) }
    var testResultSuccess by remember { mutableStateOf(false) }

    val isNotifListenerGranted = PermissionUtils.isNotificationListenerEnabled(context)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Master Mode Setup & AI",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_master_setup")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF121824)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            // Header Description
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF8B5CF6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "AI Master",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "IMO AI Master Mode Engine",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC084FC)
                        )
                        Text(
                            text = "Event-driven AI agent for IMO, IMO Beta, IMO HD & IMO Lite.",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Notification Listener Permission Card
            Text(
                text = "System Permission Status",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = if (isNotifListenerGranted) Color(0xFF10B981) else Color(0xFFF59E0B)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Notification Access",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isNotifListenerGranted) "Active — Ready to catch IMO chat alerts." else "Required to auto-detect IMO incoming chats.",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    if (!isNotifListenerGranted) {
                        Button(
                            onClick = { PermissionUtils.openNotificationListenerSettings(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                        ) {
                            Text("Grant", fontSize = 12.sp)
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Granted",
                            tint = Color(0xFF10B981)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. OpenAI API Key Input Section
            Text(
                text = "OpenAI API Credentials",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = Color(0xFF38BDF8))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "API Key (OpenAI / GPT-4o-mini)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = apiKeyText,
                        onValueChange = { apiKeyText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_openai_api_key"),
                        placeholder = { Text("sk-...", color = Color(0xFF64748B)) },
                        singleLine = true,
                        visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                Icon(
                                    imageVector = if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle Visibility",
                                    tint = Color(0xFF94A3B8)
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (apiKeyText.isBlank()) {
                                    Toast.makeText(context, "Please enter an API Key first", Toast.LENGTH_SHORT).show()
                                    return@OutlinedButton
                                }
                                isTestingConnection = true
                                testResultText = null
                                scope.launch {
                                    val service = OpenAiService()
                                    val res = service.generateReply(
                                        apiKey = apiKeyText,
                                        systemPrompt = "Reply with 'OK'",
                                        userMessage = "Test connection"
                                    )
                                    isTestingConnection = false
                                    if (res.isSuccess) {
                                        testResultSuccess = true
                                        testResultText = "Connection Successful: ${res.getOrNull()}"
                                    } else {
                                        testResultSuccess = false
                                        testResultText = res.exceptionOrNull()?.message ?: "Connection Failed"
                                    }
                                }
                            },
                            enabled = !isTestingConnection,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8))
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF38BDF8), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Testing...")
                            } else {
                                Text("Test Connection")
                            }
                        }

                        if (settings.openAiApiKey.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Key Saved", fontSize = 12.sp, color = Color(0xFF10B981))
                            }
                        }
                    }

                    testResultText?.let { text ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = text,
                            fontSize = 12.sp,
                            color = if (testResultSuccess) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. System Prompt & Persona Training
            Text(
                text = "AI System Prompt & Persona Training",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Persona Instructions", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        text = "Guide how the AI responds to incoming messages in IMO chat.",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = false,
                            onClick = {
                                systemPromptText = "আপনি একজন অত্যন্ত বিনয়ী ও বাস্তবসম্মত চ্যাট সহকারী। ব্যবহারকারীর বার্তা বিশ্লেষণ করে ট্রেনিং প্রম্পট অনুসারেই মানুষের মতো ১-২ লাইনের মধ্যে স্বাভাবিক ও সংক্ষিপ্ত উত্তর দিবেন। অতিরিক্ত দীর্ঘ উত্তর দেওয়া নিষেধ।"
                            },
                            label = { Text("Human 1-2 Line Bengali", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(containerColor = Color(0xFF0F172A), labelColor = Color(0xFF38BDF8))
                        )
                        FilterChip(
                            selected = false,
                            onClick = {
                                systemPromptText = "You are a realistic human assistant. Analyze user messages and training prompt carefully to reply naturally in 1-2 concise lines only."
                            },
                            label = { Text("Concise 1-2 Line English", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(containerColor = Color(0xFF0F172A), labelColor = Color(0xFFC084FC))
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = systemPromptText,
                        onValueChange = { systemPromptText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .testTag("input_system_prompt"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFC084FC),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Dynamic Overlay Target Coordinates Overview
            Text(
                text = "Master Mode Target Coordinates Overview",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.SmartButton, contentDescription = null, tint = Color(0xFFF59E0B))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "6 Dedicated Master Targets", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        IconButton(onClick = {
                            settings = settings.copy(
                                notificationX = 540, notificationY = 220,
                                scannerX = 540, scannerY = 900,
                                audioToTextX = 850, audioToTextY = 1300,
                                textInputX = 400, textInputY = 2100,
                                sendX = 980, sendY = 2100,
                                backX = 100, backY = 150
                            )
                            Toast.makeText(context, "Reset target positions to defaults", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset", tint = Color(0xFF94A3B8))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TargetItemRow(label = "1. Notification Target", desc = "Triggers incoming chat", x = settings.notificationX, y = settings.notificationY, color = Color(0xFF00E5FF))
                    TargetItemRow(label = "2. Scanner Target", desc = "Reads active chat nodes", x = settings.scannerX, y = settings.scannerY, color = Color(0xFF38BDF8))
                    TargetItemRow(label = "3. Audio-to-Text Target", desc = "Triggers 'A' icon on voice", x = settings.audioToTextX, y = settings.audioToTextY, color = Color(0xFFA855F7))
                    TargetItemRow(label = "4. Text Input Target", desc = "Focuses chat input box", x = settings.textInputX, y = settings.textInputY, color = Color(0xFFF59E0B))
                    TargetItemRow(label = "5. Send Target", desc = "Taps IMO send icon", x = settings.sendX, y = settings.sendY, color = Color(0xFF10B981))
                    TargetItemRow(label = "6. Back Target", desc = "Executes back action", x = settings.backX, y = settings.backY, color = Color(0xFFEF4444))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save Settings Button
            Button(
                onClick = {
                    val updated = settings.copy(
                        openAiApiKey = apiKeyText.trim(),
                        systemPrompt = systemPromptText.trim()
                    )
                    repository.saveSettings(updated)
                    settings = updated
                    Toast.makeText(context, "Master Mode settings saved successfully!", Toast.LENGTH_SHORT).show()
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_save_master_settings"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6), contentColor = Color.White)
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Save Master Mode Configuration", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun TargetItemRow(label: String, desc: String, x: Int, y: Int, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = desc, fontSize = 11.sp, color = Color(0xFF94A3B8))
        }
        Text(
            text = "($x, $y)",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}
