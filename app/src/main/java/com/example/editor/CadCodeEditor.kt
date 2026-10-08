package com.example.editor

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.WrapText
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CadCodeEditor(
    modifier: Modifier = Modifier,
    code: String,
    onCodeChange: (String) -> Unit,
    highlightLine: Int? = null,
    onNavigateToLine: ((Int) -> Unit)? = null
) {
    var textFieldValue by remember(code) {
        mutableStateOf(
            TextFieldValue(
                text = code,
                selection = TextRange(code.length)
            )
        )
    }

    // Keep history for undo / redo
    val undoStack = remember { mutableStateListOf<String>() }
    val redoStack = remember { mutableStateListOf<String>() }
    var isUndoRedoOperation by remember { mutableStateOf(false) }

    // Editor settings
    var fontSize by remember { mutableFloatStateOf(13.5f) }
    var wordWrap by remember { mutableStateOf(false) }
    var showSearchReplace by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }

    // Autocomplete state
    val cursorIndex = textFieldValue.selection.start
    val (ghostSuffix, suggestions) = remember(textFieldValue.text, cursorIndex) {
        CadAutocomplete.getSuggestions(textFieldValue.text, cursorIndex)
    }

    // Current line index calculation
    val lines = textFieldValue.text.lines()
    val activeLineIndex = remember(cursorIndex, textFieldValue.text) {
        val sub = textFieldValue.text.take(cursorIndex)
        sub.count { it == '\n' }
    }

    // Jump to highlighted line if requested
    LaunchedEffect(highlightLine) {
        if (highlightLine != null && highlightLine > 0 && highlightLine <= lines.size) {
            var charCount = 0
            for (i in 0 until highlightLine - 1) {
                charCount += lines[i].length + 1
            }
            textFieldValue = textFieldValue.copy(selection = TextRange(charCount, charCount + lines[highlightLine - 1].length))
        }
    }

    fun applyTextChange(newVal: TextFieldValue) {
        if (!isUndoRedoOperation && newVal.text != textFieldValue.text) {
            if (undoStack.size > 50) undoStack.removeAt(0)
            undoStack.add(textFieldValue.text)
            redoStack.clear()
        }
        textFieldValue = newVal
        onCodeChange(newVal.text)
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            isUndoRedoOperation = true
            val prev = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(textFieldValue.text)
            textFieldValue = TextFieldValue(prev, selection = TextRange(prev.length))
            onCodeChange(prev)
            isUndoRedoOperation = false
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            isUndoRedoOperation = true
            val next = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(textFieldValue.text)
            textFieldValue = TextFieldValue(next, selection = TextRange(next.length))
            onCodeChange(next)
            isUndoRedoOperation = false
        }
    }

    fun insertSymbol(symbol: String) {
        val currentText = textFieldValue.text
        val start = textFieldValue.selection.min
        val end = textFieldValue.selection.max

        // Auto-close pairs for quotes and brackets
        val (inserted, offset) = when (symbol) {
            "(" -> Pair("()", 1)
            "[" -> Pair("[]", 1)
            "{" -> Pair("{}", 1)
            "\"" -> Pair("\"\"", 1)
            "'" -> Pair("''", 1)
            "Tab" -> Pair("    ", 4)
            else -> Pair(symbol, symbol.length)
        }

        val newText = currentText.substring(0, start) + inserted + currentText.substring(end)
        applyTextChange(
            TextFieldValue(
                text = newText,
                selection = TextRange(start + offset)
            )
        )
    }

    fun acceptGhostSuggestion() {
        if (!ghostSuffix.isNullOrEmpty()) {
            insertSymbol(ghostSuffix)
        }
    }

    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        // --- 1. Quick accessory symbol toolbar above keyboard ---
        Surface(
            color = Color(0xFF161B22),
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left toolbar icons: Undo, Redo, Search, Font sizing
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { undo() },
                            enabled = undoStack.isNotEmpty(),
                            modifier = Modifier.size(32.dp).testTag("editor_undo_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Undo,
                                contentDescription = "Deshacer",
                                tint = if (undoStack.isNotEmpty()) Color(0xFFE2E8F0) else Color(0xFF475569),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = { redo() },
                            enabled = redoStack.isNotEmpty(),
                            modifier = Modifier.size(32.dp).testTag("editor_redo_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Redo,
                                contentDescription = "Rehacer",
                                tint = if (redoStack.isNotEmpty()) Color(0xFFE2E8F0) else Color(0xFF475569),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = { showSearchReplace = !showSearchReplace },
                            modifier = Modifier.size(32.dp).testTag("editor_search_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Buscar y reemplazar",
                                tint = if (showSearchReplace) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = { wordWrap = !wordWrap },
                            modifier = Modifier.size(32.dp).testTag("editor_wrap_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.WrapText,
                                contentDescription = "Ajuste de línea",
                                tint = if (wordWrap) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Ghost suggestion chip if available
                    if (!ghostSuffix.isNullOrEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1E293B))
                                .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(6.dp))
                                .clickable { acceptGhostSuggestion() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("ghost_suggestion_chip")
                        ) {
                            Text(
                                text = "Completar: ",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = ghostSuffix,
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Tab ⇥",
                                color = Color(0xFF64748B),
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Font zoom controls
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (fontSize > 10f) fontSize -= 1.5f },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomOut,
                                contentDescription = "Reducir letra",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Text(
                            text = "${fontSize.toInt()}pt",
                            color = Color(0xFF64748B),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        IconButton(
                            onClick = { if (fontSize < 24f) fontSize += 1.5f },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomIn,
                                contentDescription = "Aumentar letra",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                // Quick symbol keyboard bar
                val symbols = listOf("Tab", "(", ")", "[", "]", "\"", "'", ".", "=", "|", "<", ">", ":", "_", "#", ",", "+", "-", "*", "/")
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(symbols) { sym ->
                        Surface(
                            color = Color(0xFF21262D),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { insertSymbol(sym) }
                        ) {
                            Text(
                                text = sym,
                                color = if (sym == "|" || sym == "<" || sym == ">") Color(0xFFFACC15) else Color(0xFFE2E8F0),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }

        // --- 2. Search & Replace Bar ---
        if (showSearchReplace) {
            Surface(
                color = Color(0xFF1E2430),
                modifier = Modifier.fillMaxWidth().padding(4.dp),
                shape = RoundedCornerShape(6.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFF0F172A), RoundedCornerShape(4.dp))
                                .padding(6.dp),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text("Buscar...", color = Color.Gray, fontSize = 12.sp)
                                }
                                innerTextField()
                            }
                        )
                        Spacer(Modifier.width(6.dp))
                        BasicTextField(
                            value = replaceQuery,
                            onValueChange = { replaceQuery = it },
                            textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFF0F172A), RoundedCornerShape(4.dp))
                                .padding(6.dp),
                            decorationBox = { innerTextField ->
                                if (replaceQuery.isEmpty()) {
                                    Text("Reemplazar...", color = Color.Gray, fontSize = 12.sp)
                                }
                                innerTextField()
                            }
                        )
                        Spacer(Modifier.width(6.dp))
                        IconButton(
                            onClick = {
                                if (searchQuery.isNotEmpty()) {
                                    val replaced = textFieldValue.text.replace(searchQuery, replaceQuery)
                                    applyTextChange(TextFieldValue(replaced))
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.FindReplace, "Reemplazar todo", tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = { showSearchReplace = false },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, "Cerrar", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // --- 3. Autocomplete suggestion candidates bar ---
        if (suggestions.isNotEmpty() && suggestions.size > 1) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF161B22))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(suggestions.take(5)) { sug ->
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                insertSymbol(sug.ghostSuffix)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = sug.completionText,
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = sug.docSummary.take(24),
                                color = Color(0xFF64748B),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        // --- 4. Main Editor Area: Line Numbers + Text Field ---
        Row(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .verticalScroll(verticalScrollState)
        ) {
            // Line numbers column
            Column(
                modifier = Modifier
                    .background(Color(0xFF090D13))
                    .padding(horizontal = 6.dp, vertical = 8.dp)
                    .width(42.dp),
                horizontalAlignment = Alignment.End
            ) {
                lines.forEachIndexed { idx, _ ->
                    val lineNum = idx + 1
                    val isActive = idx == activeLineIndex
                    val isError = highlightLine == lineNum

                    Text(
                        text = "$lineNum",
                        color = when {
                            isError -> Color(0xFFEF4444)
                            isActive -> Color(0xFF38BDF8)
                            else -> Color(0xFF475569)
                        },
                        fontSize = fontSize.sp,
                        fontWeight = if (isActive || isError) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = (fontSize * 1.35f).sp
                    )
                }
            }

            // Code Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(vertical = 8.dp, horizontal = 8.dp)
                    .then(if (!wordWrap) Modifier.horizontalScroll(horizontalScrollState) else Modifier)
            ) {
                val annotatedCode = remember(textFieldValue.text) {
                    CadSyntaxHighlighter.highlight(textFieldValue.text)
                }

                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { newVal ->
                        // Handle automatic indent on new line
                        if (newVal.text.length == textFieldValue.text.length + 1 &&
                            newVal.selection.start > 0 &&
                            newVal.text[newVal.selection.start - 1] == '\n'
                        ) {
                            val lineIndex = textFieldValue.text.take(textFieldValue.selection.start).count { it == '\n' }
                            val prevLine = lines.getOrNull(lineIndex) ?: ""
                            val leadingSpaces = prevLine.takeWhile { it == ' ' }.length
                            val extraIndent = if (prevLine.trimEnd().endsWith(":")) 4 else 0
                            val indent = " ".repeat(leadingSpaces + extraIndent)

                            if (indent.isNotEmpty()) {
                                val insertedPos = newVal.selection.start
                                val textWithIndent = newVal.text.substring(0, insertedPos) + indent + newVal.text.substring(insertedPos)
                                applyTextChange(
                                    TextFieldValue(
                                        text = textWithIndent,
                                        selection = TextRange(insertedPos + indent.length)
                                    )
                                )
                                return@BasicTextField
                            }
                        }
                        applyTextChange(newVal)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cad_code_editor_field")
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_TAB &&
                                keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN
                            ) {
                                if (!ghostSuffix.isNullOrEmpty()) {
                                    acceptGhostSuggestion()
                                    true
                                } else {
                                    insertSymbol("    ")
                                    true
                                }
                            } else {
                                false
                            }
                        },
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = fontSize.sp,
                        lineHeight = (fontSize * 1.35f).sp,
                        color = Color(0xFFE2E8F0)
                    ),
                    cursorBrush = SolidColor(Color(0xFF38BDF8)),
                    visualTransformation = {
                        // Display annotated syntax highlighting
                        androidx.compose.ui.text.input.TransformedText(
                            text = annotatedCode,
                            offsetMapping = androidx.compose.ui.text.input.OffsetMapping.Identity
                        )
                    }
                )
            }
        }
    }
}
