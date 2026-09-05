package com.efajtahamid.weblab.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.efajtahamid.weblab.runtime.NodeRuntimeManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class AppearanceMode { DARK, LIGHT, SYSTEM }

data class EditorSettings(
    val fontSize: Int = 14,
    val wordWrap: Boolean = false,
    val showLineNumbers: Boolean = true,
    val tabSize: Int = 4,
    val autoIndent: Boolean = true
)

data class SettingsUiState(
    val editor: EditorSettings = EditorSettings(),
    val appearance: AppearanceMode = AppearanceMode.DARK,
    val runtimeStatus: NodeRuntimeManager.RuntimeStatus = NodeRuntimeManager.RuntimeStatus.NOT_INSTALLED,
    val runtimeVersion: String? = null,
    val defaultPort: Int = 3000,
    val previewViewport: String = "Mobile",
    val autoRefreshPreview: Boolean = true
)

class SettingsViewModel(private val runtimeManager: NodeRuntimeManager) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        viewModelScope.launch {
            val info = runtimeManager.detectRuntime()
            _uiState.value = _uiState.value.copy(
                runtimeStatus = info.status,
                runtimeVersion = info.version
            )
        }
    }

    fun setFontSize(size: Int) {
        _uiState.value = _uiState.value.copy(editor = _uiState.value.editor.copy(fontSize = size))
    }

    fun setWordWrap(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(editor = _uiState.value.editor.copy(wordWrap = enabled))
    }

    fun setShowLineNumbers(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(editor = _uiState.value.editor.copy(showLineNumbers = enabled))
    }

    fun setTabSize(size: Int) {
        _uiState.value = _uiState.value.copy(editor = _uiState.value.editor.copy(tabSize = size))
    }

    fun setAutoIndent(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(editor = _uiState.value.editor.copy(autoIndent = enabled))
    }

    fun setAppearance(mode: AppearanceMode) {
        _uiState.value = _uiState.value.copy(appearance = mode)
    }

    fun setDefaultPort(port: Int) {
        _uiState.value = _uiState.value.copy(defaultPort = port)
    }

    fun setAutoRefreshPreview(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(autoRefreshPreview = enabled)
    }

    companion object {
        fun factory(runtimeManager: NodeRuntimeManager) = viewModelFactory {
            initializer { SettingsViewModel(runtimeManager) }
        }
    }
}
