package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.CadScript
import com.example.model.CadTemplate
import com.example.model.CadTemplates
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ScriptExplorerDialog(
    scripts: List<CadScript>,
    currentScriptId: String?,
    onOpenScript: (CadScript) -> Unit,
    onCreateNewScript: (name: String, content: String) -> Unit,
    onRenameScript: (CadScript, String) -> Unit,
    onDuplicateScript: (CadScript) -> Unit,
    onDeleteScript: (CadScript) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var sortByName by remember { mutableStateOf(false) }
    var showTemplatePicker by remember { mutableStateOf(false) }
    var scriptToDelete by remember { mutableStateOf<CadScript?>(null) }
    var scriptToRename by remember { mutableStateOf<CadScript?>(null) }
    var renameInput by remember { mutableStateOf("") }

    val filteredScripts = remember(scripts, searchQuery, sortByName) {
        val list = scripts.filter { it.name.contains(searchQuery, ignoreCase = true) }
        if (sortByName) list.sortedBy { it.name }
        else list.sortedByDescending { it.lastModified }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xFF161B22),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
                .testTag("script_explorer_dialog")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Folder, "Explorador", tint = Color(0xFF38BDF8))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Explorador de Scripts .py",
                            color = Color(0xFFF8FAFC),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Cerrar", tint = Color.Gray)
                    }
                }

                HorizontalDivider(color = Color(0xFF1E293B), modifier = Modifier.padding(vertical = 10.dp))

                // Search & Sort row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar scripts...", color = Color.Gray, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray, modifier = Modifier.size(16.dp)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(6.dp))
                    IconButton(onClick = { sortByName = !sortByName }) {
                        Icon(
                            Icons.Default.SortByAlpha,
                            "Ordenar",
                            tint = if (sortByName) Color(0xFF38BDF8) else Color.Gray
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Scripts List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredScripts) { script ->
                        val isCurrent = script.id == currentScriptId
                        val dateFormatted = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(Date(script.lastModified))

                        Surface(
                            color = if (isCurrent) Color(0xFF0F2B48) else Color(0xFF0F172A),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    if (isCurrent) Color(0xFF38BDF8) else Color(0xFF1E293B),
                                    RoundedCornerShape(8.dp)
                                )
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onOpenScript(script)
                                    onDismiss()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Description, null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = script.name,
                                        color = Color(0xFFF1F5F9),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "$dateFormatted · ${script.content.lines().size} líneas",
                                        color = Color(0xFF64748B),
                                        fontSize = 10.sp
                                    )
                                }

                                // Duplicate
                                IconButton(
                                    onClick = { onDuplicateScript(script) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, "Duplicar", tint = Color(0xFF94A3B8), modifier = Modifier.size(15.dp))
                                }

                                // Rename
                                IconButton(
                                    onClick = {
                                        scriptToRename = script
                                        renameInput = script.name
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Edit, "Renombrar", tint = Color(0xFF94A3B8), modifier = Modifier.size(15.dp))
                                }

                                // Delete
                                IconButton(
                                    onClick = { scriptToDelete = script },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Delete, "Eliminar", tint = Color(0xFFEF4444), modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // New Script button
                Button(
                    onClick = { showTemplatePicker = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("new_script_btn")
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Nuevo Script desde Plantilla", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Template Picker Sub-Dialog
    if (showTemplatePicker) {
        Dialog(onDismissRequest = { showTemplatePicker = false }) {
            Surface(
                color = Color(0xFF161B22),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Seleccionar Plantilla CadQuery",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(10.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(CadTemplates.templates) { tmpl ->
                            Surface(
                                color = Color(0xFF0F172A),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        onCreateNewScript("${tmpl.title.replace(" ", "_").lowercase()}.py", tmpl.code)
                                        showTemplatePicker = false
                                        onDismiss()
                                    }
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(tmpl.title, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(tmpl.description, color = Color(0xFF94A3B8), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Rename confirmation dialog
    if (scriptToRename != null) {
        AlertDialog(
            onDismissRequest = { scriptToRename = null },
            title = { Text("Renombrar script", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            },
            confirmButton = {
                Button(onClick = {
                    val target = scriptToRename ?: return@Button
                    val finalName = if (renameInput.endsWith(".py")) renameInput else "$renameInput.py"
                    onRenameScript(target, finalName)
                    scriptToRename = null
                }) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { scriptToRename = null }) { Text("Cancelar") }
            },
            containerColor = Color(0xFF161B22)
        )
    }

    // Delete confirmation dialog
    if (scriptToDelete != null) {
        AlertDialog(
            onDismissRequest = { scriptToDelete = null },
            title = { Text("¿Eliminar script?", color = Color.White) },
            text = {
                Text(
                    "¿Estás seguro de que deseas eliminar permanentemente '${scriptToDelete?.name}'?",
                    color = Color(0xFF94A3B8)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scriptToDelete?.let { onDeleteScript(it) }
                        scriptToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { scriptToDelete = null }) { Text("Cancelar") }
            },
            containerColor = Color(0xFF161B22)
        )
    }
}
