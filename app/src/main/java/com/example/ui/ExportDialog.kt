package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.CadObject

@Composable
fun ExportDialog(
    objects: List<CadObject>,
    defaultFilename: String,
    onExport: (format: String, filename: String, tolerance: Float) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFormat by remember { mutableStateOf("STEP") }
    var filename by remember { mutableStateOf(defaultFilename.removeSuffix(".py")) }
    var selectedQuality by remember { mutableStateOf("Media (0.1 mm)") }

    val formats = listOf(
        FormatOption("STEP", "Estándar CAD ISO-10303-21 (Intercambio BRep exacto)", "step"),
        FormatOption("STL", "Malla triangular estándar para impresión 3D", "stl"),
        FormatOption("OBJ", "Geometría 3D Wavefront con normales", "obj"),
        FormatOption("3MF", "Formato moderno de manufactura aditiva 3D", "3mf")
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xFF161B22),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("export_dialog")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FileDownload, "Exportar", tint = Color(0xFF38BDF8))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Exportar Geometría 3D",
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

                // Filename Input
                Text("Nombre del archivo:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = filename,
                    onValueChange = { filename = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                // Format Picker
                Text("Formato de exportación:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    formats.forEach { fmt ->
                        val isSelected = selectedFormat == fmt.name
                        Surface(
                            color = if (isSelected) Color(0xFF0F2640) else Color(0xFF0F172A),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E293B),
                                    RoundedCornerShape(8.dp)
                                )
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedFormat = fmt.name }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = fmt.name,
                                    color = if (isSelected) Color(0xFF38BDF8) else Color(0xFFE2E8F0),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = fmt.description,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Quality deflection selector
                Text("Tolerancia de triangulación:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Fina (0.05 mm)", "Media (0.1 mm)", "Rápida (0.5 mm)").forEach { qual ->
                        val isSel = selectedQuality == qual
                        Surface(
                            color = if (isSel) Color(0xFF0284C7) else Color(0xFF1E293B),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { selectedQuality = qual }
                        ) {
                            Text(
                                text = qual,
                                color = if (isSel) Color.White else Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Export & Share Button
                Button(
                    onClick = {
                        val tolerance = when (selectedQuality) {
                            "Fina (0.05 mm)" -> 0.05f
                            "Rápida (0.5 mm)" -> 0.5f
                            else -> 0.1f
                        }
                        onExport(selectedFormat, filename, tolerance)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("export_submit_btn")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Exportar y Compartir .$selectedFormat", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private data class FormatOption(
    val name: String,
    val description: String,
    val ext: String
)
