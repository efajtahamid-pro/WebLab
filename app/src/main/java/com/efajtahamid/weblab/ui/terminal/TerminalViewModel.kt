package com.efajtahamid.weblab.ui.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.efajtahamid.weblab.data.model.OutputStreamKind
import com.efajtahamid.weblab.runtime.NodeRuntimeManager
import com.efajtahamid.weblab.runtime.TerminalEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

data class TerminalLine(val text: String, val kind: LineKind)
enum class LineKind { COMMAND, OUTPUT, ERROR, SYSTEM }

data class TerminalUiState(
    val lines: List<TerminalLine> = emptyList(),
    val cwd: String = "",
    val history: List<String> = emptyList(),
    val historyIndex: Int = -1
)

class TerminalViewModel(
    private val engine: TerminalEngine,
    private val runtimeManager: NodeRuntimeManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(TerminalUiState())
    val uiState: StateFlow<TerminalUiState> = _uiState

    init {
        viewModelScope.launch {
            runtimeManager.output.collect { (_, line) ->
                val kind = when (line.stream) {
                    OutputStreamKind.STDOUT -> LineKind.OUTPUT
                    OutputStreamKind.STDERR -> LineKind.ERROR
                    OutputStreamKind.SYSTEM -> LineKind.SYSTEM
                }
                appendLine(TerminalLine(line.text, kind))
            }
        }
    }

    fun submit(command: String) {
        if (command.isBlank()) return
        appendLine(TerminalLine("$ $command", LineKind.COMMAND))
        val newHistory = _uiState.value.history + command
        _uiState.value = _uiState.value.copy(history = newHistory, historyIndex = newHistory.size)

        viewModelScope.launch {
            when (val outcome = engine.execute(command)) {
                is TerminalEngine.CommandOutcome.Immediate -> {
                    if (outcome.text.isNotEmpty()) {
                        appendLine(TerminalLine(outcome.text, if (outcome.isError) LineKind.ERROR else LineKind.OUTPUT))
                    }
                    _uiState.value = _uiState.value.copy(cwd = engine.currentRelativeDir)
                }
                is TerminalEngine.CommandOutcome.LaunchedProcess -> {
                    // Output streams in asynchronously via runtimeManager.output above.
                }
                TerminalEngine.CommandOutcome.Cleared -> {
                    _uiState.value = _uiState.value.copy(lines = emptyList())
                }
            }
        }
    }

    fun historyUp(): String? {
        val state = _uiState.value
        if (state.history.isEmpty()) return null
        val newIndex = (state.historyIndex - 1).coerceAtLeast(0)
        _uiState.value = state.copy(historyIndex = newIndex)
        return state.history.getOrNull(newIndex)
    }

    fun historyDown(): String? {
        val state = _uiState.value
        val newIndex = (state.historyIndex + 1).coerceAtMost(state.history.size)
        _uiState.value = state.copy(historyIndex = newIndex)
        return state.history.getOrNull(newIndex)
    }

    private fun appendLine(line: TerminalLine) {
        _uiState.value = _uiState.value.copy(lines = _uiState.value.lines + line)
    }

    companion object {
        fun factory(engine: TerminalEngine, runtimeManager: NodeRuntimeManager) = viewModelFactory {
            initializer { TerminalViewModel(engine, runtimeManager) }
        }
    }
}
