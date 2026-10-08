package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CadError

@Composable
fun ConsolePanel(
    modifier: Modifier = Modifier,
    stdout: String,
    errors: List<CadError>,
    executionTimeMs: Long,
    onNavigateToLine: (Int) -> Unit,
    onClearConsole: () -> Unit,
    onCloseConsole: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    fun copyToClipboard(text: String, label: String = "CadQuery Log") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }

    Surface(
        color = Color(0xFF090D14),
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        tonalElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 260.dp)
            .testTag("cad_console_panel")
    ) {
        Column {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF161B22))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (errors.isNotEmpty()) Icons.Default.ErrorOutline else Icons.Default.Info,
                        contentDescription = "Estado",
                        tint = if (errors.isNotEmpty()) Color(0xFFEF4444) else Color(0xFF38BDF8),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Consola y Diagnóstico",
                        color = Color(0xFFE2E8F0),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "⏱ ${executionTimeMs}ms",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            val fullLog = buildString {
                                appendLine("--- STDOUT ---")
                                appendLine(stdout)
                                if (errors.isNotEmpty()) {
                                    appendLine("--- ERRORES ---")
                                    errors.forEach { err ->
                                        appendLine("Línea ${err.lineNumber ?: "?"}: ${err.message}")
                                        appendLine(err.rawTraceback)
                                    }
                                }
                            }
                            copyToClipboard(fullLog)
                        },
                        modifier = Modifier.size(28.dp).testTag("console_copy_btn")
                    ) {
                        Icon(Icons.Default.ContentCopy, "Copiar registro", tint = Color(0xFF94A3B8), modifier = Modifier.size(15.dp))
                    }

                    IconButton(
                        onClick = onClearConsole,
                        modifier = Modifier.size(28.dp).testTag("console_clear_btn")
                    ) {
                        Icon(Icons.Default.DeleteSweep, "Limpiar", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                    }

                    IconButton(
                        onClick = onCloseConsole,
                        modifier = Modifier.size(28.dp).testTag("console_close_btn")
                    ) {
                        Icon(Icons.Default.Close, "Cerrar consola", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                    }
                }
            }

            HorizontalDivider(color = Color(0xFF1E293B))

            // Body logs
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .verticalScroll(scrollState)
            ) {
                // Render Errors
                if (errors.isNotEmpty()) {
                    errors.forEach { err ->
                        Surface(
                            color = Color(0x33EF4444),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    err.lineNumber?.let { onNavigateToLine(it) }
                                }
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (err.lineNumber != null) "ERROR EN LA LÍNEA ${err.lineNumber}" else "ERROR DE EJECUCIÓN",
                                        color = Color(0xFFEF4444),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(Modifier.weight(1f))
                                    if (err.lineNumber != null) {
                                        Text(
                                            text = "Tocar para ir a línea ➔",
                                            color = Color(0xFF38BDF8),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = err.message,
                                    color = Color(0xFFFCA5A5),
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                if (err.rawTraceback.isNotEmpty()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = err.rawTraceback.take(400),
                                        color = Color(0xFF94A3B8),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }

                // Render stdout
                if (stdout.isNotEmpty()) {
                    Text(
                        text = stdout,
                        color = Color(0xFF86EFAC),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(4.dp)
                    )
                }

                if (errors.isEmpty() && stdout.isEmpty()) {
                    Text(
                        text = "Sin salidas de consola. Pulsa ▶ para ejecutar el script CadQuery.",
                        color = Color(0xFF475569),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}
