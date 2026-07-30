package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ClickTarget
import com.example.model.ScriptModel
import com.example.model.TargetType
import com.example.utils.ScriptJsonUtils

@Composable
fun ScriptsScreen(
    scripts: List<ScriptModel>,
    onCreateNewScript: () -> Unit,
    onEditScript: (ScriptModel) -> Unit,
    onRunScript: (ScriptModel) -> Unit,
    onDeleteScript: (Long) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onDuplicateScript: (ScriptModel) -> Unit,
    onRenameScript: (Long, String) -> Unit,
    onImportScriptJson: (String) -> Boolean
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var searchQuery by remember { mutableStateOf("") }

    var renamingScript by remember { mutableStateOf<ScriptModel?>(null) }
    var exportingScript by remember { mutableStateOf<ScriptModel?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }

    val filteredScripts = scripts.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121824))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Profiles & Scripts",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Unlimited profiles • Stores buttons, delays, texts",
                        fontSize = 11.sp,
                        color = Color(0xFF38BDF8)
                    )
                }

                OutlinedButton(
                    onClick = { showImportDialog = true },
                    modifier = Modifier.testTag("btn_import_script"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF))
                ) {
                    Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Import", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_search_scripts"),
                placeholder = { Text("Search profiles...", color = Color(0xFF64748B)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedContainerColor = Color(0xFF1E293B),
                    unfocusedContainerColor = Color(0xFF1E293B),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredScripts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No saved profiles found",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap '+' to create your first visual auto click profile",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredScripts, key = { it.id }) { script ->
                        ScriptCardItem(
                            script = script,
                            onEdit = { onEditScript(script) },
                            onRun = { onRunScript(script) },
                            onDuplicate = { onDuplicateScript(script) },
                            onRename = { renamingScript = script },
                            onExport = { exportingScript = script },
                            onDelete = { onDeleteScript(script.id) },
                            onToggleFavorite = { onToggleFavorite(script.id) }
                        )
                    }
                }
            }
        }

        // FAB to create new script
        FloatingActionButton(
            onClick = onCreateNewScript,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .testTag("fab_create_script"),
            containerColor = Color(0xFF00E5FF),
            contentColor = Color(0xFF0F172A)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Create Profile")
        }
    }

    // Rename Profile Dialog
    renamingScript?.let { script ->
        var newName by remember { mutableStateOf(script.name) }
        AlertDialog(
            onDismissRequest = { renamingScript = null },
            title = { Text("Rename Profile", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Profile Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newName.isNotBlank()) {
                        onRenameScript(script.id, newName.trim())
                    }
                    renamingScript = null
                }) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingScript = null }) {
                    Text("Cancel")
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    // Export Profile Dialog
    exportingScript?.let { script ->
        val jsonString = remember(script) { ScriptJsonUtils.exportToJson(script) }
        AlertDialog(
            onDismissRequest = { exportingScript = null },
            title = { Text("Export Profile: ${script.name}", color = Color.White) },
            text = {
                Column {
                    Text(
                        text = "JSON configuration containing all buttons, coordinates, delays, and text content:",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = jsonString,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    clipboardManager.setText(AnnotatedString(jsonString))
                    Toast.makeText(context, "Profile JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
                    exportingScript = null
                }) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy to Clipboard")
                }
            },
            dismissButton = {
                TextButton(onClick = { exportingScript = null }) {
                    Text("Close")
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    // Import Profile Dialog
    if (showImportDialog) {
        var importJsonText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Profile", color = Color.White) },
            text = {
                Column {
                    Text(
                        text = "Paste profile JSON text below to import:",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        placeholder = { Text("Paste JSON here...", color = Color(0xFF64748B)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (importJsonText.isNotBlank()) {
                        val success = onImportScriptJson(importJsonText.trim())
                        if (success) {
                            Toast.makeText(context, "Profile imported successfully!", Toast.LENGTH_SHORT).show()
                            showImportDialog = false
                        } else {
                            Toast.makeText(context, "Invalid JSON profile format!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) {
                    Text("Import Profile")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}

@Composable
fun ScriptCardItem(
    script: ScriptModel,
    onEdit: () -> Unit,
    onRun: () -> Unit,
    onDuplicate: () -> Unit,
    onRename: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit
) {
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = script.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${script.targets.size} Action Buttons • ${if (script.repeatCount == -1) "Infinite Loop" else "${script.repeatCount} Loops"}",
                        fontSize = 12.sp,
                        color = Color(0xFF38BDF8)
                    )
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.testTag("btn_favorite_${script.id}")
                ) {
                    Icon(
                        imageVector = if (script.isFavorite) Icons.Default.Star else Icons.Outlined.StarOutline,
                        contentDescription = "Favorite",
                        tint = if (script.isFavorite) Color(0xFFF59E0B) else Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    IconButton(onClick = onDuplicate, modifier = Modifier.testTag("btn_duplicate_${script.id}")) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Duplicate Profile",
                            tint = Color(0xFF38BDF8)
                        )
                    }

                    IconButton(onClick = onRename, modifier = Modifier.testTag("btn_rename_${script.id}")) {
                        Icon(
                            imageVector = Icons.Default.DriveFileRenameOutline,
                            contentDescription = "Rename Profile",
                            tint = Color(0xFF10B981)
                        )
                    }

                    IconButton(onClick = onExport, modifier = Modifier.testTag("btn_export_${script.id}")) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export Profile",
                            tint = Color(0xFFA855F7)
                        )
                    }

                    IconButton(onClick = onEdit, modifier = Modifier.testTag("btn_edit_${script.id}")) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = Color(0xFF94A3B8)
                        )
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.testTag("btn_delete_${script.id}")) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Profile",
                            tint = Color(0xFFEF4444)
                        )
                    }
                }

                Button(
                    onClick = onRun,
                    modifier = Modifier.testTag("btn_run_${script.id}"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Run", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

