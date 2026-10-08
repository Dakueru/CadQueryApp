package com.example.engine

import androidx.compose.ui.graphics.Color
import com.example.model.CadBoundingBox
import com.example.model.CadEdge
import com.example.model.CadFace
import com.example.model.CadObject
import com.example.model.CadPoint3D
import com.example.model.CadTriangle
import java.util.UUID
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object CadGeometryBuilder {

    /**
     * Builds a rectangular box with optional fillet on vertical (|Z) edges
     */
    fun createBox(
        name: String = "Caja",
        length: Float,
        width: Float,
        height: Float,
        filletRadius: Float = 0f,
        chamferLength: Float = 0f,
        holeDiameter: Float = 0f,
        color: Color = Color(0xFF38BDF8)
    ): CadObject {
        val halfL = length / 2f
        val halfW = width / 2f
        val halfH = height / 2f

        val triangles = mutableListOf<CadTriangle>()
        val edges = mutableListOf<CadEdge>()
        val faces = mutableListOf<CadFace>()

        if (filletRadius > 0.001f && filletRadius < minOf(halfL, halfW)) {
            // Box with rounded vertical edges (|Z fillet)
            val r = filletRadius
            val xInner = halfL - r
            val yInner = halfW - r
            val segments = 8

            // 4 Corner centers
            val centers = listOf(
                Pair(xInner, yInner) to 0f, // Q1: 0 to PI/2
                Pair(-xInner, yInner) to (PI / 2).toFloat(), // Q2: PI/2 to PI
                Pair(-xInner, -yInner) to PI.toFloat(), // Q3: PI to 3PI/2
                Pair(xInner, -yInner) to (3 * PI / 2).toFloat() // Q4: 3PI/2 to 2PI
            )

            val bottomLoop = mutableListOf<CadPoint3D>()
            val topLoop = mutableListOf<CadPoint3D>()

            for ((centerPair, startAngle) in centers) {
                val (cx, cy) = centerPair
                for (i in 0 until segments) {
                    val angle = startAngle + (PI / 2).toFloat() * (i.toFloat() / segments)
                    val x = cx + r * cos(angle)
                    val y = cy + r * sin(angle)
                    bottomLoop.add(CadPoint3D(x, y, -halfH))
                    topLoop.add(CadPoint3D(x, y, halfH))
                }
            }

            val n = bottomLoop.size
            // Side cylindrical and flat walls
            for (i in 0 until n) {
                val next = (i + 1) % n
                val b1 = bottomLoop[i]
                val b2 = bottomLoop[next]
                val t1 = topLoop[i]
                val t2 = topLoop[next]

                triangles.add(CadTriangle(b1, b2, t2))
                triangles.add(CadTriangle(b1, t2, t1))

                // Feature edges: top and bottom loops, plus corner seam edges
                edges.add(CadEdge(b1, b2, isFeatureEdge = true))
                edges.add(CadEdge(t1, t2, isFeatureEdge = true))
                if (i % segments == 0) {
                    edges.add(CadEdge(b1, t1, isFeatureEdge = true, edgeTag = "|Z"))
                }
            }

            // Top and bottom cap fan (if no hole)
            if (holeDiameter <= 0.001f) {
                val topCenter = CadPoint3D(0f, 0f, halfH)
                val bottomCenter = CadPoint3D(0f, 0f, -halfH)
                for (i in 0 until n) {
                    val next = (i + 1) % n
                    triangles.add(CadTriangle(topCenter, topLoop[i], topLoop[next]))
                    triangles.add(CadTriangle(bottomCenter, bottomLoop[next], bottomLoop[i]))
                }
            } else {
                // Annular caps around central hole
                val holeR = holeDiameter / 2f
                val holeTopLoop = mutableListOf<CadPoint3D>()
                val holeBottomLoop = mutableListOf<CadPoint3D>()
                for (i in 0 until n) {
                    val angle = 2 * PI.toFloat() * (i.toFloat() / n)
                    holeTopLoop.add(CadPoint3D(holeR * cos(angle), holeR * sin(angle), halfH))
                    holeBottomLoop.add(CadPoint3D(holeR * cos(angle), holeR * sin(angle), -halfH))
                }

                for (i in 0 until n) {
                    val next = (i + 1) % n
                    // Top annular ring
                    triangles.add(CadTriangle(holeTopLoop[i], topLoop[i], topLoop[next]))
                    triangles.add(CadTriangle(holeTopLoop[i], topLoop[next], holeTopLoop[next]))

                    // Bottom annular ring
                    triangles.add(CadTriangle(holeBottomLoop[i], bottomLoop[next], bottomLoop[i]))
                    triangles.add(CadTriangle(holeBottomLoop[i], holeBottomLoop[next], bottomLoop[next]))

                    // Inner cylinder wall for the hole
                    triangles.add(CadTriangle(holeBottomLoop[i], holeTopLoop[i], holeTopLoop[next]))
                    triangles.add(CadTriangle(holeBottomLoop[i], holeTopLoop[next], holeBottomLoop[next]))

                    edges.add(CadEdge(holeTopLoop[i], holeTopLoop[next], isFeatureEdge = true))
                    edges.add(CadEdge(holeBottomLoop[i], holeBottomLoop[next], isFeatureEdge = true))
                }
            }
        } else {
            // Standard sharp rectangular prism box
            val p0 = CadPoint3D(-halfL, -halfW, -halfH)
            val p1 = CadPoint3D(halfL, -halfW, -halfH)
            val p2 = CadPoint3D(halfL, halfW, -halfH)
            val p3 = CadPoint3D(-halfL, halfW, -halfH)

            val p4 = CadPoint3D(-halfL, -halfW, halfH)
            val p5 = CadPoint3D(halfL, -halfW, halfH)
            val p6 = CadPoint3D(halfL, halfW, halfH)
            val p7 = CadPoint3D(-halfL, halfW, halfH)

            // Front (+Y): p3, p2, p6, p7
            triangles.add(CadTriangle(p3, p2, p6))
            triangles.add(CadTriangle(p3, p6, p7))

            // Back (-Y): p0, p4, p5, p1
            triangles.add(CadTriangle(p0, p4, p5))
            triangles.add(CadTriangle(p0, p5, p1))

            // Right (+X): p1, p5, p6, p2
            triangles.add(CadTriangle(p1, p5, p6))
            triangles.add(CadTriangle(p1, p6, p2))

            // Left (-X): p0, p3, p7, p4
            triangles.add(CadTriangle(p0, p3, p7))
            triangles.add(CadTriangle(p0, p7, p4))

            if (holeDiameter <= 0.001f) {
                // Top (+Z)
                triangles.add(CadTriangle(p4, p7, p6))
                triangles.add(CadTriangle(p4, p6, p5))

                // Bottom (-Z)
                triangles.add(CadTriangle(p0, p1, p2))
                triangles.add(CadTriangle(p0, p2, p3))
            } else {
                // Top (+Z) & Bottom (-Z) with hole cutout
                val holeR = holeDiameter / 2f
                val segments = 24
                val topHole = mutableListOf<CadPoint3D>()
                val bottomHole = mutableListOf<CadPoint3D>()
                for (i in 0 until segments) {
                    val angle = 2 * PI.toFloat() * (i.toFloat() / segments)
                    val x = holeR * cos(angle)
                    val y = holeR * sin(angle)
                    topHole.add(CadPoint3D(x, y, halfH))
                    bottomHole.add(CadPoint3D(x, y, -halfH))
                }

                // Inner cylinder wall
                for (i in 0 until segments) {
                    val next = (i + 1) % segments
                    triangles.add(CadTriangle(bottomHole[i], topHole[i], topHole[next]))
                    triangles.add(CadTriangle(bottomHole[i], topHole[next], bottomHole[next]))
                    edges.add(CadEdge(topHole[i], topHole[next], isFeatureEdge = true))
                    edges.add(CadEdge(bottomHole[i], bottomHole[next], isFeatureEdge = true))
                }

                // Subdivided top and bottom faces to accommodate hole
                val topOuter = listOf(p4, p5, p6, p7)
                val bottomOuter = listOf(p0, p1, p2, p3)
                for (i in 0 until segments) {
                    val next = (i + 1) % segments
                    val outerIdx = (i * 4) / segments
                    val outerNextIdx = ((next * 4) / segments)
                    triangles.add(CadTriangle(topHole[i], topOuter[outerIdx], topOuter[outerNextIdx]))
                    triangles.add(CadTriangle(topHole[i], topOuter[outerNextIdx], topHole[next]))

                    triangles.add(CadTriangle(bottomHole[i], bottomOuter[outerNextIdx], bottomOuter[outerIdx]))
                    triangles.add(CadTriangle(bottomHole[i], bottomHole[next], bottomOuter[outerNextIdx]))
                }
            }

            // 12 Outer Box Edges
            // Bottom 4
            edges.add(CadEdge(p0, p1, true, edgeTag = "-Z"))
            edges.add(CadEdge(p1, p2, true, edgeTag = "-Z"))
            edges.add(CadEdge(p2, p3, true, edgeTag = "-Z"))
            edges.add(CadEdge(p3, p0, true, edgeTag = "-Z"))
            // Top 4
            edges.add(CadEdge(p4, p5, true, edgeTag = "+Z"))
            edges.add(CadEdge(p5, p6, true, edgeTag = "+Z"))
            edges.add(CadEdge(p6, p7, true, edgeTag = "+Z"))
            edges.add(CadEdge(p7, p4, true, edgeTag = "+Z"))
            // Vertical 4 (|Z)
            edges.add(CadEdge(p0, p4, true, edgeTag = "|Z"))
            edges.add(CadEdge(p1, p5, true, edgeTag = "|Z"))
            edges.add(CadEdge(p2, p6, true, edgeTag = "|Z"))
            edges.add(CadEdge(p3, p7, true, edgeTag = "|Z"))

            // 6 Principal Faces metadata
            faces.add(CadFace(">Z", CadPoint3D(0f, 0f, 1f), CadPoint3D(0f, 0f, halfH), length * width))
            faces.add(CadFace("<Z", CadPoint3D(0f, 0f, -1f), CadPoint3D(0f, 0f, -halfH), length * width))
            faces.add(CadFace(">X", CadPoint3D(1f, 0f, 0f), CadPoint3D(halfL, 0f, 0f), width * height))
            faces.add(CadFace("<X", CadPoint3D(-1f, 0f, 0f), CadPoint3D(-halfL, 0f, 0f), width * height))
            faces.add(CadFace(">Y", CadPoint3D(0f, 1f, 0f), CadPoint3D(0f, halfW, 0f), length * height))
            faces.add(CadFace("<Y", CadPoint3D(0f, -1f, 0f), CadPoint3D(0f, -halfW, 0f), length * height))
        }

        return CadObject(
            id = UUID.randomUUID().toString(),
            name = name,
            triangles = triangles,
            featureEdges = edges,
            faces = faces,
            color = color
        )
    }

    /**
     * Builds a cylinder
     */
    fun createCylinder(
        name: String = "Cilindro",
        height: Float,
        radius: Float,
        segments: Int = 32,
        color: Color = Color(0xFFF59E0B),
        centerOffset: CadPoint3D = CadPoint3D.ZERO
    ): CadObject {
        val halfH = height / 2f
        val triangles = mutableListOf<CadTriangle>()
        val edges = mutableListOf<CadEdge>()

        val topPoints = ArrayList<CadPoint3D>(segments)
        val bottomPoints = ArrayList<CadPoint3D>(segments)

        for (i in 0 until segments) {
            val angle = 2 * PI.toFloat() * (i.toFloat() / segments)
            val x = radius * cos(angle) + centerOffset.x
            val y = radius * sin(angle) + centerOffset.y
            topPoints.add(CadPoint3D(x, y, halfH + centerOffset.z))
            bottomPoints.add(CadPoint3D(x, y, -halfH + centerOffset.z))
        }

        val topCenter = CadPoint3D(centerOffset.x, centerOffset.y, halfH + centerOffset.z)
        val bottomCenter = CadPoint3D(centerOffset.x, centerOffset.y, -halfH + centerOffset.z)

        for (i in 0 until segments) {
            val next = (i + 1) % segments
            val t1 = topPoints[i]
            val t2 = topPoints[next]
            val b1 = bottomPoints[i]
            val b2 = bottomPoints[next]

            // Top Cap
            triangles.add(CadTriangle(topCenter, t1, t2))
            // Bottom Cap
            triangles.add(CadTriangle(bottomCenter, b2, b1))
            // Side
            triangles.add(CadTriangle(b1, b2, t2))
            triangles.add(CadTriangle(b1, t2, t1))

            // Feature edges at circular rims
            edges.add(CadEdge(t1, t2, true))
            edges.add(CadEdge(b1, b2, true))
        }

        val faces = listOf(
            CadFace(">Z", CadPoint3D(0f, 0f, 1f), topCenter, (PI * radius * radius).toFloat()),
            CadFace("<Z", CadPoint3D(0f, 0f, -1f), bottomCenter, (PI * radius * radius).toFloat()),
            CadFace("CYLINDER_SURFACE", CadPoint3D(1f, 0f, 0f), centerOffset, (2 * PI * radius * height).toFloat(), isCurved = true, diameter = radius * 2f)
        )

        return CadObject(
            id = UUID.randomUUID().toString(),
            name = name,
            triangles = triangles,
            featureEdges = edges,
            faces = faces,
            color = color
        )
    }

    /**
     * Builds a sphere
     */
    fun createSphere(
        name: String = "Esfera",
        radius: Float,
        latitudes: Int = 16,
        longitudes: Int = 24,
        color: Color = Color(0xFF10B981)
    ): CadObject {
        val triangles = mutableListOf<CadTriangle>()
        val edges = mutableListOf<CadEdge>()

        for (lat in 0 until latitudes) {
            val theta1 = PI.toFloat() * (lat.toFloat() / latitudes)
            val theta2 = PI.toFloat() * ((lat + 1).toFloat() / latitudes)

            for (lon in 0 until longitudes) {
                val phi1 = 2 * PI.toFloat() * (lon.toFloat() / longitudes)
                val phi2 = 2 * PI.toFloat() * ((lon + 1).toFloat() / longitudes)

                val p1 = CadPoint3D(radius * sin(theta1) * cos(phi1), radius * sin(theta1) * sin(phi1), radius * cos(theta1))
                val p2 = CadPoint3D(radius * sin(theta2) * cos(phi1), radius * sin(theta2) * sin(phi1), radius * cos(theta2))
                val p3 = CadPoint3D(radius * sin(theta2) * cos(phi2), radius * sin(theta2) * sin(phi2), radius * cos(theta2))
                val p4 = CadPoint3D(radius * sin(theta1) * cos(phi2), radius * sin(theta1) * sin(phi2), radius * cos(theta1))

                if (lat != 0) {
                    triangles.add(CadTriangle(p1, p2, p3))
                }
                if (lat != latitudes - 1) {
                    triangles.add(CadTriangle(p1, p3, p4))
                }

                if (lat % 4 == 0) {
                    edges.add(CadEdge(p1, p4, true))
                }
            }
        }

        return CadObject(
            id = UUID.randomUUID().toString(),
            name = name,
            triangles = triangles,
            featureEdges = edges,
            color = color
        )
    }
}
