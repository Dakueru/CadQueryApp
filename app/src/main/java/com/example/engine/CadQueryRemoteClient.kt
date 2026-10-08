package com.example.engine

import androidx.compose.ui.graphics.Color
import com.example.model.CadBoundingBox
import com.example.model.CadEdge
import com.example.model.CadError
import com.example.model.CadExecutionResult
import com.example.model.CadFace
import com.example.model.CadObject
import com.example.model.CadPoint3D
import com.example.model.CadTriangle
import com.example.model.EngineType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

class CadQueryRemoteClient(
    private var serverUrl: String = "http://10.0.2.2:5000"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    fun setServerUrl(url: String) {
        serverUrl = url.trim().removeSuffix("/")
    }

    fun getServerUrl(): String = serverUrl

    suspend fun checkHealth(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$serverUrl/health")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    val engine = json.optString("engine", "CadQuery Real")
                    val version = json.optString("version", "")
                    Pair(true, "$engine (v$version)")
                } else {
                    Pair(false, "Servidor respondió con código ${response.code}")
                }
            }
        } catch (e: Exception) {
            Pair(false, "No se puede conectar a $serverUrl: ${e.message}")
        }
    }

    suspend fun executeScript(scriptCode: String): CadExecutionResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val jsonPayload = JSONObject().apply {
                put("code", scriptCode)
            }
            val body = jsonPayload.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$serverUrl/execute")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val responseString = response.body?.string() ?: "{}"
                val json = JSONObject(responseString)
                val success = json.optBoolean("success", false)
                val stdout = json.optString("stdout", "")
                val executionTimeMs = json.optLong("execution_time_ms", System.currentTimeMillis() - startTime)

                if (success) {
                    val objectsJson = json.optJSONArray("objects") ?: JSONArray()
                    val cadObjects = parseCadObjectsFromJson(objectsJson)

                    CadExecutionResult(
                        objects = cadObjects,
                        stdout = stdout,
                        errors = emptyList(),
                        executionTimeMs = executionTimeMs,
                        isSuccess = true,
                        engineType = EngineType.REMOTE_CADQUERY
                    )
                } else {
                    val errorMsg = json.optString("error", "Error en el servidor CadQuery")
                    val line = if (json.has("line") && !json.isNull("line")) json.getInt("line") else null
                    val tb = json.optString("traceback", "")

                    CadExecutionResult(
                        objects = emptyList(),
                        stdout = stdout,
                        errors = listOf(
                            CadError(
                                lineNumber = line,
                                message = errorMsg,
                                rawTraceback = tb,
                                errorType = "CadQueryError"
                            )
                        ),
                        executionTimeMs = executionTimeMs,
                        isSuccess = false,
                        engineType = EngineType.REMOTE_CADQUERY
                    )
                }
            }
        } catch (e: IOException) {
            CadExecutionResult(
                objects = emptyList(),
                stdout = "",
                errors = listOf(
                    CadError(
                        lineNumber = null,
                        message = "Fallo de conexión con el servidor CadQuery ($serverUrl). Comprueba que el servidor esté activo o cambia a modo Motor Autónomo Móvil.",
                        rawTraceback = e.stackTraceToString(),
                        errorType = "NetworkError"
                    )
                ),
                executionTimeMs = System.currentTimeMillis() - startTime,
                isSuccess = false,
                engineType = EngineType.REMOTE_CADQUERY
            )
        }
    }

    suspend fun exportGeometry(format: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$serverUrl/export/$format")
                .post("{}".toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.bytes()
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseCadObjectsFromJson(jsonArray: JSONArray): List<CadObject> {
        val result = mutableListOf<CadObject>()
        val palette = listOf(
            Color(0xFF38BDF8),
            Color(0xFFF59E0B),
            Color(0xFF10B981),
            Color(0xFFA855F7),
            Color(0xFFEC4899)
        )

        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.getJSONObject(i)
            val name = item.optString("name", "Pieza ${i + 1}")
            val verticesArr = item.optJSONArray("vertices") ?: JSONArray()
            val trianglesArr = item.optJSONArray("triangles") ?: JSONArray()

            val vertexList = ArrayList<CadPoint3D>(verticesArr.length())
            for (vIdx in 0 until verticesArr.length()) {
                val coords = verticesArr.getJSONArray(vIdx)
                vertexList.add(
                    CadPoint3D(
                        coords.getDouble(0).toFloat(),
                        coords.getDouble(1).toFloat(),
                        coords.getDouble(2).toFloat()
                    )
                )
            }

            val triangles = ArrayList<CadTriangle>(trianglesArr.length())
            val edges = mutableListOf<CadEdge>()

            for (tIdx in 0 until trianglesArr.length()) {
                val idxs = trianglesArr.getJSONArray(tIdx)
                val i1 = idxs.getInt(0)
                val i2 = idxs.getInt(1)
                val i3 = idxs.getInt(2)

                if (i1 < vertexList.size && i2 < vertexList.size && i3 < vertexList.size) {
                    val p1 = vertexList[i1]
                    val p2 = vertexList[i2]
                    val p3 = vertexList[i3]
                    triangles.add(CadTriangle(p1, p2, p3))

                    // Extract edges
                    if (tIdx % 3 == 0) {
                        edges.add(CadEdge(p1, p2, true))
                        edges.add(CadEdge(p2, p3, true))
                        edges.add(CadEdge(p3, p1, true))
                    }
                }
            }

            val vol = item.optDouble("volume", 0.0).toFloat()
            val bboxObj = item.optJSONObject("bbox")
            val bbox = if (bboxObj != null) {
                CadBoundingBox(
                    bboxObj.getDouble("minX").toFloat(),
                    bboxObj.getDouble("maxX").toFloat(),
                    bboxObj.getDouble("minY").toFloat(),
                    bboxObj.getDouble("maxY").toFloat(),
                    bboxObj.getDouble("minZ").toFloat(),
                    bboxObj.getDouble("maxZ").toFloat()
                )
            } else {
                CadBoundingBox.fromPoints(vertexList)
            }

            result.add(
                CadObject(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    triangles = triangles,
                    featureEdges = edges,
                    color = palette[i % palette.size],
                    boundingBox = bbox,
                    volume = if (vol > 0) vol else CadObject.computeVolume(triangles)
                )
            )
        }
        return result
    }
}
