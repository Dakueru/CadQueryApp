package com.example.model

enum class EngineType(val displayName: String, val badge: String) {
    REMOTE_CADQUERY("Servidor CadQuery (OCP/Python Real)", "OCP Real"),
    STANDALONE_OFFLINE("Motor Autónomo Móvil (CadQuery Core)", "Móvil Offline")
}

data class CadExecutionResult(
    val objects: List<CadObject>,
    val stdout: String,
    val errors: List<CadError>,
    val executionTimeMs: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val isSuccess: Boolean,
    val engineType: EngineType
)

data class CadError(
    val lineNumber: Int?,
    val message: String,
    val rawTraceback: String,
    val errorType: String = "Exception"
)

data class CadScript(
    val id: String,
    val name: String,
    val content: String,
    val lastModified: Long = System.currentTimeMillis(),
    val isDraft: Boolean = false,
    val path: String? = null
)

data class CadParameter(
    val name: String,
    val value: Float,
    val min: Float,
    val max: Float,
    val step: Float = 1f,
    val lineNumber: Int
)

data class CadDocItem(
    val name: String,
    val signature: String,
    val category: String,
    val description: String,
    val parameters: List<String>,
    val example: String
)

enum class MeasurementType {
    DISTANCE,
    EDGE_LENGTH,
    DIAMETER_RADIUS,
    FACE_AREA,
    VOLUME,
    BOUNDING_BOX
}

data class MeasurementResult(
    val type: MeasurementType,
    val title: String,
    val valueString: String,
    val details: String,
    val point1: CadPoint3D? = null,
    val point2: CadPoint3D? = null
)

data class ScriptSnapshot(
    val id: String,
    val timestamp: Long,
    val title: String,
    val content: String,
    val lineCount: Int
)

enum class ViewerShadingMode {
    SOLID,
    SOLID_WITH_EDGES,
    WIREFRAME_ONLY
}

enum class PredefinedView {
    ISOMETRIC,
    TOP,
    BOTTOM,
    FRONT,
    BACK,
    LEFT,
    RIGHT
}
