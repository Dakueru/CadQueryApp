package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.engine.CadQueryArchitectureDoc
import com.example.engine.CadQueryRemoteClient
import com.example.model.EngineType
import kotlinx.coroutines.launch

@Composable
fun EngineSettingsDialog(
    currentEngineType: EngineType,
    serverUrl: String,
    onEngineChange: (EngineType, String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedType by remember { mutableStateOf(currentEngineType) }
    var inputUrl by remember { mutableStateOf(serverUrl) }
    var pingStatus by remember { mutableStateOf<String?>(null) }
    var pingSuccess by remember { mutableStateOf(false) }
    var isPinging by remember { mutableStateOf(false) }
    var showArchitectureReport by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    fun copyScriptToClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("cadquery_server.py", CadQueryArchitectureDoc.PYTHON_SERVER_SCRIPT)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "cadquery_server.py copiado al portapapeles", Toast.LENGTH_SHORT).show()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xFF161B22),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
                .testTag("engine_settings_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(scrollState)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, "Ajustes", tint = Color(0xFF38BDF8))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Motor de Ejecución CadQuery",
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

                // Engine Selector Options
                Text("Seleccionar motor activo:", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))

                // Option 1: Remote Real CadQuery (OCP)
                Surface(
                    color = if (selectedType == EngineType.REMOTE_CADQUERY) Color(0xFF0F2B48) else Color(0xFF0F172A),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (selectedType == EngineType.REMOTE_CADQUERY) Color(0xFF38BDF8) else Color(0xFF1E293B),
                            RoundedCornerShape(10.dp)
                        )
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { selectedType = EngineType.REMOTE_CADQUERY }
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        RadioButton(
                            selected = selectedType == EngineType.REMOTE_CADQUERY,
                            onClick = { selectedType = EngineType.REMOTE_CADQUERY },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF38BDF8))
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lan, null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Servidor CadQuery (OCP Real)",
                                    color = Color(0xFFF1F5F9),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Text(
                                text = "Ejecuta CadQuery y OpenCASCADE C++ reales en servidor local (Termux, PC o nube).",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Option 2: Standalone Mobile Engine (CadQuery Core)
                Surface(
                    color = if (selectedType == EngineType.STANDALONE_OFFLINE) Color(0xFF0F2B48) else Color(0xFF0F172A),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (selectedType == EngineType.STANDALONE_OFFLINE) Color(0xFF38BDF8) else Color(0xFF1E293B),
                            RoundedCornerShape(10.dp)
                        )
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { selectedType = EngineType.STANDALONE_OFFLINE }
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        RadioButton(
                            selected = selectedType == EngineType.STANDALONE_OFFLINE,
                            onClick = { selectedType = EngineType.STANDALONE_OFFLINE },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF38BDF8))
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PhoneAndroid, null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Motor Autónomo Móvil (Offline)",
                                    color = Color(0xFFF1F5F9),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Text(
                                text = "Evaluador geométrico local integrado para modelar y probar sin red ni servidor externo.",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Server Configuration fields (if remote selected)
                if (selectedType == EngineType.REMOTE_CADQUERY) {
                    Text("URL del Servidor CadQuery:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        singleLine = true,
                        placeholder = { Text("http://10.0.2.2:5000", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                isPinging = true
                                pingStatus = "Conectando a $inputUrl/health..."
                                scope.launch {
                                    val client = CadQueryRemoteClient(inputUrl)
                                    val (success, message) = client.checkHealth()
                                    pingSuccess = success
                                    pingStatus = message
                                    isPinging = false
                                }
                            },
                            enabled = !isPinging,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8))
                        ) {
                            Icon(Icons.Default.NetworkCheck, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isPinging) "Probando..." else "Probar Conexión", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { copyScriptToClipboard() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Copiar server.py", color = Color(0xFF38BDF8), fontSize = 12.sp)
                        }
                    }

                    // Ping status message
                    if (pingStatus != null) {
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            color = if (pingSuccess) Color(0x2210B981) else Color(0x22EF4444),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (pingSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (pingSuccess) Color(0xFF10B981) else Color(0xFFEF4444),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = pingStatus ?: "",
                                    color = if (pingSuccess) Color(0xFF86EFAC) else Color(0xFFFCA5A5),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Documentation Accordion
                OutlinedButton(
                    onClick = { showArchitectureReport = !showArchitectureReport },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Info, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (showArchitectureReport) "Ocultar Análisis OpenCASCADE/Android" else "Ver Viabilidad OpenCASCADE en Android", fontSize = 12.sp)
                }

                if (showArchitectureReport) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        color = Color(0xFF090D14),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = CadQueryArchitectureDoc.REPORT_SUMMARY,
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Save button
                Button(
                    onClick = {
                        onEngineChange(selectedType, inputUrl)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Guardar y Aplicar Configuración", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
