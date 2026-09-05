package com.efajtahamid.weblab.ui.explorer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.efajtahamid.weblab.data.model.FileNode
import com.efajtahamid.weblab.data.repository.FileRepository
import com.efajtahamid.weblab.data.repository.ProjectResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

data class FileExplorerUiState(
    val root: FileNode? = null,
    val expandedPaths: Set<String> = emptySet(),
    val errorMessage: String? = null,
    val pendingAction: PendingAction? = null,
    val searchQuery: String = "",
    val searchResults: List<FileRepository.SearchMatch> = emptyList()
)

sealed class PendingAction {
    data class NewFile(val relativeDir: String) : PendingAction()
    data class NewFolder(val relativeDir: String) : PendingAction()
    data class Rename(val relativePath: String, val currentName: String) : PendingAction()
}

class FileExplorerViewModel(
    private val repository: FileRepository,
    private val projectRoot: File
) : ViewModel() {

    private val _uiState = MutableStateFlow(FileExplorerUiState())
    val uiState: StateFlow<FileExplorerUiState> = _uiState

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(root = repository.readTree())
        }
    }

    fun toggleExpanded(path: String) {
        val expanded = _uiState.value.expandedPaths
        _uiState.value = _uiState.value.copy(
            expandedPaths = if (path in expanded) expanded - path else expanded + path
        )
    }

    fun requestNewFile(relativeDir: String) {
        _uiState.value = _uiState.value.copy(pendingAction = PendingAction.NewFile(relativeDir))
    }

    fun requestNewFolder(relativeDir: String) {
        _uiState.value = _uiState.value.copy(pendingAction = PendingAction.NewFolder(relativeDir))
    }

    fun requestRename(relativePath: String, currentName: String) {
        _uiState.value = _uiState.value.copy(pendingAction = PendingAction.Rename(relativePath, currentName))
    }

    fun dismissPendingAction() {
        _uiState.value = _uiState.value.copy(pendingAction = null, errorMessage = null)
    }

    fun confirmNewFile(relativeDir: String, name: String) = act { repository.createFile(relativeDir, name) }
    fun confirmNewFolder(relativeDir: String, name: String) = act { repository.createFolder(relativeDir, name) }
    fun confirmRename(relativePath: String, newName: String) = act { repository.rename(relativePath, newName) }

    fun delete(relativePath: String) {
        viewModelScope.launch {
            when (val result = repository.delete(relativePath)) {
                is ProjectResult.Failure -> _uiState.value = _uiState.value.copy(errorMessage = result.message)
                is ProjectResult.Success -> refresh()
            }
        }
    }

    fun duplicate(relativePath: String) {
        viewModelScope.launch {
            when (val result = repository.duplicate(relativePath)) {
                is ProjectResult.Failure -> _uiState.value = _uiState.value.copy(errorMessage = result.message)
                is ProjectResult.Success -> refresh()
            }
        }
    }

    fun search(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        viewModelScope.launch {
            val results = repository.search(query)
            _uiState.value = _uiState.value.copy(searchResults = results)
        }
    }

    private fun act(block: suspend () -> ProjectResult<*>) {
        viewModelScope.launch {
            when (val result = block()) {
                is ProjectResult.Failure -> _uiState.value = _uiState.value.copy(errorMessage = result.message)
                is ProjectResult.Success -> {
                    _uiState.value = _uiState.value.copy(pendingAction = null)
                    refresh()
                }
            }
        }
    }

    companion object {
        fun factory(repository: FileRepository, projectRoot: File) = viewModelFactory {
            initializer { FileExplorerViewModel(repository, projectRoot) }
        }
    }
}
