package com.example.editor

data class AutocompleteSuggestion(
    val displayText: String,
    val completionText: String,
    val ghostSuffix: String,
    val docSummary: String,
    val category: String = "CadQuery"
)

object CadAutocomplete {

    private val CADQUERY_METHOD_CATALOG = listOf(
        AutocompleteItem("box", "box(length, width, height)", "Prisma rectangular tridimensional"),
        AutocompleteItem("cylinder", "cylinder(height, radius)", "Cilindro recto tridimensional"),
        AutocompleteItem("sphere", "sphere(radius)", "Esfera tridimensional centrada"),
        AutocompleteItem("fillet", "fillet(radius)", "Redondeo de aristas seleccionadas"),
        AutocompleteItem("chamfer", "chamfer(length)", "Chaflán de aristas seleccionadas"),
        AutocompleteItem("hole", "hole(diameter, depth=None)", "Taladro pasante o ciego"),
        AutocompleteItem("edges", "edges(selector=None)", "Selector de aristas (|Z, >Y...)"),
        AutocompleteItem("faces", "faces(selector=None)", "Selector de caras (>Z, <Z...)"),
        AutocompleteItem("vertices", "vertices(selector=None)", "Selector de vértices"),
        AutocompleteItem("wires", "wires()", "Selector de contornos alámbricos"),
        AutocompleteItem("extrude", "extrude(distance)", "Extrusión lineal de boceto 2D"),
        AutocompleteItem("revolve", "revolve(angleDegrees=360)", "Revolución de boceto 2D"),
        AutocompleteItem("cut", "cut(toCut)", "Resta booleana de sólidos"),
        AutocompleteItem("union", "union(toUnion)", "Unión booleana de sólidos"),
        AutocompleteItem("intersect", "intersect(toIntersect)", "Intersección booleana"),
        AutocompleteItem("shell", "shell(thickness)", "Vaciado de cáscara con espesor"),
        AutocompleteItem("circle", "circle(radius)", "Boceto 2D de círculo"),
        AutocompleteItem("rect", "rect(xLen, yLen)", "Boceto 2D de rectángulo"),
        AutocompleteItem("polygon", "polygon(nSides, diameter)", "Polígono regular 2D"),
        AutocompleteItem("slot2D", "slot2D(length, diameter)", "Ranura colisa 2D"),
        AutocompleteItem("workplane", "workplane(offset=0)", "Nuevo plano de trabajo desplazado"),
        AutocompleteItem("transformed", "transformed(offset, rotate)", "Transformación espacial"),
        AutocompleteItem("polarArray", "polarArray(radius, startAngle, angle, count)", "Matriz polar"),
        AutocompleteItem("rarray", "rarray(xSpacing, ySpacing, xCount, yCount)", "Matriz rectangular"),
        AutocompleteItem("show_object", "show_object(shape, name=\"Pieza\")", "Registrar pieza en visor 3D")
    )

    private data class AutocompleteItem(val name: String, val signature: String, val doc: String)

    /**
     * Extracts token at cursor position and user defined variables from script
     */
    fun getSuggestions(code: String, cursorIndex: Int): Pair<String?, List<AutocompleteSuggestion>> {
        if (code.isEmpty() || cursorIndex < 0 || cursorIndex > code.length) {
            return Pair(null, emptyList())
        }

        val textBeforeCursor = code.substring(0, cursorIndex)
        // Find token being typed
        var tokenStart = cursorIndex
        while (tokenStart > 0 && (code[tokenStart - 1].isLetterOrDigit() || code[tokenStart - 1] == '_')) {
            tokenStart--
        }

        val currentWord = code.substring(tokenStart, cursorIndex)
        if (currentWord.isEmpty()) {
            return Pair(null, emptyList())
        }

        // Check if after dot (e.g. .bo or cq.Workplane("XY").bo)
        val isMethodCall = tokenStart > 0 && code[tokenStart - 1] == '.'

        // Extract user variables from code
        val userVariables = extractUserVariables(code)

        val matches = mutableListOf<AutocompleteSuggestion>()

        // 1. Match CadQuery methods
        for (item in CADQUERY_METHOD_CATALOG) {
            if (item.name.startsWith(currentWord, ignoreCase = true) && !item.name.equals(currentWord, ignoreCase = true)) {
                val ghost = item.name.substring(currentWord.length)
                matches.add(
                    AutocompleteSuggestion(
                        displayText = item.signature,
                        completionText = item.name,
                        ghostSuffix = ghost,
                        docSummary = item.doc,
                        category = "CadQuery"
                    )
                )
            }
        }

        // 2. Match user variables (if not directly after a dot)
        if (!isMethodCall) {
            for (v in userVariables) {
                if (v.startsWith(currentWord, ignoreCase = true) && !v.equals(currentWord, ignoreCase = true)) {
                    val ghost = v.substring(currentWord.length)
                    matches.add(
                        AutocompleteSuggestion(
                            displayText = v,
                            completionText = v,
                            ghostSuffix = ghost,
                            docSummary = "Variable de usuario",
                            category = "Variable"
                        )
                    )
                }
            }
        }

        // Primary ghost suggestion (most probable completion)
        val bestGhost = matches.firstOrNull()?.ghostSuffix

        return Pair(bestGhost, matches)
    }

    private fun extractUserVariables(code: String): List<String> {
        val vars = mutableListOf<String>()
        val regex = Regex("""^\s*([a-zA-Z_][a-zA-Z0-9_]*)\s*=""", RegexOption.MULTILINE)
        for (match in regex.findAll(code)) {
            val name = match.groupValues[1]
            if (name != "cq" && !vars.contains(name)) {
                vars.add(name)
            }
        }
        return vars
    }
}
