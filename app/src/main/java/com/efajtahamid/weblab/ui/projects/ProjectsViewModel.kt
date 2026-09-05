package com.efajtahamid.weblab.ui.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efajtahamid.weblab.data.db.ProjectEntity
import com.efajtahamid.weblab.data.model.ProjectType
import com.efajtahamid.weblab.data.repository.ProjectRepository
import com.efajtahamid.weblab.data.repository.ProjectResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

data class ProjectsUiState(
    val projects: List<ProjectEntity> = emptyList(),
    val isCreateDialogOpen: Boolean = false,
    val createError: String? = null,
    val isCreating: Boolean = false
)

class ProjectsViewModel(private val repository: ProjectRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ProjectsUiState())
    val uiState: StateFlow<ProjectsUiState> = _uiState

    init {
        viewModelScope.launch {
            repository.observeProjects().collect { list ->
                _uiState.value = _uiState.value.copy(projects = list)
            }
        }
    }

    fun openCreateDialog() {
        _uiState.value = _uiState.value.copy(isCreateDialogOpen = true, createError = null)
    }

    fun dismissCreateDialog() {
        _uiState.value = _uiState.value.copy(isCreateDialogOpen = false, createError = null)
    }

    fun createProject(name: String, type: ProjectType, onCreated: (ProjectEntity) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreating = true, createError = null)
            when (val result = repository.createProject(name, type)) {
                is ProjectResult.Success -> {
                    _uiState.value = _uiState.value.copy(isCreating = false, isCreateDialogOpen = false)
                    onCreated(result.value)
                }
                is ProjectResult.Failure -> {
                    _uiState.value = _uiState.value.copy(isCreating = false, createError = result.message)
                }
            }
        }
    }

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch { repository.deleteProject(project) }
    }

    companion object {
        fun factory(repository: ProjectRepository) = viewModelFactory {
            initializer { ProjectsViewModel(repository) }
        }
    }
}
