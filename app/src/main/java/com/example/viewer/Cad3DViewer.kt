package com.example.viewer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CadBoundingBox
import com.example.model.CadObject
import com.example.model.CadPoint3D
import com.example.model.CadTriangle
import com.example.model.PredefinedView
import com.example.model.ViewerShadingMode
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun Cad3DViewer(
    modifier: Modifier = Modifier,
    objects: List<CadObject>,
    onOpenPartsList: () -> Unit,
    onOpenMeasurement: () -> Unit,
    isStaleGeometry: Boolean = false,
    selectedObjectId: String? = null
) {
    // Camera Spherical Coordinates
    var azimuth by remember { mutableFloatStateOf(45f) } // horizontal angle (degrees)
    var elevation by remember { mutableFloatStateOf(30f) } // vertical angle (degrees)
    var zoomScale by remember { mutableFloatStateOf(3.2f) } // zoom scale factor
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    // Display Options
    var showAxes by remember { mutableStateOf(true) }
    var showGrid by remember { mutableStateOf(true) }
    var shadingMode by remember { mutableStateOf(ViewerShadingMode.SOLID_WITH_EDGES) }
    var globalAlpha by remember { mutableFloatStateOf(1.0f) }
    var backgroundColor by remember { mutableStateOf(Color(0xFF0F172A)) }

    // Combined Bounding Box of all visible objects
    val combinedBbox = remember(objects) {
        val visibleObjs = objects.filter { it.isVisible }
        if (visibleObjs.isEmpty()) CadBoundingBox.EMPTY
        else {
            val allTriangles = visibleObjs.flatMap { it.triangles }
            CadObject.computeBoundingBox(allTriangles)
        }
    }

    fun setPredefinedView(view: PredefinedView) {
        when (view) {
            PredefinedView.ISOMETRIC -> { azimuth = 45f; elevation = 30f }
            PredefinedView.TOP -> { azimuth = 0f; elevation = 89.9f }
            PredefinedView.BOTTOM -> { azimuth = 0f; elevation = -89.9f }
            PredefinedView.FRONT -> { azimuth = 0f; elevation = 0f }
            PredefinedView.BACK -> { azimuth = 180f; elevation = 0f }
            PredefinedView.LEFT -> { azimuth = -90f; elevation = 0f }
            PredefinedView.RIGHT -> { azimuth = 90f; elevation = 0f }
        }
    }

    fun fitToScreen() {
        panOffsetX = 0f
        panOffsetY = 0f
        val maxDim = combinedBbox.maxDimension
        zoomScale = if (maxDim > 0.01f) (300f / maxDim).coerceIn(0.5f, 25f) else 3.5f
    }

    fun resetCamera() {
        azimuth = 45f
        elevation = 30f
        panOffsetX = 0f
        panOffsetY = 0f
        fitToScreen()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .testTag("cad_3d_viewport")
    ) {
        // --- 1. Interactive 3D Canvas ---
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, rotation ->
                        if (zoom != 1f) {
                            // Pinch to zoom
                            zoomScale = (zoomScale * zoom).coerceIn(0.2f, 50f)
                        }
                        if (pan != Offset.Zero) {
                            // Single finger rotates orbit; multi-touch pans
                            azimuth = (azimuth + pan.x * 0.5f) % 360f
                            elevation = (elevation - pan.y * 0.5f).coerceIn(-89.9f, 89.9f)
                        }
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f + panOffsetX
            val centerY = height / 2f + panOffsetY

            // Camera Transformation Matrix Elements
            val azRad = (azimuth * PI / 180.0).toFloat()
            val elRad = (elevation * PI / 180.0).toFloat()

            val cosAz = cos(azRad)
            val sinAz = sin(azRad)
            val cosEl = cos(elRad)
            val sinEl = sin(elRad)

            // Center target offset from model center
            val modelCenter = combinedBbox.center

            fun projectPoint(p: CadPoint3D): Triple<Float, Float, Float> {
                // Translate to model center
                val tx = p.x - modelCenter.x
                val ty = p.y - modelCenter.y
                val tz = p.z - modelCenter.z

                // Rotate by Azimuth around Z
                val x1 = tx * cosAz - ty * sinAz
                val y1 = tx * sinAz + ty * cosAz
                val z1 = tz

                // Rotate by Elevation around X
                val x2 = x1
                val y2 = y1 * cosEl - z1 * sinEl
                val z2 = y1 * sinEl + z1 * cosEl

                // Screen coordinates (orthographic with depth sorting)
                val screenX = centerX + x2 * zoomScale
                val screenY = centerY - z2 * zoomScale
                val depth = y2

                return Triple(screenX, screenY, depth)
            }

            // Directional Light vector in camera space
            val lightDir = CadPoint3D(0.4f, 0.5f, 0.77f).normalized()

            // --- A. Draw Ground Grid at Z=0 ---
            if (showGrid) {
                val gridRange = 80f
                val step = 10f
                var g = -gridRange
                while (g <= gridRange) {
                    val p1 = projectPoint(CadPoint3D(g, -gridRange, 0f))
                    val p2 = projectPoint(CadPoint3D(g, gridRange, 0f))
                    drawLine(
                        color = Color(0x2238BDF8),
                        start = Offset(p1.first, p1.second),
                        end = Offset(p2.first, p2.second),
                        strokeWidth = if (g == 0f) 1.5f else 0.8f
                    )

                    val p3 = projectPoint(CadPoint3D(-gridRange, g, 0f))
                    val p4 = projectPoint(CadPoint3D(gridRange, g, 0f))
                    drawLine(
                        color = Color(0x2238BDF8),
                        start = Offset(p3.first, p3.second),
                        end = Offset(p4.first, p4.second),
                        strokeWidth = if (g == 0f) 1.5f else 0.8f
                    )
                    g += step
                }
            }

            // --- B. Draw Coordinate Axes in 3D Space ---
            // MANDATORY: Starts strictly at (0,0,0) and extends positive only with arrows and labels!
            if (showAxes) {
                val axisLength = max(40f, combinedBbox.maxDimension * 0.7f)
                val origin = projectPoint(CadPoint3D.ZERO)

                // X Axis - Red (#EF4444)
                val endX = projectPoint(CadPoint3D(axisLength, 0f, 0f))
                drawLine(
                    color = Color(0xFFEF4444),
                    start = Offset(origin.first, origin.second),
                    end = Offset(endX.first, endX.second),
                    strokeWidth = 3f
                )
                drawArrowHead(Offset(origin.first, origin.second), Offset(endX.first, endX.second), Color(0xFFEF4444))

                // Y Axis - Green (#22C55E)
                val endY = projectPoint(CadPoint3D(0f, axisLength, 0f))
                drawLine(
                    color = Color(0xFF22C55E),
                    start = Offset(origin.first, origin.second),
                    end = Offset(endY.first, endY.second),
                    strokeWidth = 3f
                )
                drawArrowHead(Offset(origin.first, origin.second), Offset(endY.first, endY.second), Color(0xFF22C55E))

                // Z Axis - Blue (#3B82F6)
                val endZ = projectPoint(CadPoint3D(0f, 0f, axisLength))
                drawLine(
                    color = Color(0xFF3B82F6),
                    start = Offset(origin.first, origin.second),
                    end = Offset(endZ.first, endZ.second),
                    strokeWidth = 3f
                )
                drawArrowHead(Offset(origin.first, origin.second), Offset(endZ.first, endZ.second), Color(0xFF3B82F6))

                // Native Android text labels for X, Y, Z
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        textSize = 28f
                        isFakeBoldText = true
                    }
                    paint.color = android.graphics.Color.parseColor("#EF4444")
                    drawText("X", endX.first + 6f, endX.second, paint)

                    paint.color = android.graphics.Color.parseColor("#22C55E")
                    drawText("Y", endY.first + 6f, endY.second, paint)

                    paint.color = android.graphics.Color.parseColor("#3B82F6")
                    drawText("Z", endZ.first + 6f, endZ.second, paint)
                }
            }

            // --- C. Collect & Sort Triangles by Depth (Painter's Algorithm) ---
            data class ProjectedTriangle(
                val sp1: Offset,
                val sp2: Offset,
                val sp3: Offset,
                val depth: Float,
                val normal: CadPoint3D,
                val color: Color,
                val alpha: Float,
                val isSelected: Boolean
            )

            val projectedList = mutableListOf<ProjectedTriangle>()

            for (obj in objects.filter { it.isVisible }) {
                val isSelected = obj.id == selectedObjectId
                val baseColor = if (isSelected) Color(0xFFFACC15) else obj.color
                val objAlpha = obj.alpha * globalAlpha

                for (t in obj.triangles) {
                    val p1 = projectPoint(t.v1)
                    val p2 = projectPoint(t.v2)
                    val p3 = projectPoint(t.v3)
                    val avgDepth = (p1.third + p2.third + p3.third) / 3f

                    projectedList.add(
                        ProjectedTriangle(
                            sp1 = Offset(p1.first, p1.second),
                            sp2 = Offset(p2.first, p2.second),
                            sp3 = Offset(p3.first, p3.second),
                            depth = avgDepth,
                            normal = t.normal,
                            color = baseColor,
                            alpha = objAlpha,
                            isSelected = isSelected
                        )
                    )
                }
            }

            // Sort from back to front
            projectedList.sortBy { it.depth }

            // Render Solid Shaded Triangles
            if (shadingMode != ViewerShadingMode.WIREFRAME_ONLY) {
                for (pt in projectedList) {
                    // Shading calculation: ambient + diffuse directional light
                    val normal = pt.normal
                    // Rotate normal to camera space
                    val nx1 = normal.x * cosAz - normal.y * sinAz
                    val ny1 = normal.x * sinAz + normal.y * cosAz
                    val nz1 = normal.z

                    val nx2 = nx1
                    val ny2 = ny1 * cosEl - nz1 * sinEl
                    val nz2 = ny1 * sinEl + nz1 * cosEl

                    val dot = max(0f, nx2 * lightDir.x + ny2 * lightDir.y + nz2 * lightDir.z)
                    val intensity = (0.35f + 0.65f * dot).coerceIn(0.2f, 1.0f)

                    val shadedColor = Color(
                        red = pt.color.red * intensity,
                        green = pt.color.green * intensity,
                        blue = pt.color.blue * intensity,
                        alpha = pt.alpha
                    )

                    val path = Path().apply {
                        moveTo(pt.sp1.x, pt.sp1.y)
                        lineTo(pt.sp2.x, pt.sp2.y)
                        lineTo(pt.sp3.x, pt.sp3.y)
                        close()
                    }
                    drawPath(path = path, color = shadedColor)
                }
            }

            // Render Sharp Feature Edges & Wireframe
            if (shadingMode == ViewerShadingMode.SOLID_WITH_EDGES || shadingMode == ViewerShadingMode.WIREFRAME_ONLY) {
                for (obj in objects.filter { it.isVisible }) {
                    val edgeColor = if (shadingMode == ViewerShadingMode.WIREFRAME_ONLY) {
                        obj.color
                    } else {
                        Color(0xFF0F172A).copy(alpha = 0.85f)
                    }

                    for (edge in obj.featureEdges) {
                        val p1 = projectPoint(edge.start)
                        val p2 = projectPoint(edge.end)
                        drawLine(
                            color = edgeColor,
                            start = Offset(p1.first, p1.second),
                            end = Offset(p2.first, p2.second),
                            strokeWidth = if (shadingMode == ViewerShadingMode.WIREFRAME_ONLY) 1.5f else 1.2f
                        )
                    }
                }
            }
        }

        // --- 2. Top-Left: Model Info & Stale Execution Notice ---
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
        ) {
            if (isStaleGeometry) {
                Surface(
                    color = Color(0xDDF59E0B),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Text(
                        text = "⚠ Mostrando geometría de la última ejecución válida",
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Quick Stats Pill
            Surface(
                color = Color(0xCC1E293B),
                shape = RoundedCornerShape(8.dp),
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${objects.count { it.isVisible }} piezas | ${objects.filter { it.isVisible }.sumOf { it.triangles.size }} triángulos",
                        color = Color(0xFFE2E8F0),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // --- 3. Top-Right: Orientation Indicator Widget (Compass Tripod) ---
        Surface(
            color = Color(0xBB1E2430),
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .size(46.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val r = 16f
                val azRad = (azimuth * PI / 180.0).toFloat()
                val elRad = (elevation * PI / 180.0).toFloat()

                // X axis indicator (Red)
                val xx = cx + r * cos(azRad)
                val xy = cy + r * sin(azRad) * sin(elRad)
                drawLine(Color(0xFFEF4444), Offset(cx, cy), Offset(xx, xy), strokeWidth = 2.5f)

                // Y axis indicator (Green)
                val yx = cx - r * sin(azRad)
                val yy = cy + r * cos(azRad) * sin(elRad)
                drawLine(Color(0xFF22C55E), Offset(cx, cy), Offset(yx, yy), strokeWidth = 2.5f)

                // Z axis indicator (Blue)
                val zx = cx
                val zy = cy - r * cos(elRad)
                drawLine(Color(0xFF3B82F6), Offset(cx, cy), Offset(zx, zy), strokeWidth = 2.5f)
            }
        }

        // --- 4. Right Side Toolbar: Camera & Display Controls ---
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Predefined view toggle
            SmallFloatingActionButton(
                onClick = { resetCamera() },
                containerColor = Color(0xFF1E293B),
                contentColor = Color(0xFF38BDF8),
                modifier = Modifier.size(36.dp).testTag("viewer_reset_cam_btn")
            ) {
                Icon(Icons.Default.Refresh, "Restablecer cámara", modifier = Modifier.size(18.dp))
            }

            SmallFloatingActionButton(
                onClick = { fitToScreen() },
                containerColor = Color(0xFF1E293B),
                contentColor = Color(0xFF38BDF8),
                modifier = Modifier.size(36.dp).testTag("viewer_fit_btn")
            ) {
                Icon(Icons.Default.FitScreen, "Ajustar al modelo", modifier = Modifier.size(18.dp))
            }

            SmallFloatingActionButton(
                onClick = { onOpenPartsList() },
                containerColor = Color(0xFF1E293B),
                contentColor = Color(0xFF38BDF8),
                modifier = Modifier.size(36.dp).testTag("viewer_parts_list_btn")
            ) {
                Icon(Icons.Default.Layers, "Gestión de piezas", modifier = Modifier.size(18.dp))
            }

            SmallFloatingActionButton(
                onClick = { onOpenMeasurement() },
                containerColor = Color(0xFF1E293B),
                contentColor = Color(0xFFF59E0B),
                modifier = Modifier.size(36.dp).testTag("viewer_measure_btn")
            ) {
                Icon(Icons.Default.SquareFoot, "Medición CAD", modifier = Modifier.size(18.dp))
            }

            SmallFloatingActionButton(
                onClick = {
                    shadingMode = when (shadingMode) {
                        ViewerShadingMode.SOLID_WITH_EDGES -> ViewerShadingMode.SOLID
                        ViewerShadingMode.SOLID -> ViewerShadingMode.WIREFRAME_ONLY
                        ViewerShadingMode.WIREFRAME_ONLY -> ViewerShadingMode.SOLID_WITH_EDGES
                    }
                },
                containerColor = Color(0xFF1E293B),
                contentColor = Color(0xFF38BDF8),
                modifier = Modifier.size(36.dp).testTag("viewer_shading_btn")
            ) {
                Icon(Icons.Default.ViewInAr, "Modo de sombreado", modifier = Modifier.size(18.dp))
            }

            SmallFloatingActionButton(
                onClick = { showGrid = !showGrid },
                containerColor = Color(0xFF1E293B),
                contentColor = if (showGrid) Color(0xFF38BDF8) else Color.Gray,
                modifier = Modifier.size(36.dp).testTag("viewer_grid_btn")
            ) {
                Icon(Icons.Default.GridOn, "Cuadrícula Z=0", modifier = Modifier.size(18.dp))
            }
        }

        // --- 5. Bottom Predefined Views Bar ---
        Surface(
            color = Color(0xCC0F172A),
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    "3D" to PredefinedView.ISOMETRIC,
                    "Sup" to PredefinedView.TOP,
                    "Inf" to PredefinedView.BOTTOM,
                    "Front" to PredefinedView.FRONT,
                    "Post" to PredefinedView.BACK,
                    "Izq" to PredefinedView.LEFT,
                    "Der" to PredefinedView.RIGHT
                ).forEach { (label, view) ->
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { setPredefinedView(view) }
                    ) {
                        Text(
                            text = label,
                            color = Color(0xFFE2E8F0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawArrowHead(start: Offset, end: Offset, color: Color) {
    val angle = atan2(end.y - start.y, end.x - start.x)
    val arrowLen = 10f
    val arrowAngle = PI.toFloat() / 6f

    val x1 = end.x - arrowLen * cos(angle - arrowAngle)
    val y1 = end.y - arrowLen * sin(angle - arrowAngle)
    val x2 = end.x - arrowLen * cos(angle + arrowAngle)
    val y2 = end.y - arrowLen * sin(angle + arrowAngle)

    val path = Path().apply {
        moveTo(end.x, end.y)
        lineTo(x1, y1)
        lineTo(x2, y2)
        close()
    }
    drawPath(path, color)
}
