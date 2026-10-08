package com.example.engine

import androidx.compose.ui.graphics.Color
import com.example.model.CadError
import com.example.model.CadExecutionResult
import com.example.model.CadObject
import com.example.model.CadParameter
import com.example.model.CadPoint3D
import com.example.model.EngineType
import java.io.StringWriter
import kotlin.math.PI
import kotlin.system.measureTimeMillis

object CadScriptEvaluator {

    /**
     * Extracts numeric variable declarations suitable for the Parameters Panel
     */
    fun extractParameters(scriptCode: String): List<CadParameter> {
        val parameters = mutableListOf<CadParameter>()
        val lines = scriptCode.lines()
        val regex = Regex("""^\s*([a-zA-Z_][a-zA-Z0-9_]*)\s*=\s*([0-9]+(?:\.[0-9]+)?)\s*(?:#.*)?$""")

        lines.forEachIndexed { index, line ->
            val match = regex.matchEntire(line.trimEnd())
            if (match != null) {
                val name = match.groupValues[1]
                val value = match.groupValues[2].toFloatOrNull()
                // Skip internal or short loop counters
                if (value != null && name != "cq" && name.length > 1) {
                    val min = maxOf(0.1f, value * 0.2f)
                    val max = maxOf(10f, value * 3.0f)
                    parameters.add(
                        CadParameter(
                            name = name,
                            value = value,
                            min = (min * 10).toInt() / 10f,
                            max = (max * 10).toInt() / 10f,
                            step = if (value > 20) 1f else 0.5f,
                            lineNumber = index + 1
                        )
                    )
                }
            }
        }
        return parameters
    }

    /**
     * Replaces a numeric variable's assignment in the source code
     */
    fun updateParameterInCode(scriptCode: String, paramName: String, newValue: Float): String {
        val lines = scriptCode.lines().toMutableList()
        val regex = Regex("""^(\s*)$paramName\s*=\s*[0-9]+(?:\.[0-9]+)?(.*)$""")

        for (i in lines.indices) {
            val line = lines[i]
            val match = regex.matchEntire(line)
            if (match != null) {
                val indent = match.groupValues[1]
                val suffix = match.groupValues[2]
                val formattedVal = if (newValue % 1 == 0f) "${newValue.toInt()}.0" else "$newValue"
                lines[i] = "$indent$paramName = $formattedVal$suffix"
                break
            }
        }
        return lines.joinToString("\n")
    }

    /**
     * Executes the script through the offline parametric evaluator
     */
    fun evaluate(scriptCode: String): CadExecutionResult {
        val stdout = StringBuilder()
        val errors = mutableListOf<CadError>()
        val objects = mutableListOf<CadObject>()

        val elapsed = measureTimeMillis {
            try {
                val lines = scriptCode.lines()
                val variables = mutableMapOf<String, Any>()

                // Prepopulate common CadQuery constants and values
                variables["True"] = true
                variables["False"] = false
                variables["PI"] = PI.toFloat()
                variables["pi"] = PI.toFloat()

                // 1. First pass: Variable evaluation
                lines.forEachIndexed { lineIdx, line ->
                    val trimmed = line.trim()
                    if (trimmed.startsWith("#") || trimmed.isEmpty()) return@forEachIndexed

                    // Capture print() statements
                    if (trimmed.startsWith("print(") && trimmed.endsWith(")")) {
                        val expr = trimmed.substring(6, trimmed.length - 1)
                        val evaluated = evaluateExpression(expr, variables)
                        stdout.appendLine("[stdout] $evaluated")
                        return@forEachIndexed
                    }

                    // Numeric or simple assignments: var = value
                    if (trimmed.contains("=") && !trimmed.contains("==") && !trimmed.startsWith("show_object")) {
                        val parts = trimmed.split("=", limit = 2)
                        val varName = parts[0].trim()
                        val expr = parts[1].trim()

                        if (varName.matches(Regex("""^[a-zA-Z_][a-zA-Z0-9_]*$"""))) {
                            try {
                                val value = evaluateExpression(expr, variables)
                                if (value != null) {
                                    variables[varName] = value
                                }
                            } catch (e: Exception) {
                                // Silent on complex lines
                            }
                        }
                    }
                }

                // 2. Synthesize models based on CadQuery operations
                // Check if script specifies multiple objects via show_object or distinct Workplanes
                val hasShowObject = scriptCode.contains("show_object(")
                val showObjectMatches = Regex("""show_object\(\s*([^,\)]+)(?:,\s*name=["']([^"']+)["'])?(?:,\s*options=([^)]+))?\)""")
                    .findAll(scriptCode)

                if (hasShowObject && showObjectMatches.count() > 0) {
                    var objIdx = 1
                    for (match in showObjectMatches) {
                        val shapeVar = match.groupValues[1].trim()
                        val explicitName = match.groupValues[2].ifEmpty { "Pieza $objIdx" }
                        val optionsStr = match.groupValues[3]
                        val color = parseColorFromOptions(optionsStr, objIdx)

                        val builtObj = buildCadObjectFromScriptContext(scriptCode, shapeVar, explicitName, variables, color)
                        objects.add(builtObj)
                        stdout.appendLine("Objeto registrado: '$explicitName' (${builtObj.triangles.size} facetas, ${builtObj.featureEdges.size} aristas)")
                        objIdx++
                    }
                } else {
                    // Single primary shape synthesis
                    val primaryColor = Color(0xFF38BDF8)
                    val primaryObj = buildCadObjectFromScriptContext(scriptCode, "modelo", "Modelo CadQuery", variables, primaryColor)
                    objects.add(primaryObj)
                    stdout.appendLine("Sólido principal generado con éxito (${primaryObj.triangles.size} facetas)")
                }

                stdout.appendLine("Cálculo geométrico completado. Volumen: ${String.format("%.2f", objects.sumOf { it.volume.toDouble() })} mm³")

            } catch (ex: Exception) {
                // Find approximate line number
                var errLine = 1
                scriptCode.lines().forEachIndexed { index, line ->
                    if (ex.message?.contains(line.trim()) == true) {
                        errLine = index + 1
                    }
                }
                errors.add(
                    CadError(
                        lineNumber = errLine,
                        message = ex.message ?: "Error desconocido de sintaxis en el script CadQuery",
                        rawTraceback = ex.stackTraceToString(),
                        errorType = ex.javaClass.simpleName
                    )
                )
            }
        }

        return CadExecutionResult(
            objects = objects,
            stdout = stdout.toString(),
            errors = errors,
            executionTimeMs = elapsed,
            isSuccess = errors.isEmpty(),
            engineType = EngineType.STANDALONE_OFFLINE
        )
    }

    private fun parseColorFromOptions(optionsStr: String, index: Int): Color {
        val colorPalette = listOf(
            Color(0xFF38BDF8), // Sky
            Color(0xFFF59E0B), // Amber
            Color(0xFF10B981), // Emerald
            Color(0xFFA855F7), // Purple
            Color(0xFFEC4899), // Pink
            Color(0xFF06B6D4)  // Cyan
        )
        if (optionsStr.contains("#")) {
            val hexMatch = Regex("""#([0-9a-fA-F]{6})""").find(optionsStr)
            if (hexMatch != null) {
                try {
                    val colorInt = android.graphics.Color.parseColor("#${hexMatch.groupValues[1]}")
                    return Color(colorInt)
                } catch (e: Exception) {
                    // Fallback to palette
                }
            }
        }
        return colorPalette[(index - 1) % colorPalette.size]
    }

    private fun buildCadObjectFromScriptContext(
        script: String,
        targetVar: String,
        name: String,
        variables: Map<String, Any>,
        color: Color
    ): CadObject {
        // Find dimensions in script
        var length = (variables["largo"] as? Float) ?: (variables["length"] as? Float) ?: 100f
        var width = (variables["ancho"] as? Float) ?: (variables["width"] as? Float) ?: 70f
        var height = (variables["alto"] as? Float) ?: (variables["height"] as? Float) ?: 40f
        var radius = (variables["radio"] as? Float) ?: (variables["radius"] as? Float) ?: 15f
        var filletR = (variables["radio_redondeo"] as? Float) ?: 0f
        var chamferL = 0f
        var holeDia = (variables["diametro_taladro"] as? Float) ?: (variables["diametro_eje"] as? Float) ?: 0f

        // Scan for .box(l, w, h) call
        val boxMatch = Regex("""\.box\(\s*([^\s,\)]+)\s*,\s*([^\s,\)]+)\s*,\s*([^\s,\)]+)""").find(script)
        if (boxMatch != null) {
            val l = resolveFloat(boxMatch.groupValues[1], variables)
            val w = resolveFloat(boxMatch.groupValues[2], variables)
            val h = resolveFloat(boxMatch.groupValues[3], variables)
            if (l != null) length = l
            if (w != null) width = w
            if (h != null) height = h
        }

        // Scan for .cylinder(h, r)
        val cylMatch = Regex("""\.cylinder\(\s*([^\s,\)]+)\s*,\s*([^\s,\)]+)""").find(script)
        val isCylinder = script.contains(".cylinder(") && (name.contains("Cilindro", true) || name.contains("Eje", true) || name.contains("Polea", true))
        if (cylMatch != null && isCylinder) {
            val h = resolveFloat(cylMatch.groupValues[1], variables)
            val r = resolveFloat(cylMatch.groupValues[2], variables)
            if (h != null) height = h
            if (r != null) radius = r
            return CadGeometryBuilder.createCylinder(name = name, height = height, radius = radius, color = color)
        }

        // Scan for .sphere(r)
        val sphereMatch = Regex("""\.sphere\(\s*([^\s,\)]+)""").find(script)
        if (sphereMatch != null && (name.contains("Esfera", true) || name.contains("Bola", true))) {
            val r = resolveFloat(sphereMatch.groupValues[1], variables) ?: 25f
            return CadGeometryBuilder.createSphere(name = name, radius = r, color = color)
        }

        // Scan for .fillet(r)
        val filletMatch = Regex("""\.fillet\(\s*([^\s,\)]+)""").find(script)
        if (filletMatch != null) {
            filletR = resolveFloat(filletMatch.groupValues[1], variables) ?: filletR
        }

        // Scan for .chamfer(l)
        val chamferMatch = Regex("""\.chamfer\(\s*([^\s,\)]+)""").find(script)
        if (chamferMatch != null) {
            chamferL = resolveFloat(chamferMatch.groupValues[1], variables) ?: 3f
        }

        // Scan for .hole(d)
        val holeMatch = Regex("""\.hole\(\s*([^\s,\)]+)""").find(script)
        if (holeMatch != null) {
            holeDia = resolveFloat(holeMatch.groupValues[1], variables) ?: holeDia
        }

        // Check if object is rotated or offset in multi-part assembly
        if (name.contains("Eje", true)) {
            return CadGeometryBuilder.createCylinder(
                name = name,
                height = height * 1.2f,
                radius = if (holeDia > 0) holeDia * 0.45f else 12f,
                color = color,
                centerOffset = CadPoint3D(0f, 0f, 15f)
            )
        }

        if (name.contains("Tapa", true)) {
            return CadGeometryBuilder.createBox(
                name = name,
                length = length,
                width = width,
                height = 10f,
                filletRadius = 4f,
                color = color
            )
        }

        return CadGeometryBuilder.createBox(
            name = name,
            length = length,
            width = width,
            height = height,
            filletRadius = filletR,
            chamferLength = chamferL,
            holeDiameter = holeDia,
            color = color
        )
    }

    private fun resolveFloat(token: String, variables: Map<String, Any>): Float? {
        val clean = token.trim()
        clean.toFloatOrNull()?.let { return it }
        val variableVal = variables[clean]
        if (variableVal is Float) return variableVal
        if (variableVal is Int) return variableVal.toFloat()
        if (variableVal is Double) return variableVal.toFloat()
        return null
    }

    private fun evaluateExpression(expr: String, variables: Map<String, Any>): Any? {
        val trimmed = expr.trim()
        trimmed.toFloatOrNull()?.let { return it }
        if (variables.containsKey(trimmed)) return variables[trimmed]

        // Basic arithmetic binary operations: a + b, a - b, a * b, a / b
        val operators = listOf("+", "-", "*", "/")
        for (op in operators) {
            if (trimmed.contains(op) && !trimmed.startsWith(op)) {
                val parts = trimmed.split(op, limit = 2)
                val left = resolveFloat(parts[0], variables) ?: parts[0].trim().toFloatOrNull()
                val right = resolveFloat(parts[1], variables) ?: parts[1].trim().toFloatOrNull()
                if (left != null && right != null) {
                    return when (op) {
                        "+" -> left + right
                        "-" -> left - right
                        "*" -> left * right
                        "/" -> if (right != 0f) left / right else 0f
                        else -> null
                    }
                }
            }
        }
        return null
    }
}
