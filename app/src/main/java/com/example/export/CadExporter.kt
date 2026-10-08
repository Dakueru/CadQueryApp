package com.example.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.model.CadObject
import com.example.model.CadTriangle
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CadExporter {

    /**
     * Generates an ASCII STL file content
     */
    fun exportAsciiStl(objects: List<CadObject>, solidName: String = "CadQueryModel"): String {
        val sb = StringBuilder()
        sb.appendLine("solid $solidName")

        for (obj in objects.filter { it.isVisible }) {
            for (t in obj.triangles) {
                sb.appendLine("  facet normal ${t.normal.x} ${t.normal.y} ${t.normal.z}")
                sb.appendLine("    outer loop")
                sb.appendLine("      vertex ${t.v1.x} ${t.v1.y} ${t.v1.z}")
                sb.appendLine("      vertex ${t.v2.x} ${t.v2.y} ${t.v2.z}")
                sb.appendLine("      vertex ${t.v3.x} ${t.v3.y} ${t.v3.z}")
                sb.appendLine("    endloop")
                sb.appendLine("  endfacet")
            }
        }

        sb.appendLine("endsolid $solidName")
        return sb.toString()
    }

    /**
     * Generates a Binary STL byte array
     */
    fun exportBinaryStl(objects: List<CadObject>): ByteArray {
        val visibleTriangles = mutableListOf<CadTriangle>()
        for (obj in objects.filter { it.isVisible }) {
            visibleTriangles.addAll(obj.triangles)
        }

        val totalBytes = 80 + 4 + visibleTriangles.size * 50
        val buffer = ByteBuffer.allocate(totalBytes).order(ByteOrder.LITTLE_ENDIAN)

        // 80-byte header
        val header = "CadQuery Mobile Export - Binary STL".toByteArray(Charsets.US_ASCII)
        buffer.put(header)
        for (i in header.size until 80) {
            buffer.put(0.toByte())
        }

        // 4-byte unsigned int triangle count
        buffer.putInt(visibleTriangles.size)

        for (t in visibleTriangles) {
            // Normal (12 bytes)
            buffer.putFloat(t.normal.x)
            buffer.putFloat(t.normal.y)
            buffer.putFloat(t.normal.z)

            // V1 (12 bytes)
            buffer.putFloat(t.v1.x)
            buffer.putFloat(t.v1.y)
            buffer.putFloat(t.v1.z)

            // V2 (12 bytes)
            buffer.putFloat(t.v2.x)
            buffer.putFloat(t.v2.y)
            buffer.putFloat(t.v2.z)

            // V3 (12 bytes)
            buffer.putFloat(t.v3.x)
            buffer.putFloat(t.v3.y)
            buffer.putFloat(t.v3.z)

            // Attribute byte count (2 bytes)
            buffer.putShort(0.toShort())
        }

        return buffer.array()
    }

    /**
     * Generates a Wavefront OBJ file content
     */
    fun exportObj(objects: List<CadObject>): String {
        val sb = StringBuilder()
        sb.appendLine("# CadQuery Mobile - Wavefront OBJ Export")
        sb.appendLine("# Generated on ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")

        var vertexOffset = 1

        for (obj in objects.filter { it.isVisible }) {
            sb.appendLine("o ${obj.name.replace(" ", "_")}")

            for (t in obj.triangles) {
                sb.appendLine("v ${t.v1.x} ${t.v1.y} ${t.v1.z}")
                sb.appendLine("v ${t.v2.x} ${t.v2.y} ${t.v2.z}")
                sb.appendLine("v ${t.v3.x} ${t.v3.y} ${t.v3.z}")
            }

            for (t in obj.triangles) {
                sb.appendLine("vn ${t.normal.x} ${t.normal.y} ${t.normal.z}")
            }

            for (i in obj.triangles.indices) {
                val v1 = vertexOffset
                val v2 = vertexOffset + 1
                val v3 = vertexOffset + 2
                val vn = i + 1
                sb.appendLine("f $v1//$vn $v2//$vn $v3//$vn")
                vertexOffset += 3
            }
        }

        return sb.toString()
    }

    /**
     * Generates an ISO-10303-21 STEP CAD exchange file
     */
    fun exportStep(objects: List<CadObject>, modelName: String = "CadQuery_Part"): String {
        val sb = StringBuilder()
        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())

        sb.appendLine("ISO-10303-21;")
        sb.appendLine("HEADER;")
        sb.appendLine("FILE_DESCRIPTION(('CadQuery 3D Parametric CAD Model'),'2;1');")
        sb.appendLine("FILE_NAME('$modelName.step','$timestamp',('CadQuery Mobile Developer'),('AI Studio CadQuery'),'CadQuery Mobile Kernel','OpenCASCADE OCP Standard','None');")
        sb.appendLine("FILE_SCHEMA(('AUTOMOTIVE_DESIGN { 1 0 10303 214 1 1 1 1 }'));")
        sb.appendLine("ENDSEC;")
        sb.appendLine("DATA;")

        var id = 1
        val pointsMap = mutableListOf<String>()

        // Coordinate system
        sb.appendLine("#${id++}=CARTESIAN_POINT('Origin',(0.,0.,0.));")
        sb.appendLine("#${id++}=DIRECTION('Axis_Z',(0.,0.,1.));")
        sb.appendLine("#${id++}=DIRECTION('Axis_X',(1.,0.,0.));")
        sb.appendLine("#${id++}=AXIS2_PLACEMENT_3D('World_CS',#1,#2,#3);")

        // Faceted Brep for each visible object
        val brepIds = mutableListOf<Int>()
        for (obj in objects.filter { it.isVisible }) {
            val faceIds = mutableListOf<Int>()
            for (t in obj.triangles) {
                val p1Id = id++
                val p2Id = id++
                val p3Id = id++
                sb.appendLine("#$p1Id=CARTESIAN_POINT('',(${t.v1.x},${t.v1.y},${t.v1.z}));")
                sb.appendLine("#$p2Id=CARTESIAN_POINT('',(${t.v2.x},${t.v2.y},${t.v2.z}));")
                sb.appendLine("#$p3Id=CARTESIAN_POINT('',(${t.v3.x},${t.v3.y},${t.v3.z}));")

                val loopId = id++
                sb.appendLine("#$loopId=POLY_LOOP('',(#$p1Id,#$p2Id,#$p3Id));")

                val boundId = id++
                sb.appendLine("#$boundId=FACE_OUTER_BOUND('',#$loopId,.T.);")

                val faceId = id++
                sb.appendLine("#$faceId=FACE_SURFACE('',(#$boundId),#4,.T.);")
                faceIds.add(faceId)
            }

            val shellId = id++
            val faceListStr = faceIds.joinToString(",") { "#$it" }
            sb.appendLine("#$shellId=CLOSED_SHELL('${obj.name}',($faceListStr));")

            val brepId = id++
            sb.appendLine("#$brepId=FACETED_BREP('${obj.name}',#$shellId);")
            brepIds.add(brepId)
        }

        val manifoldId = id++
        val brepListStr = brepIds.joinToString(",") { "#$it" }
        sb.appendLine("#$manifoldId=MANIFOLD_SURFACE_SHAPE_REPRESENTATION('$modelName',($brepListStr),#4);")

        sb.appendLine("ENDSEC;")
        sb.appendLine("END-ISO-10303-21;")
        return sb.toString()
    }

    /**
     * Generates a 3MF XML model
     */
    fun export3mf(objects: List<CadObject>): String {
        val sb = StringBuilder()
        sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.appendLine("""<model unit="millimeter" xml:lang="en-US" xmlns="http://schemas.microsoft.com/3dmanufacturing/core/2015/02">""")
        sb.appendLine("""  <metadata name="Title">CadQuery Mobile Export</metadata>""")
        sb.appendLine("""  <resources>""")

        var objectId = 1
        for (obj in objects.filter { it.isVisible }) {
            sb.appendLine("""    <object id="$objectId" name="${obj.name}" type="model">""")
            sb.appendLine("""      <mesh>""")
            sb.appendLine("""        <vertices>""")

            val vertexList = mutableListOf<String>()
            val indexMap = mutableMapOf<String, Int>()
            val triangleIndices = mutableListOf<Triple<Int, Int, Int>>()

            fun getVertexIndex(x: Float, y: Float, z: Float): Int {
                val key = "$x,$y,$z"
                return indexMap.getOrPut(key) {
                    val idx = vertexList.size
                    vertexList.add("""          <vertex x="$x" y="$y" z="$z" />""")
                    idx
                }
            }

            for (t in obj.triangles) {
                val i1 = getVertexIndex(t.v1.x, t.v1.y, t.v1.z)
                val i2 = getVertexIndex(t.v2.x, t.v2.y, t.v2.z)
                val i3 = getVertexIndex(t.v3.x, t.v3.y, t.v3.z)
                triangleIndices.add(Triple(i1, i2, i3))
            }

            for (v in vertexList) {
                sb.appendLine(v)
            }
            sb.appendLine("""        </vertices>""")
            sb.appendLine("""        <triangles>""")
            for (tr in triangleIndices) {
                sb.appendLine("""          <triangle v1="${tr.first}" v2="${tr.second}" v3="${tr.third}" />""")
            }
            sb.appendLine("""        </triangles>""")
            sb.appendLine("""      </mesh>""")
            sb.appendLine("""    </object>""")
            objectId++
        }

        sb.appendLine("""  </resources>""")
        sb.appendLine("""  <build>""")
        for (i in 1 until objectId) {
            sb.appendLine("""    <item objectid="$i" />""")
        }
        sb.appendLine("""  </build>""")
        sb.appendLine("""</model>""")
        return sb.toString()
    }

    /**
     * Writes model to cache/export file and returns an Android Share Intent
     */
    fun saveAndCreateShareIntent(
        context: Context,
        filename: String,
        format: String,
        objects: List<CadObject>
    ): Intent {
        val extension = format.lowercase()
        val exportDir = File(context.cacheDir, "cad_exports")
        if (!exportDir.exists()) exportDir.mkdirs()

        val cleanName = filename.removeSuffix(".$extension")
        val file = File(exportDir, "$cleanName.$extension")

        when (format.uppercase()) {
            "STL" -> {
                val bytes = exportBinaryStl(objects)
                FileOutputStream(file).use { it.write(bytes) }
            }
            "OBJ" -> {
                val text = exportObj(objects)
                PrintWriter(file).use { it.write(text) }
            }
            "STEP" -> {
                val text = exportStep(objects, cleanName)
                PrintWriter(file).use { it.write(text) }
            }
            "3MF" -> {
                val text = export3mf(objects)
                PrintWriter(file).use { it.write(text) }
            }
            else -> {
                val text = exportAsciiStl(objects, cleanName)
                PrintWriter(file).use { it.write(text) }
            }
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = when (format.uppercase()) {
                "STL" -> "application/sla"
                "OBJ" -> "model/obj"
                "STEP" -> "application/step"
                "3MF" -> "model/3mf"
                else -> "application/octet-stream"
            }
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Exportación CAD: $cleanName.$extension")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(intent, "Compartir archivo $format")
    }
}
