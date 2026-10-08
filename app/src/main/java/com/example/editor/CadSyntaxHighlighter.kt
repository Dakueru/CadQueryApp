package com.example.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

object CadSyntaxHighlighter {

    // Theme Colors for Syntax
    val COLOR_KEYWORD = Color(0xFFF43F5E) // Vibrant rose/red for Python keywords
    val COLOR_CADQUERY_CLASS = Color(0xFF38BDF8) // Cyan for CadQuery classes (cq, Workplane)
    val COLOR_CADQUERY_METHOD = Color(0xFF67E8F9) // Light cyan for CadQuery methods (box, fillet, edges...)
    val COLOR_NUMBER = Color(0xFFA5B4FC) // Indigo/lavender for numbers
    val COLOR_STRING = Color(0xFF86EFAC) // Mint green for standard strings
    val COLOR_COMMENT = Color(0xFF64748B) // Slate gray for comments
    val COLOR_OPERATOR = Color(0xFFFBBF24) // Amber for operators
    val COLOR_SELECTOR_TOKEN = Color(0xFFFACC15) // Bright yellow for CadQuery selectors (|Z, >Y, <X...)
    val COLOR_SELECTOR_OP = Color(0xFF38BDF8) // Blue/cyan for selector logic operators (and, or, not)
    val COLOR_BUILTIN = Color(0xFFC084FC) // Purple for builtins (print, range, len...)

    private val PYTHON_KEYWORDS = setOf(
        "import", "from", "def", "return", "class", "if", "else", "elif",
        "for", "in", "while", "try", "except", "finally", "with", "as",
        "True", "False", "None", "pass", "break", "continue", "lambda",
        "global", "nonlocal", "raise", "yield", "assert", "del", "async", "await"
    )

    private val PYTHON_BUILTINS = setOf(
        "print", "range", "len", "enumerate", "zip", "list", "dict", "set",
        "int", "float", "str", "bool", "min", "max", "sum", "abs", "round"
    )

    private val CADQUERY_METHODS = setOf(
        "Workplane", "box", "cylinder", "sphere", "cone", "wedge",
        "fillet", "chamfer", "hole", "cboreHole", "cskHole",
        "edges", "faces", "vertices", "wires", "shells", "solids",
        "extrude", "revolve", "twistExtrude", "loft", "sweep",
        "cut", "union", "intersect", "shell", "offset2D",
        "circle", "rect", "polygon", "polyline", "spline", "slot2D",
        "transformed", "workplane", "mirror", "rotate", "translate",
        "polarArray", "rarray", "each", "eachpoint", "pushPoints",
        "cutThruAll", "cutBlind", "val", "vals", "all", "first", "last",
        "show_object", "export"
    )

    // CadQuery selector patterns: |X, |Y, |Z, >X, <X, >Y, <Y, >Z, <Z, #X, #Y, #Z, +X, -X, %CIRCLE, %LINE, etc.
    private val SELECTOR_PATTERN = Regex(
        """(\|([XYZxyz]|plane)|[><][XYZxyz]|#[XYZxyz]|[+-][XYZxyz]|%(CIRCLE|LINE|ARC|ELLIPSE)|[><][+-]?[0-9]+(?:\.[0-9]+)?)"""
    )

    /**
     * Highlights code and returns an AnnotatedString with syntax spans
     */
    fun highlight(code: String): AnnotatedString {
        val builder = AnnotatedString.Builder(code)
        val length = code.length
        var index = 0

        while (index < length) {
            val char = code[index]

            // 1. Comments: # to end of line
            if (char == '#') {
                val start = index
                while (index < length && code[index] != '\n') {
                    index++
                }
                builder.addStyle(
                    SpanStyle(color = COLOR_COMMENT, fontStyle = FontStyle.Italic),
                    start,
                    index
                )
                continue
            }

            // 2. String literals: "..." or '...' or """..."""
            if (char == '"' || char == '\'') {
                val quoteChar = char
                val isTriple = index + 2 < length && code[index + 1] == quoteChar && code[index + 2] == quoteChar
                val start = index

                if (isTriple) {
                    index += 3
                    while (index + 2 < length && !(code[index] == quoteChar && code[index + 1] == quoteChar && code[index + 2] == quoteChar)) {
                        index++
                    }
                    index = minOf(length, index + 3)
                } else {
                    index++
                    while (index < length && code[index] != quoteChar && code[index] != '\n') {
                        if (code[index] == '\\' && index + 1 < length) {
                            index += 2
                        } else {
                            index++
                        }
                    }
                    if (index < length && code[index] == quoteChar) index++
                }

                val strContent = code.substring(start, index)
                // Base string style
                builder.addStyle(SpanStyle(color = COLOR_STRING), start, index)

                // SPECIFIC CADQUERY SELECTOR HIGHLIGHTING INSIDE STRINGS!
                // e.g. .edges("|Z and >Y") -> |Z (yellow), and (blue), >Y (yellow)
                highlightCadQuerySelectorString(builder, strContent, start)
                continue
            }

            // 3. Numbers
            if (char.isDigit() || (char == '.' && index + 1 < length && code[index + 1].isDigit())) {
                val start = index
                while (index < length && (code[index].isDigit() || code[index] == '.' || code[index] == 'e' || code[index] == 'E' || code[index] == 'f')) {
                    index++
                }
                builder.addStyle(SpanStyle(color = COLOR_NUMBER, fontWeight = FontWeight.Medium), start, index)
                continue
            }

            // 4. Identifiers, keywords, CadQuery functions
            if (char.isLetter() || char == '_') {
                val start = index
                while (index < length && (code[index].isLetterOrDigit() || code[index] == '_')) {
                    index++
                }
                val word = code.substring(start, index)

                when {
                    word == "cq" || word == "cadquery" -> {
                        builder.addStyle(SpanStyle(color = COLOR_CADQUERY_CLASS, fontWeight = FontWeight.Bold), start, index)
                    }
                    PYTHON_KEYWORDS.contains(word) -> {
                        builder.addStyle(SpanStyle(color = COLOR_KEYWORD, fontWeight = FontWeight.SemiBold), start, index)
                    }
                    PYTHON_BUILTINS.contains(word) -> {
                        builder.addStyle(SpanStyle(color = COLOR_BUILTIN), start, index)
                    }
                    CADQUERY_METHODS.contains(word) -> {
                        builder.addStyle(SpanStyle(color = COLOR_CADQUERY_METHOD, fontWeight = FontWeight.Bold), start, index)
                    }
                    // Function calls: word followed by '('
                    index < length && code[index] == '(' -> {
                        builder.addStyle(SpanStyle(color = COLOR_CADQUERY_METHOD), start, index)
                    }
                }
                continue
            }

            // 5. Operators
            if (char in setOf('=', '+', '-', '*', '/', '%', '<', '>', '!', '&', '|', '^', '~')) {
                builder.addStyle(SpanStyle(color = COLOR_OPERATOR), index, index + 1)
            }

            index++
        }

        return builder.toAnnotatedString()
    }

    /**
     * Detailed sub-token parsing for CadQuery selectors inside string quotes
     * Example: "|Z and >Y" or ">X or <X" or "|Z" or "%CIRCLE"
     */
    private fun highlightCadQuerySelectorString(builder: AnnotatedString.Builder, strWithQuotes: String, baseOffset: Int) {
        val quoteTrimmed = strWithQuotes.trim('"', '\'')
        if (quoteTrimmed.isEmpty()) return

        // Check if string contains any typical CadQuery selector cues
        val isSelectorCandidate = quoteTrimmed.contains("|") ||
                quoteTrimmed.contains(">") ||
                quoteTrimmed.contains("<") ||
                quoteTrimmed.contains("%") ||
                quoteTrimmed.contains("#") ||
                quoteTrimmed.matches(Regex("""^(XY|YZ|XZ|front|back|top|bottom|left|right)$"""))

        if (!isSelectorCandidate) return

        val innerStartOffset = baseOffset + (if (strWithQuotes.startsWith("\"\"\"") || strWithQuotes.startsWith("'''")) 3 else 1)

        // Split tokens preserving delimiters (operators: and, or, not)
        val tokens = quoteTrimmed.split(Regex("""(?<=\s)|(?=\s)"""))
        var cursor = 0

        for (token in tokens) {
            val trimmedToken = token.trim()
            val tokenStart = innerStartOffset + cursor
            val tokenEnd = tokenStart + token.length

            if (trimmedToken.equals("and", true) || trimmedToken.equals("or", true) || trimmedToken.equals("not", true)) {
                // Selector operator -> Blue
                builder.addStyle(
                    SpanStyle(color = COLOR_SELECTOR_OP, fontWeight = FontWeight.Bold),
                    tokenStart,
                    tokenEnd
                )
            } else if (SELECTOR_PATTERN.containsMatchIn(trimmedToken) || trimmedToken in setOf("XY", "YZ", "XZ")) {
                // Selector token (|Z, >Y, <X, etc.) -> Bright Yellow
                builder.addStyle(
                    SpanStyle(color = COLOR_SELECTOR_TOKEN, fontWeight = FontWeight.Bold),
                    tokenStart,
                    tokenEnd
                )
            }

            cursor += token.length
        }
    }
}
