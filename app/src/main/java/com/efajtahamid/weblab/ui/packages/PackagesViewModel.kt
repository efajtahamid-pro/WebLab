package com.efajtahamid.weblab.ui.packages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.efajtahamid.weblab.runtime.NodeRuntimeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class PackageEntry(val name: String, val version: String)

data class PackagesUiState(
    val dependencies: List<PackageEntry> = emptyList(),
    val devDependencies: List<PackageEntry> = emptyList(),
    val scripts: List<String> = emptyList(),
    val runtimeReady: Boolean = false,
    val isInstallDialogOpen: Boolean = false,
    val statusMessage: String? = null
)

/**
 * Reads the project's real package.json (no fake package database, per spec
 * section 9) and dispatches real `npm install` / `npm uninstall` / `npm run`
 * commands through [NodeRuntimeManager].
 */
class PackagesViewModel(
    private val projectRoot: File,
    private val runtimeManager: NodeRuntimeManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PackagesUiState())
    val uiState: StateFlow<PackagesUiState> = _uiState

    init {
        refresh()
        viewModelScope.launch {
            val info = runtimeManager.detectRuntime()
            _uiState.value = _uiState.value.copy(runtimeReady = info.status == NodeRuntimeManager.RuntimeStatus.READY)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val packageJson = File(projectRoot, "package.json")
            if (!packageJson.exists()) {
                _uiState.value = _uiState.value.copy(dependencies = emptyList(), devDependencies = emptyList(), scripts = emptyList())
                return@launch
            }
            val parsed = withContext(Dispatchers.IO) { parsePackageJson(packageJson) }
            _uiState.value = _uiState.value.copy(
                dependencies = parsed.first,
                devDependencies = parsed.second,
                scripts = parsed.third
            )
        }
    }

    private fun parsePackageJson(file: File): Triple<List<PackageEntry>, List<PackageEntry>, List<String>> {
        val json = JSONObject(file.readText())
        fun readDeps(key: String): List<PackageEntry> {
            if (!json.has(key)) return emptyList()
            val obj = json.getJSONObject(key)
            return obj.keys().asSequence().map { PackageEntry(it, obj.getString(it)) }.toList()
        }
        val scripts = if (json.has("scripts")) json.getJSONObject("scripts").keys().asSequence().toList() else emptyList()
        return Triple(readDeps("dependencies"), readDeps("devDependencies"), scripts)
    }

    fun openInstallDialog() {
        _uiState.value = _uiState.value.copy(isInstallDialogOpen = true)
    }

    fun dismissInstallDialog() {
        _uiState.value = _uiState.value.copy(isInstallDialogOpen = false)
    }

    fun installPackage(name: String) {
        _uiState.value = _uiState.value.copy(isInstallDialogOpen = false, statusMessage = "Running npm install $name…")
        viewModelScope.launch {
            runtimeManager.startProcess(
                id = UUID.randomUUID().toString(),
                label = "npm install $name",
                command = listOf("install", name),
                workingDirectory = projectRoot,
                useNpm = true
            )
        }
    }

    fun uninstallPackage(name: String) {
        _uiState.value = _uiState.value.copy(statusMessage = "Running npm uninstall $name…")
        viewModelScope.launch {
            runtimeManager.startProcess(
                id = UUID.randomUUID().toString(),
                label = "npm uninstall $name",
                command = listOf("uninstall", name),
                workingDirectory = projectRoot,
                useNpm = true
            )
        }
    }

    fun runScript(scriptName: String) {
        _uiState.value = _uiState.value.copy(statusMessage = "Running npm run $scriptName…")
        viewModelScope.launch {
            runtimeManager.startProcess(
                id = UUID.randomUUID().toString(),
                label = "npm run $scriptName",
                command = listOf("run", scriptName),
                workingDirectory = projectRoot,
                useNpm = true
            )
        }
    }

    companion object {
        fun factory(projectRoot: File, runtimeManager: NodeRuntimeManager) = viewModelFactory {
            initializer { PackagesViewModel(projectRoot, runtimeManager) }
        }
    }
}
