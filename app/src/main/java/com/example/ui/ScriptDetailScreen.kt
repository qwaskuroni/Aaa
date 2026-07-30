package com.example.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.TouchApp
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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

                Row {
                    OutlinedButton(
                        onClick = {
                            val newOrder = targets.size + 1
                            targets = targets + ClickTarget(
                                order = newOrder,
                                type = TargetType.SINGLE_TAP,
                                delayMs = 500L,
                                label = "Tap #$newOrder"
                            )
                        },
                        modifier = Modifier.testTag("btn_add_action_target")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "+ Action", fontSize = 12.sp)
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
fun TargetDetailCard(
    target: ClickTarget,
    onUpdate: (ClickTarget) -> Unit,
    onDelete: () -> Unit
) {
    var dropdownExpanded by remember { mutableStateOf(false) }

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
                    Box {
                        OutlinedButton(
                            onClick = { dropdownExpanded = true },
                            modifier = Modifier.height(32.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text(text = target.type.displayName, fontSize = 11.sp)
                        }

                        androidx.compose.material3.DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.background(Color(0xFF334155))
                        ) {
                            TargetType.values().forEach { typeOption ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(text = typeOption.displayName, color = Color.White) },
                                    onClick = {
                                        onUpdate(target.copy(type = typeOption, label = "${typeOption.displayName} #${target.order}"))
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
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

