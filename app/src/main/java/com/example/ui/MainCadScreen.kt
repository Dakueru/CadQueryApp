package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerticalSplit
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.editor.CadCodeEditor
import com.example.export.CadExporter
import com.example.model.CadScript
import com.example.model.EngineType
import com.example.viewer.Cad3DViewer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainCadScreen(viewModel: CadViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Dialog Visibility States
    var showExplorer by remember { mutableStateOf(false) }
    var showPartsList by remember { mutableStateOf(false) }
    var showMeasurement by remember { mutableStateOf(false) }
    var showParameters by remember { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var showEngineSettings by remember { mutableStateOf(false) }
    var showVersionHistory by remember { mutableStateOf(false) }
    var pendingCloseScript by remember { mutableStateOf<CadScript?>(null) }

    fun copyAllCode() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Script CadQuery", uiState.currentScript.content)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Código copiado al portapapeles", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF0A0E17),
        topBar = {
            Surface(
                color = Color(0xFF161B22),
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Brand and Script Name
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                color = Color(0xFF0284C7),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("CQ", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "${uiState.currentScript.name}${if (uiState.isDirty) " •" else ""}",
                                    color = if (uiState.isDirty) Color(0xFFFBBF24) else Color(0xFFF1F5F9),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                // Clickable Engine Badge
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable { showEngineSettings = true }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (uiState.engineType == EngineType.REMOTE_CADQUERY) Color(0xFF38BDF8) else Color(0xFF10B981))
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = uiState.engineType.badge,
                                        color = if (uiState.engineType == EngineType.REMOTE_CADQUERY) Color(0xFF38BDF8) else Color(0xFF10B981),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (uiState.executionTimeMs > 0) {
                                        Text(
                                            text = " · ${uiState.executionTimeMs}ms",
                                            color = Color(0xFF64748B),
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }

                        // Top Primary Action Buttons
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Run (▶) or Stop (■) button
                            if (uiState.isExecuting) {
                                IconButton(
                                    onClick = { viewModel.stopExecution() },
                                    modifier = Modifier.size(36.dp).testTag("stop_execution_btn")
                                ) {
                                    Icon(Icons.Default.Stop, "Detener ejecución", tint = Color(0xFFEF4444))
                                }
                            } else {
                                Surface(
                                    color = Color(0xFF0284C7),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { viewModel.executeCurrentScript() }
                                        .testTag("run_script_btn")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.PlayArrow, "Ejecutar", tint = Color.White, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(2.dp))
                                        Text("Ejecutar", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(Modifier.width(4.dp))

                            // Save button
                            IconButton(
                                onClick = {
                                    viewModel.saveCurrentScript()
                                    Toast.makeText(context, "Script guardado", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(34.dp).testTag("save_script_btn")
                            ) {
                                Icon(Icons.Default.Save, "Guardar", tint = if (uiState.isDirty) Color(0xFFFBBF24) else Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                            }

                            // Copy all button
                            IconButton(
                                onClick = { copyAllCode() },
                                modifier = Modifier.size(34.dp).testTag("copy_code_btn")
                            ) {
                                Icon(Icons.Default.ContentCopy, "Copiar todo", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                            }

                            // Export button
                            IconButton(
                                onClick = { showExport = true },
                                modifier = Modifier.size(34.dp).testTag("export_menu_btn")
                            ) {
                                Icon(Icons.Default.FileDownload, "Exportar 3D", tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                            }

                            // View mode toggle (Split / Editor / Viewer)
                            IconButton(
                                onClick = {
                                    val nextMode = when (uiState.layoutMode) {
                                        DisplayLayoutMode.SPLIT_BOTH -> DisplayLayoutMode.EDITOR_ONLY
                                        DisplayLayoutMode.EDITOR_ONLY -> DisplayLayoutMode.VIEWER_ONLY
                                        DisplayLayoutMode.VIEWER_ONLY -> DisplayLayoutMode.SPLIT_BOTH
                                    }
                                    viewModel.setLayoutMode(nextMode)
                                },
                                modifier = Modifier.size(34.dp).testTag("view_mode_toggle_btn")
                            ) {
                                Icon(
                                    imageVector = when (uiState.layoutMode) {
                                        DisplayLayoutMode.SPLIT_BOTH -> Icons.Default.VerticalSplit
                                        DisplayLayoutMode.EDITOR_ONLY -> Icons.Default.DataObject
                                        DisplayLayoutMode.VIEWER_ONLY -> Icons.Default.ViewInAr
                                    },
                                    contentDescription = "Modo de visualización",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Console toggle
                            IconButton(
                                onClick = { viewModel.toggleConsole() },
                                modifier = Modifier.size(34.dp).testTag("toggle_console_btn")
                            ) {
                                Icon(
                                    Icons.Default.Terminal,
                                    "Consola",
                                    tint = if (uiState.errorList.isNotEmpty()) Color(0xFFEF4444) else if (uiState.isConsoleVisible) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Script Explorer
                            IconButton(
                                onClick = { showExplorer = true },
                                modifier = Modifier.size(34.dp).testTag("explorer_btn")
                            ) {
                                Icon(Icons.Default.Folder, "Explorador", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                            }

                            // Parameters Panel
                            IconButton(
                                onClick = { showParameters = true },
                                modifier = Modifier.size(34.dp).testTag("parameters_btn")
                            ) {
                                Icon(Icons.Default.Tune, "Parámetros", tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                            }

                            // Version History
                            IconButton(
                                onClick = { showVersionHistory = true },
                                modifier = Modifier.size(34.dp).testTag("history_btn")
                            ) {
                                Icon(Icons.Default.History, "Historial", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                            }

                            // Help Dialog
                            IconButton(
                                onClick = { showHelp = true },
                                modifier = Modifier.size(34.dp).testTag("help_btn")
                            ) {
                                Icon(Icons.Default.HelpOutline, "Ayuda", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                            }

                            // Engine Settings
                            IconButton(
                                onClick = { showEngineSettings = true },
                                modifier = Modifier.size(34.dp).testTag("settings_btn")
                            ) {
                                Icon(Icons.Default.Settings, "Configuración", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Responsive Split or Single Pane Layout
            if (isLandscape) {
                // Landscape: Side by Side
                Row(modifier = Modifier.fillMaxSize()) {
                    if (uiState.layoutMode != DisplayLayoutMode.VIEWER_ONLY) {
                        Box(
                            modifier = Modifier
                                .weight(if (uiState.layoutMode == DisplayLayoutMode.SPLIT_BOTH) 0.5f else 1.0f)
                                .fillMaxHeight()
                        ) {
                            CadCodeEditor(
                                code = uiState.currentScript.content,
                                onCodeChange = { viewModel.onCodeChange(it) },
                                highlightLine = uiState.highlightErrorLine
                            )
                        }
                    }

                    if (uiState.layoutMode != DisplayLayoutMode.EDITOR_ONLY) {
                        Box(
                            modifier = Modifier
                                .weight(if (uiState.layoutMode == DisplayLayoutMode.SPLIT_BOTH) 0.5f else 1.0f)
                                .fillMaxHeight()
                        ) {
                            Cad3DViewer(
                                objects = uiState.objects,
                                onOpenPartsList = { showPartsList = true },
                                onOpenMeasurement = { showMeasurement = true },
                                isStaleGeometry = uiState.isStaleGeometry,
                                selectedObjectId = uiState.selectedObjectId
                            )
                        }
                    }
                }
            } else {
                // Portrait: Top and Bottom
                Column(modifier = Modifier.fillMaxSize()) {
                    if (uiState.layoutMode != DisplayLayoutMode.VIEWER_ONLY) {
                        Box(
                            modifier = Modifier
                                .weight(if (uiState.layoutMode == DisplayLayoutMode.SPLIT_BOTH) 0.55f else 1.0f)
                                .fillMaxWidth()
                        ) {
                            CadCodeEditor(
                                code = uiState.currentScript.content,
                                onCodeChange = { viewModel.onCodeChange(it) },
                                highlightLine = uiState.highlightErrorLine
                            )
                        }
                    }

                    if (uiState.layoutMode != DisplayLayoutMode.EDITOR_ONLY) {
                        Box(
                            modifier = Modifier
                                .weight(if (uiState.layoutMode == DisplayLayoutMode.SPLIT_BOTH) 0.45f else 1.0f)
                                .fillMaxWidth()
                        ) {
                            Cad3DViewer(
                                objects = uiState.objects,
                                onOpenPartsList = { showPartsList = true },
                                onOpenMeasurement = { showMeasurement = true },
                                isStaleGeometry = uiState.isStaleGeometry,
                                selectedObjectId = uiState.selectedObjectId
                            )
                        }
                    }
                }
            }

            // Bottom Console Panel (Animated overlay)
            AnimatedVisibility(
                visible = uiState.isConsoleVisible,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                ConsolePanel(
                    stdout = uiState.stdout,
                    errors = uiState.errorList,
                    executionTimeMs = uiState.executionTimeMs,
                    onNavigateToLine = { viewModel.navigateToLine(it) },
                    onClearConsole = { viewModel.clearConsole() },
                    onCloseConsole = { viewModel.toggleConsole() }
                )
            }
        }
    }

    // --- Sub-Dialogs ---

    // 1. Script Explorer Dialog
    if (showExplorer) {
        ScriptExplorerDialog(
            scripts = uiState.allScripts,
            currentScriptId = uiState.currentScript.id,
            onOpenScript = { script ->
                if (uiState.isDirty) {
                    pendingCloseScript = script
                } else {
                    viewModel.openScript(script)
                }
            },
            onCreateNewScript = { name, content -> viewModel.createNewScript(name, content) },
            onRenameScript = { script, newName -> viewModel.renameScript(script, newName) },
            onDuplicateScript = { script -> viewModel.duplicateScript(script) },
            onDeleteScript = { script -> viewModel.deleteScript(script) },
            onDismiss = { showExplorer = false }
        )
    }

    // 2. Parts Management Dialog
    if (showPartsList) {
        PartsListDialog(
            objects = uiState.objects,
            onToggleVisibility = { viewModel.toggleObjectVisibility(it) },
            onColorChange = { id, color -> viewModel.setObjectColor(id, color) },
            onAlphaChange = { id, alpha -> viewModel.setObjectAlpha(id, alpha) },
            onSelectObject = { viewModel.selectObject(it) },
            onDismiss = { showPartsList = false }
        )
    }

    // 3. Measurement Dialog
    if (showMeasurement) {
        MeasurementDialog(
            objects = uiState.objects,
            onDismiss = { showMeasurement = false }
        )
    }

    // 4. Parameters Dialog
    if (showParameters) {
        ParametersDialog(
            parameters = uiState.detectedParameters,
            onApplyParameters = { viewModel.applyParameters(it) },
            onDismiss = { showParameters = false }
        )
    }

    // 5. Export Dialog
    if (showExport) {
        ExportDialog(
            objects = uiState.objects,
            defaultFilename = uiState.currentScript.name,
            onExport = { format, filename, tolerance ->
                val shareIntent = CadExporter.saveAndCreateShareIntent(context, filename, format, uiState.objects)
                context.startActivity(shareIntent)
            },
            onDismiss = { showExport = false }
        )
    }

    // 6. CadQuery Help Dialog
    if (showHelp) {
        CadHelpDialog(
            onInsertSnippet = { snippet ->
                viewModel.onCodeChange(uiState.currentScript.content + snippet)
            },
            onDismiss = { showHelp = false }
        )
    }

    // 7. Engine Settings Dialog
    if (showEngineSettings) {
        EngineSettingsDialog(
            currentEngineType = uiState.engineType,
            serverUrl = uiState.serverUrl,
            onEngineChange = { type, url -> viewModel.setEngineConfig(type, url) },
            onDismiss = { showEngineSettings = false }
        )
    }

    // 8. Version History Dialog
    if (showVersionHistory) {
        VersionHistoryDialog(
            snapshots = uiState.snapshots,
            onRestoreSnapshot = { viewModel.restoreSnapshot(it) },
            onDismiss = { showVersionHistory = false }
        )
    }

    // 9. Unsaved Changes Alert Dialog (Section 10)
    if (pendingCloseScript != null) {
        AlertDialog(
            onDismissRequest = { pendingCloseScript = null },
            title = {
                Text("¿Quieres guardar los cambios?", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "El documento contiene modificaciones que todavía no se han guardado.",
                    color = Color(0xFFCBD5E1)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveCurrentScript()
                        pendingCloseScript?.let { viewModel.openScript(it) }
                        pendingCloseScript = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            pendingCloseScript?.let { viewModel.openScript(it) }
                            pendingCloseScript = null
                        }
                    ) {
                        Text("Descartar", color = Color(0xFFEF4444))
                    }
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = { pendingCloseScript = null }) {
                        Text("Cancelar", color = Color(0xFF94A3B8))
                    }
                }
            },
            containerColor = Color(0xFF161B22)
        )
    }
}
