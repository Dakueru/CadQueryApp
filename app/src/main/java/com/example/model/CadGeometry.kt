package com.example.model

import androidx.compose.ui.graphics.Color
import kotlin.math.sqrt

/**
 * 3D Point / Vector for CAD geometry
 */
data class CadPoint3D(
    val x: Float,
    val y: Float,
    val z: Float
) {
    operator fun plus(other: CadPoint3D) = CadPoint3D(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: CadPoint3D) = CadPoint3D(x - other.x, y - other.y, z - other.z)
    operator fun times(scale: Float) = CadPoint3D(x * scale, y * scale, z * scale)
    operator fun div(scale: Float) = CadPoint3D(x / scale, y / scale, z / scale)

    fun dot(other: CadPoint3D): Float = x * other.x + y * other.y + z * other.z

    fun cross(other: CadPoint3D): CadPoint3D = CadPoint3D(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )

    fun length(): Float = sqrt(x * x + y * y + z * z)

    fun distanceTo(other: CadPoint3D): Float = (this - other).length()

    fun normalized(): CadPoint3D {
        val len = length()
        return if (len > 0.00001f) this / len else CadPoint3D(0f, 0f, 1f)
    }

    companion object {
        val ZERO = CadPoint3D(0f, 0f, 0f)
        val UNIT_X = CadPoint3D(1f, 0f, 0f)
        val UNIT_Y = CadPoint3D(0f, 1f, 0f)
        val UNIT_Z = CadPoint3D(0f, 0f, 1f)
    }
}

/**
 * Triangulated surface facet
 */
data class CadTriangle(
    val v1: CadPoint3D,
    val v2: CadPoint3D,
    val v3: CadPoint3D,
    val normal: CadPoint3D = computeNormal(v1, v2, v3),
    val faceTag: String? = null
) {
    companion object {
        fun computeNormal(p1: CadPoint3D, p2: CadPoint3D, p3: CadPoint3D): CadPoint3D {
            val edge1 = p2 - p1
            val edge2 = p3 - p1
            return edge1.cross(edge2).normalized()
        }
    }

    fun area(): Float {
        val edge1 = v2 - v1
        val edge2 = v3 - v1
        return edge1.cross(edge2).length() * 0.5f
    }
}

/**
 * Sharp feature edge or wireframe line
 */
data class CadEdge(
    val start: CadPoint3D,
    val end: CadPoint3D,
    val isFeatureEdge: Boolean = true,
    val edgeTag: String? = null
) {
    fun length(): Float = start.distanceTo(end)
}

/**
 * Topological Face representation for CadQuery face selectors and measurement
 */
data class CadFace(
    val tag: String,
    val normal: CadPoint3D,
    val center: CadPoint3D,
    val area: Float,
    val isCurved: Boolean = false,
    val diameter: Float? = null,
    val boundaryEdges: List<CadEdge> = emptyList()
)

/**
 * Bounding Box
 */
data class CadBoundingBox(
    val minX: Float,
    val maxX: Float,
    val minY: Float,
    val maxY: Float,
    val minZ: Float,
    val maxZ: Float
) {
    val sizeX: Float get() = maxX - minX
    val sizeY: Float get() = maxY - minY
    val sizeZ: Float get() = maxZ - minZ

    val centerX: Float get() = (minX + maxX) / 2f
    val centerY: Float get() = (minY + maxY) / 2f
    val centerZ: Float get() = (minZ + maxZ) / 2f
    val center: CadPoint3D get() = CadPoint3D(centerX, centerY, centerZ)

    val maxDimension: Float get() = maxOf(sizeX, maxOf(sizeY, sizeZ))
    val diagonal: Float get() = sqrt(sizeX * sizeX + sizeY * sizeY + sizeZ * sizeZ)

    companion object {
        val EMPTY = CadBoundingBox(0f, 0f, 0f, 0f, 0f, 0f)

        fun fromPoints(points: List<CadPoint3D>): CadBoundingBox {
            if (points.isEmpty()) return EMPTY
            var minX = Float.MAX_VALUE
            var maxX = -Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxY = -Float.MAX_VALUE
            var minZ = Float.MAX_VALUE
            var maxZ = -Float.MAX_VALUE

            for (p in points) {
                if (p.x < minX) minX = p.x
                if (p.x > maxX) maxX = p.x
                if (p.y < minY) minY = p.y
                if (p.y > maxY) maxY = p.y
                if (p.z < minZ) minZ = p.z
                if (p.z > maxZ) maxZ = p.z
            }
            return CadBoundingBox(minX, maxX, minY, maxY, minZ, maxZ)
        }
    }
}

/**
 * Independent CAD Part/Object
 */
data class CadObject(
    val id: String,
    val name: String,
    val triangles: List<CadTriangle>,
    val featureEdges: List<CadEdge>,
    val faces: List<CadFace> = emptyList(),
    val isVisible: Boolean = true,
    val color: Color = Color(0xFF38BDF8),
    val alpha: Float = 1.0f,
    val boundingBox: CadBoundingBox = computeBoundingBox(triangles),
    val volume: Float = computeVolume(triangles),
    val surfaceArea: Float = computeSurfaceArea(triangles)
) {
    companion object {
        fun computeBoundingBox(triangles: List<CadTriangle>): CadBoundingBox {
            val points = ArrayList<CadPoint3D>(triangles.size * 3)
            for (t in triangles) {
                points.add(t.v1)
                points.add(t.v2)
                points.add(t.v3)
            }
            return CadBoundingBox.fromPoints(points)
        }

        fun computeSurfaceArea(triangles: List<CadTriangle>): Float {
            var sum = 0f
            for (t in triangles) {
                sum += t.area()
            }
            return sum
        }

        /**
         * Calculates closed mesh volume via divergence theorem:
         * Volume = 1/6 * sum(v1 dot (v2 cross v3))
         */
        fun computeVolume(triangles: List<CadTriangle>): Float {
            var signedVolume = 0.0
            for (t in triangles) {
                val cross = t.v2.cross(t.v3)
                signedVolume += t.v1.dot(cross)
            }
            return (kotlin.math.abs(signedVolume) / 6.0).toFloat()
        }
    }
}
