package com.example.ui

import android.app.Application
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.CadGeometryBuilder
import com.example.engine.CadQueryRemoteClient
import com.example.engine.CadScriptEvaluator
import com.example.export.CadExporter
import com.example.model.CadExecutionResult
import com.example.model.CadObject
import com.example.model.CadParameter
import com.example.model.CadScript
import com.example.model.CadTemplates
import com.example.model.EngineType
import com.example.model.ScriptSnapshot
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

enum class DisplayLayoutMode {
    SPLIT_BOTH,
    EDITOR_ONLY,
    VIEWER_ONLY
}

data class CadUiState(
    val currentScript: CadScript,
    val isDirty: Boolean = false,
    val hasPendingDraft: Boolean = false,
    val objects: List<CadObject> = emptyList(),
    val isExecuting: Boolean = false,
    val executionTimeMs: Long = 0,
    val stdout: String = "",
    val errorList: List<com.example.model.CadError> = emptyList(),
    val isStaleGeometry: Boolean = false,
    val engineType: EngineType = EngineType.STANDALONE_OFFLINE,
    val serverUrl: String = "http://10.0.2.2:5000",
    val layoutMode: DisplayLayoutMode = DisplayLayoutMode.SPLIT_BOTH,
    val isConsoleVisible: Boolean = false,
    val highlightErrorLine: Int? = null,
    val allScripts: List<CadScript> = emptyList(),
    val snapshots: List<ScriptSnapshot> = emptyList(),
    val detectedParameters: List<CadParameter> = emptyList(),
    val selectedObjectId: String? = null
)

class CadViewModel(application: Application) : AndroidViewModel(application) {

    private val remoteClient = CadQueryRemoteClient()
    private val scriptsDir: File = File(application.filesDir, "scripts").apply { if (!exists()) mkdirs() }
    private val draftsFile: File = File(application.filesDir, "current_draft.json")

    private var executionJob: Job? = null
    private var autoSaveJob: Job? = null

    private val _uiState = MutableStateFlow(
        CadUiState(
            currentScript = CadScript(
                id = UUID.randomUUID().toString(),
                name = "ejemplo_cadquery.py",
                content = CadTemplates.templates.first().code
            )
        )
    )
    val uiState: StateFlow<CadUiState> = _uiState.asStateFlow()

    init {
        loadSavedScripts()
        // Execute initial script to render initial 3D piece immediately
        executeCurrentScript()
    }

    private fun loadSavedScripts() {
        val scriptFiles = scriptsDir.listFiles { f -> f.extension == "py" } ?: emptyArray()
        val loaded = mutableListOf<CadScript>()

        if (scriptFiles.isEmpty()) {
            // Seed initial scripts from templates
            CadTemplates.templates.take(3).forEachIndexed { idx, tmpl ->
                val name = when (idx) {
                    0 -> "pieza_redondeada.py"
                    1 -> "brida_parametrica.py"
                    else -> "ensamblaje_multipieza.py"
                }
                val file = File(scriptsDir, name)
                file.writeText(tmpl.code)
                loaded.add(CadScript(id = UUID.randomUUID().toString(), name = name, content = tmpl.code, path = file.absolutePath))
            }
        } else {
            scriptFiles.forEach { f ->
                loaded.add(
                    CadScript(
                        id = UUID.randomUUID().toString(),
                        name = f.name,
                        content = f.readText(),
                        lastModified = f.lastModified(),
                        path = f.absolutePath
                    )
                )
            }
        }

        val first = loaded.firstOrNull() ?: _uiState.value.currentScript
        _uiState.value = _uiState.value.copy(
            allScripts = loaded,
            currentScript = first,
            detectedParameters = CadScriptEvaluator.extractParameters(first.content)
        )
    }

    fun onCodeChange(newCode: String) {
        val curr = _uiState.value.currentScript
        _uiState.value = _uiState.value.copy(
            currentScript = curr.copy(content = newCode),
            isDirty = true,
            detectedParameters = CadScriptEvaluator.extractParameters(newCode)
        )

        // Auto-save draft after 600ms of inactivity
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(600)
            saveDraftLocally(newCode)
        }
    }

    private fun saveDraftLocally(code: String) {
        try {
            draftsFile.writeText(code)
        } catch (e: Exception) {
            // ignore
        }
    }

    fun executeCurrentScript() {
        executionJob?.cancel()
        val script = _uiState.value.currentScript.content
        val engine = _uiState.value.engineType

        _uiState.value = _uiState.value.copy(
            isExecuting = true,
            highlightErrorLine = null
        )

        executionJob = viewModelScope.launch {
            val result: CadExecutionResult = if (engine == EngineType.REMOTE_CADQUERY) {
                remoteClient.setServerUrl(_uiState.value.serverUrl)
                remoteClient.executeScript(script)
            } else {
                CadScriptEvaluator.evaluate(script)
            }

            if (result.isSuccess) {
                // Save version snapshot
                addSnapshot(script, "Ejecución exitosa (${result.objects.size} piezas)")
                _uiState.value = _uiState.value.copy(
                    isExecuting = false,
                    objects = result.objects,
                    stdout = result.stdout,
                    errorList = emptyList(),
                    executionTimeMs = result.executionTimeMs,
                    isStaleGeometry = false,
                    isConsoleVisible = false
                )
            } else {
                // Preserve previous geometry if error occurs, and mark as stale!
                _uiState.value = _uiState.value.copy(
                    isExecuting = false,
                    stdout = result.stdout,
                    errorList = result.errors,
                    executionTimeMs = result.executionTimeMs,
                    isStaleGeometry = _uiState.value.objects.isNotEmpty(),
                    isConsoleVisible = true,
                    highlightErrorLine = result.errors.firstOrNull()?.lineNumber
                )
            }
        }
    }

    fun stopExecution() {
        executionJob?.cancel()
        _uiState.value = _uiState.value.copy(
            isExecuting = false,
            stdout = _uiState.value.stdout + "\n[Ejecución cancelada por el usuario]"
        )
    }

    fun saveCurrentScript() {
        val curr = _uiState.value.currentScript
        val file = File(scriptsDir, curr.name)
        file.writeText(curr.content)

        addSnapshot(curr.content, "Guardado manual")

        val updatedList = _uiState.value.allScripts.map {
            if (it.id == curr.id) it.copy(content = curr.content, lastModified = System.currentTimeMillis()) else it
        }

        _uiState.value = _uiState.value.copy(
            isDirty = false,
            allScripts = updatedList
        )
    }

    fun openScript(script: CadScript) {
        _uiState.value = _uiState.value.copy(
            currentScript = script,
            isDirty = false,
            detectedParameters = CadScriptEvaluator.extractParameters(script.content)
        )
        executeCurrentScript()
    }

    fun createNewScript(name: String, content: String) {
        val cleanName = if (name.endsWith(".py")) name else "$name.py"
        val file = File(scriptsDir, cleanName)
        file.writeText(content)

        val newScript = CadScript(
            id = UUID.randomUUID().toString(),
            name = cleanName,
            content = content,
            path = file.absolutePath
        )

        _uiState.value = _uiState.value.copy(
            allScripts = _uiState.value.allScripts + newScript,
            currentScript = newScript,
            isDirty = false,
            detectedParameters = CadScriptEvaluator.extractParameters(content)
        )
        executeCurrentScript()
    }

    fun renameScript(script: CadScript, newName: String) {
        val oldFile = File(scriptsDir, script.name)
        val newFile = File(scriptsDir, newName)
        if (oldFile.exists()) oldFile.renameTo(newFile)

        val updated = _uiState.value.allScripts.map {
            if (it.id == script.id) it.copy(name = newName, path = newFile.absolutePath) else it
        }
        val isCurr = _uiState.value.currentScript.id == script.id

        _uiState.value = _uiState.value.copy(
            allScripts = updated,
            currentScript = if (isCurr) _uiState.value.currentScript.copy(name = newName) else _uiState.value.currentScript
        )
    }

    fun duplicateScript(script: CadScript) {
        val copyName = script.name.removeSuffix(".py") + "_copia.py"
        createNewScript(copyName, script.content)
    }

    fun deleteScript(script: CadScript) {
        val file = File(scriptsDir, script.name)
        if (file.exists()) file.delete()

        val updated = _uiState.value.allScripts.filter { it.id != script.id }
        _uiState.value = _uiState.value.copy(allScripts = updated)

        if (_uiState.value.currentScript.id == script.id && updated.isNotEmpty()) {
            openScript(updated.first())
        }
    }

    fun applyParameters(params: Map<String, Float>) {
        var updatedCode = _uiState.value.currentScript.content
        params.forEach { (name, value) ->
            updatedCode = CadScriptEvaluator.updateParameterInCode(updatedCode, name, value)
        }
        onCodeChange(updatedCode)
        executeCurrentScript()
    }

    fun toggleObjectVisibility(id: String) {
        val updated = _uiState.value.objects.map {
            if (it.id == id) it.copy(isVisible = !it.isVisible) else it
        }
        _uiState.value = _uiState.value.copy(objects = updated)
    }

    fun setObjectColor(id: String, color: Color) {
        val updated = _uiState.value.objects.map {
            if (it.id == id) it.copy(color = color) else it
        }
        _uiState.value = _uiState.value.copy(objects = updated)
    }

    fun setObjectAlpha(id: String, alpha: Float) {
        val updated = _uiState.value.objects.map {
            if (it.id == id) it.copy(alpha = alpha) else it
        }
        _uiState.value = _uiState.value.copy(objects = updated)
    }

    fun selectObject(id: String) {
        _uiState.value = _uiState.value.copy(
            selectedObjectId = if (_uiState.value.selectedObjectId == id) null else id
        )
    }

    fun setEngineConfig(type: EngineType, url: String) {
        _uiState.value = _uiState.value.copy(engineType = type, serverUrl = url)
        remoteClient.setServerUrl(url)
    }

    fun setLayoutMode(mode: DisplayLayoutMode) {
        _uiState.value = _uiState.value.copy(layoutMode = mode)
    }

    fun toggleConsole() {
        _uiState.value = _uiState.value.copy(isConsoleVisible = !_uiState.value.isConsoleVisible)
    }

    fun clearConsole() {
        _uiState.value = _uiState.value.copy(stdout = "", errorList = emptyList())
    }

    fun navigateToLine(line: Int) {
        _uiState.value = _uiState.value.copy(highlightErrorLine = line)
    }

    private fun addSnapshot(content: String, title: String) {
        val snap = ScriptSnapshot(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            title = title,
            content = content,
            lineCount = content.lines().size
        )
        val list = _uiState.value.snapshots.takeLast(20) + snap
        _uiState.value = _uiState.value.copy(snapshots = list)
    }

    fun restoreSnapshot(snap: ScriptSnapshot) {
        onCodeChange(snap.content)
        executeCurrentScript()
    }
}
