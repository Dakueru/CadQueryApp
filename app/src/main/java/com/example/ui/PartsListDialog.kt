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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.CadObject

@Composable
fun PartsListDialog(
    objects: List<CadObject>,
    onToggleVisibility: (String) -> Unit,
    onColorChange: (String, Color) -> Unit,
    onAlphaChange: (String, Float) -> Unit,
    onSelectObject: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = listOf(
        Color(0xFF38BDF8),
        Color(0xFFF59E0B),
        Color(0xFF10B981),
        Color(0xFFA855F7),
        Color(0xFFEC4899),
        Color(0xFFEF4444),
        Color(0xFFE2E8F0)
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xFF161B22),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("parts_management_dialog")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Gestión de Piezas 3D",
                            color = Color(0xFFF8FAFC),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${objects.size} objetos generados por CadQuery",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Cerrar", tint = Color.Gray)
                    }
                }

                HorizontalDivider(color = Color(0xFF1E293B), modifier = Modifier.padding(vertical = 10.dp))

                if (objects.isEmpty()) {
                    Text(
                        text = "No hay objetos generados en esta ejecución.",
                        color = Color(0xFF64748B),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(objects) { obj ->
                            Surface(
                                color = Color(0xFF0F172A),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, if (obj.isVisible) Color(0xFF334155) else Color(0xFF1E293B), RoundedCornerShape(10.dp))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    // Row 1: Checkbox + Name + Center Focus button
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Checkbox(
                                            checked = obj.isVisible,
                                            onCheckedChange = { onToggleVisibility(obj.id) },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = Color(0xFF38BDF8),
                                                uncheckedColor = Color(0xFF64748B)
                                            )
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = obj.name,
                                                color = if (obj.isVisible) Color(0xFFF1F5F9) else Color(0xFF64748B),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "${obj.triangles.size} triángulos | ${String.format("%.1f", obj.volume)} mm³",
                                                color = Color(0xFF94A3B8),
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }

                                        IconButton(
                                            onClick = { onSelectObject(obj.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CenterFocusStrong,
                                                contentDescription = "Centrar pieza",
                                                tint = Color(0xFF38BDF8),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    // Row 2: Bounding Box Dimensions
                                    Text(
                                        text = "Dim: ${String.format("%.1f", obj.boundingBox.sizeX)} × ${String.format("%.1f", obj.boundingBox.sizeY)} × ${String.format("%.1f", obj.boundingBox.sizeZ)} mm",
                                        color = Color(0xFF64748B),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(start = 48.dp, bottom = 6.dp)
                                    )

                                    // Row 3: Color Palette Picker
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 48.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        palette.forEach { col ->
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(col)
                                                    .border(
                                                        width = if (obj.color == col) 2.dp else 0.dp,
                                                        color = Color.White,
                                                        shape = CircleShape
                                                    )
                                                    .clickable { onColorChange(obj.id, col) }
                                            )
                                        }
                                    }

                                    // Row 4: Transparency Slider
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 48.dp, top = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Opacidad: ", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                        Slider(
                                            value = obj.alpha,
                                            onValueChange = { onAlphaChange(obj.id, it) },
                                            valueRange = 0.1f..1.0f,
                                            colors = SliderDefaults.colors(
                                                thumbColor = Color(0xFF38BDF8),
                                                activeTrackColor = Color(0xFF38BDF8)
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "${(obj.alpha * 100).toInt()}%",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp,
                                            modifier = Modifier.width(36.dp)
                                        )
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
