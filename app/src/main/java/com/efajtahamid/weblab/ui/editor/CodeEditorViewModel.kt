package com.efajtahamid.weblab.ui.editor

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.efajtahamid.weblab.data.model.EditorLanguage
import com.efajtahamid.weblab.data.repository.FileRepository
import com.efajtahamid.weblab.data.repository.ProjectResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class SaveState { IDLE, PENDING, SAVING, SAVED, ERROR, TOO_LARGE }

data class CodeEditorUiState(
    val relativePath: String = "",
    val language: EditorLanguage = EditorLanguage.PLAIN,
    val content: TextFieldValue = TextFieldValue(""),
    val saveState: SaveState = SaveState.IDLE,
    val errorMessage: String? = null
)

/** Debounce window before an edit is written to disk (spec section 24: avoid per-keystroke writes). */
private const val AUTOSAVE_DEBOUNCE_MS = 600L

class CodeEditorViewModel(private val repository: FileRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CodeEditorUiState())
    val uiState: StateFlow<CodeEditorUiState> = _uiState

    private var autosaveJob: Job? = null

    fun open(relativePath: String) {
        val language = EditorLanguage.fromFileName(relativePath)
        _uiState.value = CodeEditorUiState(relativePath = relativePath, language = language, saveState = SaveState.IDLE)
        viewModelScope.launch {
            when (val result = repository.readTextFile(relativePath)) {
                is ProjectResult.Success -> {
                    _uiState.value = _uiState.value.copy(content = TextFieldValue(result.value))
                }
                is ProjectResult.Failure -> {
                    val tooLarge = result.message.contains("too large", ignoreCase = true)
                    _uiState.value = _uiState.value.copy(
                        saveState = if (tooLarge) SaveState.TOO_LARGE else SaveState.ERROR,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun onContentChange(newValue: TextFieldValue) {
        _uiState.value = _uiState.value.copy(content = newValue, saveState = SaveState.PENDING)
        scheduleAutosave()
    }

    /** Insert text (from the coding toolbar) at the current cursor position. */
    fun insertAtCursor(insert: String) {
        val current = _uiState.value.content
        val selection = current.selection
        val newText = current.text.replaceRange(selection.start, selection.end, insert)
        val newCursor = selection.start + insert.length
        onContentChange(TextFieldValue(newText, androidx.compose.ui.text.TextRange(newCursor)))
    }

    fun saveNow() {
        autosaveJob?.cancel()
        persist()
    }

    private fun scheduleAutosave() {
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            delay(AUTOSAVE_DEBOUNCE_MS)
            persist()
        }
    }

    private fun persist() {
        val path = _uiState.value.relativePath
        val text = _uiState.value.content.text
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(saveState = SaveState.SAVING)
            when (val result = repository.writeTextFile(path, text)) {
                is ProjectResult.Success -> _uiState.value = _uiState.value.copy(saveState = SaveState.SAVED)
                is ProjectResult.Failure -> _uiState.value = _uiState.value.copy(
                    saveState = SaveState.ERROR,
                    errorMessage = result.message
                )
            }
        }
    }

    companion object {
        fun factory(repository: FileRepository) = viewModelFactory {
            initializer { CodeEditorViewModel(repository) }
        }
    }
}
