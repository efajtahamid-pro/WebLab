package com.efajtahamid.weblab.ui.preview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.efajtahamid.weblab.data.model.ProjectType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

enum class ViewportMode { MOBILE, TABLET, DESKTOP }

data class PreviewUiState(
    val viewport: ViewportMode = ViewportMode.MOBILE,
    val reloadToken: Int = 0,
    val serverRunning: Boolean = false,
    val serverPort: Int? = null
)

/**
 * Resolves the correct preview target for a project:
 *  - Static sites load index.html directly from disk into the WebView.
 *  - Node/Vite projects need a running local dev server; until the Node runtime
 *    (Phase 4 of the spec) is wired in, WebLab reports that state honestly
 *    instead of faking a server response.
 */
class PreviewViewModel(
    private val projectRoot: File,
    private val projectType: ProjectType
) : ViewModel() {

    private val _uiState = MutableStateFlow(PreviewUiState())
    val uiState: StateFlow<PreviewUiState> = _uiState

    fun setViewport(mode: ViewportMode) {
        _uiState.value = _uiState.value.copy(viewport = mode)
    }

    fun reload() {
        _uiState.value = _uiState.value.copy(reloadToken = _uiState.value.reloadToken + 1)
    }

    /** Null means "no static entry point found" — the UI shows a clear message rather than a blank WebView. */
    fun staticEntryPointUrl(): String? {
        val index = File(projectRoot, "index.html")
        return if (index.exists()) "file://${index.absolutePath}" else null
    }

    fun requiresLocalServer(): Boolean = projectType != ProjectType.STATIC_SITE

    fun devServerUrl(): String? {
        val port = _uiState.value.serverPort ?: return null
        return "http://127.0.0.1:$port"
    }

    companion object {
        fun factory(projectRoot: File, projectType: ProjectType) = viewModelFactory {
            initializer { PreviewViewModel(projectRoot, projectType) }
        }
    }
}
