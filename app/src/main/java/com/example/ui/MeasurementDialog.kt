package com.example.ui

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.CadObject
import com.example.model.MeasurementResult
import com.example.model.MeasurementType

@Composable
fun MeasurementDialog(
    objects: List<CadObject>,
    onDismiss: () -> Unit
) {
    val visibleObjs = objects.filter { it.isVisible }

    val measurements = mutableListOf<MeasurementItem>()

    if (visibleObjs.isNotEmpty()) {
        val totalVolume = visibleObjs.sumOf { it.volume.toDouble() }
        val totalArea = visibleObjs.sumOf { it.surfaceArea.toDouble() }

        measurements.add(
            MeasurementItem(
                title = "Volumen Total Sólido",
                value = "${String.format("%.2f", totalVolume)} mm³",
                details = "Calculado según el teorema de divergencia BRep",
                icon = Icons.Default.ViewInAr
            )
        )

        measurements.add(
            MeasurementItem(
                title = "Área Superficial Total",
                value = "${String.format("%.2f", totalArea)} mm²",
                details = "Suma de áreas de todas las caras de sólidos visibles",
                icon = Icons.Default.CropFree
            )
        )

        // Per-object bounding box and dimension breakdown
        visibleObjs.forEach { obj ->
            val bb = obj.boundingBox
            measurements.add(
                MeasurementItem(
                    title = "Caja Envolvente: ${obj.name}",
                    value = "${String.format("%.2f", bb.sizeX)} × ${String.format("%.2f", bb.sizeY)} × ${String.format("%.2f", bb.sizeZ)} mm",
                    details = "Diagonal: ${String.format("%.2f", bb.diagonal)} mm | Centro: (${String.format("%.1f", bb.centerX)}, ${String.format("%.1f", bb.centerY)}, ${String.format("%.1f", bb.centerZ)})",
                    icon = Icons.Default.Straighten
                )
            )

            // Features on faces (e.g. holes, radii)
            obj.faces.forEach { face ->
                if (face.diameter != null) {
                    measurements.add(
                        MeasurementItem(
                            title = "Diámetro Cilíndrico (${obj.name})",
                            value = "Ø ${String.format("%.2f", face.diameter)} mm (R: ${String.format("%.2f", face.diameter / 2f)} mm)",
                            details = "Superficie cilíndrica de revolución",
                            icon = Icons.Default.RadioButtonUnchecked
                        )
                    )
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xFF161B22),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("measurement_dialog")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SquareFoot, "Medición CAD", tint = Color(0xFFF59E0B))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Metrología e Inspección CAD",
                            color = Color(0xFFF8FAFC),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Cerrar", tint = Color.Gray)
                    }
                }

                HorizontalDivider(color = Color(0xFF1E293B), modifier = Modifier.padding(vertical = 10.dp))

                if (measurements.isEmpty()) {
                    Text(
                        text = "No hay piezas visibles para medir. Ejecuta un script o activa piezas en la lista de objetos.",
                        color = Color(0xFF64748B),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(measurements) { item ->
                            Surface(
                                color = Color(0xFF0F172A),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(item.icon, item.title, tint = Color(0xFF38BDF8), modifier = Modifier.size(24.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.title, color = Color(0xFF94A3B8), fontSize = 11.sp)
                                        Text(
                                            item.value,
                                            color = Color(0xFF38BDF8),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(item.details, color = Color(0xFF64748B), fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class MeasurementItem(
    val title: String,
    val value: String,
    val details: String,
    val icon: ImageVector
)
