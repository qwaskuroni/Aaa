package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SmartButton
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ClickTarget
import com.example.model.ScriptModel
import com.example.model.TargetType

@Composable
fun ScriptDetailScreen(
    initialScript: ScriptModel?,
    onSaveScript: (ScriptModel) -> Unit,
    onBack: () -> Unit
) {
    var scriptName by remember { mutableStateOf(initialScript?.name ?: "New Macro Script") }
    var repeatCountStr by remember { mutableStateOf((initialScript?.repeatCount ?: -1).toString()) }
    var repeatIntervalStr by remember { mutableStateOf((initialScript?.repeatIntervalMs ?: 500L).toString()) }
    var randomOffsetStr by remember { mutableStateOf((initialScript?.randomOffsetPx ?: 0).toString()) }

    var targets by remember {
        mutableStateOf(initialScript?.targets ?: listOf(
            ClickTarget(order = 1, type = TargetType.SINGLE_TAP, delayMs = 500L)
        ))
    }

    var showAddActionDialog by remember { mutableStateOf(false) }

    if (showAddActionDialog) {
        ActionTypePickerDialog(
            currentType = null,
            onTypeSelected = { selectedType ->
                val newOrder = targets.size + 1
                targets = targets + ClickTarget(
                    order = newOrder,
                    type = selectedType,
                    delayMs = 500L,
                    label = "${selectedType.displayName} #$newOrder"
                )
                showAddActionDialog = false
            },
            onDismiss = { showAddActionDialog = false }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121824))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back")) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = if (initialScript == null) "Create Macro Script" else "Edit Macro Script",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        val script = ScriptModel(
                            id = initialScript?.id ?: 0,
                            name = scriptName,
                            repeatCount = repeatCountStr.toIntOrNull() ?: -1,
                            repeatIntervalMs = repeatIntervalStr.toLongOrNull() ?: 500L,
                            randomOffsetPx = randomOffsetStr.toIntOrNull() ?: 0,
                            createdAt = initialScript?.createdAt ?: System.currentTimeMillis(),
                            isFavorite = initialScript?.isFavorite ?: false,
                            targets = targets
                        )
                        onSaveScript(script)
                        onBack()
                    },
                    modifier = Modifier.testTag("btn_save_script"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color(0xFF0F172A))
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Save", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Script Name Input
            OutlinedTextField(
                value = scriptName,
                onValueChange = { scriptName = it },
                label = { Text("Script Name", color = Color(0xFF94A3B8)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_script_name"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Parameters Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = repeatCountStr,
                    onValueChange = { repeatCountStr = it },
                    label = { Text("Repeat (-1=Inf)", color = Color(0xFF94A3B8), fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_repeat_count"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = repeatIntervalStr,
                    onValueChange = { repeatIntervalStr = it },
                    label = { Text("Loop Delay (ms)", color = Color(0xFF94A3B8), fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_repeat_interval"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Automation Queue Sequence Visualizer Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Automation Queue Flow",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (targets.isEmpty()) {
                        Text(
                            text = "Queue is empty. Add action buttons below to build sequence.",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            targets.forEachIndexed { idx, target ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                when (target.type) {
                                                    TargetType.SINGLE_TAP -> Color(0xFF00E5FF)
                                                    TargetType.DOUBLE_TAP -> Color(0xFF38BDF8)
                                                    TargetType.LONG_PRESS -> Color(0xFF818CF8)
                                                    TargetType.SWIPE -> Color(0xFFF59E0B)
                                                    TargetType.WAIT -> Color(0xFF94A3B8)
                                                    TargetType.TEXT_INPUT -> Color(0xFFA855F7)
                                                    TargetType.CLIPBOARD_PASTE -> Color(0xFF10B981)
                                                    else -> Color(0xFFEC4899)
                                                },
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Button ${target.order}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        text = "${target.type.displayName} (Wait ${target.delayMs}ms)",
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                }

                                if (idx < targets.size - 1) {
                                    Text(
                                        text = "  ↓ delayBefore ${targets[idx + 1].delayMs}ms",
                                        fontSize = 11.sp,
                                        color = Color(0xFF00E5FF),
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(start = 12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Target List Title & Add Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Action Buttons (${targets.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { showAddActionDialog = true },
                        modifier = Modifier.testTag("btn_add_action_target"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF))
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "+ Action Type", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    OutlinedButton(
                        onClick = {
                            val newOrder = targets.size + 1
                            targets = targets + ClickTarget(
                                order = newOrder,
                                type = TargetType.SMART_MESSAGE_SCANNER,
                                delayMs = 500L,
                                label = "Smart Message Scanner #$newOrder"
                            )
                        },
                        modifier = Modifier.testTag("btn_add_smart_scanner_target")
                    ) {
                        Text(text = "🔍 + Smart Scanner", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3B82F6))
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    OutlinedButton(
                        onClick = {
                            val newOrder = targets.size + 1
                            targets = targets + ClickTarget(
                                order = newOrder,
                                type = TargetType.XLS_SMART_REPLY,
                                delayMs = 500L,
                                label = "XLS Smart Reply #$newOrder"
                            )
                        },
                        modifier = Modifier.testTag("btn_add_xls_reply_target")
                    ) {
                        Text(text = "📊 + XLS Reply", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B5CF6))
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    OutlinedButton(
                        onClick = {
                            val newOrder = targets.size + 1
                            targets = targets + ClickTarget(
                                order = newOrder,
                                type = TargetType.VOICE_TO_TEXT,
                                delayMs = 500L,
                                label = "Voice To Text #$newOrder"
                            )
                        },
                        modifier = Modifier.testTag("btn_add_voice_to_text_target")
                    ) {
                        Text(text = "🎙️ + Voice To Text", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    OutlinedButton(
                        onClick = {
                            val newOrder = targets.size + 1
                            targets = targets + ClickTarget(
                                order = newOrder,
                                type = TargetType.AI_INTENT_SCANNER,
                                delayMs = 500L,
                                label = "AI Intent Scanner #$newOrder"
                            )
                        },
                        modifier = Modifier.testTag("btn_add_ai_intent_scanner_target")
                    ) {
                        Text(text = "🧠 + AI Intent Scanner", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF06B6D4))
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    OutlinedButton(
                        onClick = {
                            val newOrder = targets.size + 1
                            targets = targets + ClickTarget(
                                order = newOrder,
                                type = TargetType.OPEN_UNREAD_CHATS,
                                delayMs = 500L,
                                label = "Open Unread Chats #$newOrder"
                            )
                        },
                        modifier = Modifier.testTag("btn_add_unread_chats_target")
                    ) {
                        Text(text = "📩 + Unread Chats", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    OutlinedButton(
                        onClick = {
                            val newOrder = targets.size + 1
                            targets = targets + ClickTarget(
                                order = newOrder,
                                type = TargetType.TEXT_INPUT,
                                delayMs = 500L,
                                label = "Text Input #$newOrder"
                            )
                        },
                        modifier = Modifier.testTag("btn_add_text_target")
                    ) {
                        Icon(imageVector = Icons.Default.TouchApp, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "+ Text", fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    OutlinedButton(
                        onClick = {
                            val newOrder = targets.size + 1
                            targets = targets + ClickTarget(
                                order = newOrder,
                                type = TargetType.VIDEO_PLAY,
                                delayMs = 500L,
                                label = "Video Play #$newOrder"
                            )
                        },
                        modifier = Modifier.testTag("btn_add_video_target")
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "+ Video", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(targets) { index, target ->
                    TargetDetailCard(
                        target = target,
                        onUpdate = { updatedTarget ->
                            val mutable = targets.toMutableList()
                            mutable[index] = updatedTarget
                            targets = mutable
                        },
                        onDelete = {
                            val mutable = targets.toMutableList()
                            mutable.removeAt(index)
                            // Reorder remaining
                            targets = mutable.mapIndexed { idx, item -> item.copy(order = idx + 1) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ActionTypePickerDialog(
    currentType: TargetType? = null,
    onTypeSelected: (TargetType) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SmartButton,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Select Action Type",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TargetType.values().forEach { typeOption ->
                    val isSelected = typeOption == currentType
                    val optionColor = when (typeOption) {
                        TargetType.SINGLE_TAP -> Color(0xFF00E5FF)
                        TargetType.DOUBLE_TAP -> Color(0xFF38BDF8)
                        TargetType.LONG_PRESS -> Color(0xFF818CF8)
                        TargetType.SWIPE -> Color(0xFFF59E0B)
                        TargetType.WAIT -> Color(0xFF94A3B8)
                        TargetType.TEXT_INPUT -> Color(0xFFA855F7)
                        TargetType.CLIPBOARD_PASTE -> Color(0xFF10B981)
                        TargetType.SMART_MESSAGE_SCANNER -> Color(0xFF3B82F6)
                        TargetType.XLS_SMART_REPLY -> Color(0xFF8B5CF6)
                        TargetType.VOICE_TO_TEXT -> Color(0xFFF59E0B)
                        TargetType.AI_INTENT_SCANNER -> Color(0xFF06B6D4)
                        TargetType.OPEN_UNREAD_CHATS -> Color(0xFF10B981)
                        else -> Color(0xFFEC4899)
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onTypeSelected(typeOption)
                            },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) optionColor.copy(alpha = 0.2f) else Color(0xFF0F172A)
                        ),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) optionColor else Color(0xFF334155)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(optionColor)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = typeOption.displayName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = optionColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        },
        containerColor = Color(0xFF1E293B),
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun TargetDetailCard(
    target: ClickTarget,
    onUpdate: (ClickTarget) -> Unit,
    onDelete: () -> Unit
) {
    var showPickerModal by remember { mutableStateOf(false) }

    if (showPickerModal) {
        ActionTypePickerDialog(
            currentType = target.type,
            onTypeSelected = { selectedType ->
                onUpdate(target.copy(type = selectedType, label = "${selectedType.displayName} #${target.order}"))
                showPickerModal = false
            },
            onDismiss = { showPickerModal = false }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#${target.order} ${target.type.displayName}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (target.type) {
                        TargetType.SINGLE_TAP -> Color(0xFF00E5FF)
                        TargetType.DOUBLE_TAP -> Color(0xFF38BDF8)
                        TargetType.LONG_PRESS -> Color(0xFF818CF8)
                        TargetType.SWIPE -> Color(0xFFF59E0B)
                        TargetType.WAIT -> Color(0xFF94A3B8)
                        TargetType.TEXT_INPUT -> Color(0xFFA855F7)
                        TargetType.CLIPBOARD_PASTE -> Color(0xFF10B981)
                        else -> Color(0xFFEC4899)
                    }
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { showPickerModal = true },
                        modifier = Modifier.height(32.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text(text = target.type.displayName, fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(onClick = onDelete, modifier = Modifier.testTag("btn_delete_target_${target.order}")) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Target", tint = Color(0xFFEF4444))
                    }
                }
            }

            if (target.type == TargetType.TEXT_INPUT) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Text Content (Multi-line & Emoji supported):",
                    fontSize = 12.sp,
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = target.textContent,
                    onValueChange = { newText ->
                        // Auto-saves immediately
                        onUpdate(target.copy(textContent = newText))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_text_content_${target.order}"),
                    placeholder = { Text("Enter text or emojis to input...", color = Color(0xFF64748B)) },
                    minLines = 3,
                    maxLines = 6,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFA855F7),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Text(
                    text = "✓ Saved automatically",
                    fontSize = 10.sp,
                    color = Color(0xFF10B981),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            if (target.type == TargetType.VIDEO_PLAY) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri: android.net.Uri? ->
                    if (uri != null) {
                        try {
                            context.contentResolver.takePersistableUriPermission(
                                uri,
                                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        } catch (e: Exception) {
                            // ignore if not persistable
                        }
                        onUpdate(target.copy(mediaUri = uri.toString()))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Select Video from Gallery:",
                    fontSize = 12.sp,
                    color = Color(0xFFFF2D55),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = { launcher.launch("video/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2D55)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_select_gallery_video_${target.order}")
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "🎥 Choose Video from Gallery", color = Color.White, fontWeight = FontWeight.Bold)
                }
                if (target.mediaUri.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Selected URI: ${target.mediaUri}",
                        fontSize = 10.sp,
                        color = Color(0xFF38BDF8)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "✓ Selected video will play in a small window when this step executes.",
                    fontSize = 10.sp,
                    color = Color(0xFF10B981)
                )
            }

            if (target.type == TargetType.SMART_MESSAGE_SCANNER) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "🔍 Smart Message Scanner",
                    fontSize = 12.sp,
                    color = Color(0xFF3B82F6),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "✓ Automatically extracts the latest unread customer text message.\n✓ Ignores voice, call logs, photos, videos, stickers, gifs, urls, emoji-only & system msgs.\n✓ Saves result in variable: {{SCANNED_MESSAGE}}",
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            if (target.type == TargetType.XLS_SMART_REPLY) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val excelLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri: android.net.Uri? ->
                    if (uri != null) {
                        try {
                            context.contentResolver.takePersistableUriPermission(
                                uri,
                                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        } catch (e: Exception) {
                            // ignore
                        }
                        var savedPathOrUri = uri.toString()
                        var extractedRulesText = ""
                        try {
                            val extension = if (uri.toString().endsWith(".csv", ignoreCase = true)) "csv" else "xlsx"
                            val destFile = java.io.File(context.filesDir, "target_excel_${target.order}.$extension")
                            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                java.io.FileOutputStream(destFile).use { outputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                            }
                            if (destFile.exists() && destFile.length() > 0) {
                                savedPathOrUri = destFile.absolutePath
                            }
                            val rules = com.example.automation.XlsSmartReplyEngine.loadRules(context, savedPathOrUri, "")
                            if (rules.isNotEmpty()) {
                                extractedRulesText = rules.joinToString("\n") { row ->
                                    "${row.keywords.joinToString(", ")} : ${row.reply}"
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        onUpdate(
                            target.copy(
                                excelFilePath = savedPathOrUri,
                                excelRulesContent = if (extractedRulesText.isNotBlank()) extractedRulesText else target.excelRulesContent
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "📊 XLS Smart Reply Engine",
                    fontSize = 12.sp,
                    color = Color(0xFF8B5CF6),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = { excelLauncher.launch("*/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "📁 Choose Excel (.xlsx) File", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                if (target.excelFilePath.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "File URI: ${target.excelFilePath}", fontSize = 10.sp, color = Color(0xFF38BDF8))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Or Enter Rules (Keyword : Reply):",
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8)
                )
                OutlinedTextField(
                    value = target.excelRulesContent,
                    onValueChange = { onUpdate(target.copy(excelRulesContent = it)) },
                    placeholder = { Text("namber, bkash, nagad : 01889411602\nprice : 500 tk", fontSize = 11.sp, color = Color(0xFF64748B)) },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "✓ Matches {{SCANNED_MESSAGE}} with keywords & stores reply in variable: {{AUTO_REPLY}}",
                    fontSize = 10.sp,
                    color = Color(0xFF10B981)
                )
            }

            if (target.type == TargetType.VOICE_TO_TEXT) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "🎙️ Voice To Text Settings",
                    fontSize = 12.sp,
                    color = Color(0xFFF59E0B),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = target.voiceToTextDelayBeforeMs.toString(),
                        onValueChange = {
                            val delay = it.toLongOrNull() ?: 2000L
                            onUpdate(target.copy(voiceToTextDelayBeforeMs = delay))
                        },
                        label = { Text("Delay Before Click (ms)", fontSize = 10.sp, color = Color(0xFF94A3B8)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = target.voiceToTextWaitAfterMs.toString(),
                        onValueChange = {
                            val wait = it.toLongOrNull() ?: 2000L
                            onUpdate(target.copy(voiceToTextWaitAfterMs = wait))
                        },
                        label = { Text("Wait After Click (ms)", fontSize = 10.sp, color = Color(0xFF94A3B8)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "✓ Automatically converts detected voice message to text using existing 'A' button.",
                    fontSize = 10.sp,
                    color = Color(0xFF10B981)
                )
            }

            if (target.type == TargetType.AI_INTENT_SCANNER) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "🧠 AI Intent Scanner Settings",
                    fontSize = 12.sp,
                    color = Color(0xFF06B6D4),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = target.aiIntentApiKey,
                    onValueChange = { onUpdate(target.copy(aiIntentApiKey = it)) },
                    label = { Text("OpenAI API Key (Optional Override)", fontSize = 10.sp, color = Color(0xFF94A3B8)) },
                    placeholder = { Text("Leave empty to use Master Mode setup key", fontSize = 10.sp, color = Color(0xFF64748B)) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "✓ Analyzes latest customer message (text/voice-transcription) and returns Intent ID (e.g. PAYMENT_NUMBER, LOCATION, PRICE, UNKNOWN).",
                    fontSize = 10.sp,
                    color = Color(0xFF10B981)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = target.delayMs.toString(),
                    onValueChange = {
                        val delay = it.toLongOrNull() ?: target.delayMs
                        onUpdate(target.copy(delayMs = delay))
                    },
                    label = { Text("Delay Before (ms)", fontSize = 10.sp, color = Color(0xFF94A3B8)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = target.durationMs.toString(),
                    onValueChange = {
                        val duration = it.toLongOrNull() ?: target.durationMs
                        onUpdate(target.copy(durationMs = duration))
                    },
                    label = { Text("Duration (ms)", fontSize = 10.sp, color = Color(0xFF94A3B8)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )
            }
        }
    }
}

