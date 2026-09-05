package com.efajtahamid.weblab.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.efajtahamid.weblab.ui.theme.AccentCyan
import com.efajtahamid.weblab.ui.theme.AccentGreen
import com.efajtahamid.weblab.ui.theme.EditorTextStyle
import com.efajtahamid.weblab.ui.theme.TextSecondary

private val toolbarSymbols = listOf("<", ">", "{", "}", "[", "]", "(", ")", "/", "=", ";", ":", "\"", "'", "_")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeEditorScreen(
    viewModel: CodeEditorViewModel,
    relativePath: String,
    onBack: () -> Unit
) {
    LaunchedEffect(relativePath) { viewModel.open(relativePath) }
    DisposableEffect(Unit) { onDispose { viewModel.saveNow() } }

    val state by viewModel.uiState.collectAsState()
    // Simple undo/redo history — a bounded stack of previous text snapshots.
    val undoStack = remember { mutableStateListOf<String>() }
    val redoStack = remember { mutableStateListOf<String>() }
    var lastTrackedText by remember { mutableStateOf(state.content.text) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.relativePath.substringAfterLast('/'), style = MaterialTheme.typography.titleMedium)
                        SaveStateLabel(state.saveState)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = {
                        if (undoStack.isNotEmpty()) {
                            redoStack.add(state.content.text)
                            val previous = undoStack.removeAt(undoStack.lastIndex)
                            viewModel.onContentChange(TextFieldValue(previous, androidx.compose.ui.text.TextRange(previous.length)))
                            lastTrackedText = previous
                        }
                    }) { Icon(Icons.Filled.Undo, contentDescription = "Undo") }
                    IconButton(onClick = {
                        if (redoStack.isNotEmpty()) {
                            undoStack.add(state.content.text)
                            val next = redoStack.removeAt(redoStack.lastIndex)
                            viewModel.onContentChange(TextFieldValue(next, androidx.compose.ui.text.TextRange(next.length)))
                            lastTrackedText = next
                        }
                    }) { Icon(Icons.Filled.Redo, contentDescription = "Redo") }
                }
            )
        },
        bottomBar = {
            CodingToolbar(
                onSymbol = { viewModel.insertAtCursor(it) },
                onTab = { viewModel.insertAtCursor("    ") }
            )
        }
    ) { padding ->
        when (state.saveState) {
            SaveState.TOO_LARGE -> Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.errorMessage ?: "This file is too large to safely edit in WebLab.")
            }
            else -> {
                Row(modifier = Modifier.padding(padding).fillMaxSize()) {
                    LineNumberGutter(text = state.content.text)
                    BasicTextField(
                        value = state.content,
                        onValueChange = { newValue ->
                            if (newValue.text != lastTrackedText) {
                                undoStack.add(lastTrackedText)
                                if (undoStack.size > 100) undoStack.removeAt(0)
                                redoStack.clear()
                                lastTrackedText = newValue.text
                            }
                            viewModel.onContentChange(newValue)
                        },
                        textStyle = EditorTextStyle.copy(color = MaterialTheme.colorScheme.onBackground),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(AccentCyan),
                        visualTransformation = remember(state.language) { SyntaxVisualTransformation(state.language) },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .horizontalScroll(rememberScrollState())
                    )
                }
            }
        }
    }
}

@Composable
private fun SaveStateLabel(state: SaveState) {
    val (text, color) = when (state) {
        SaveState.IDLE -> "" to TextSecondary
        SaveState.PENDING -> "Editing…" to TextSecondary
        SaveState.SAVING -> "Saving…" to TextSecondary
        SaveState.SAVED -> "Saved" to AccentGreen
        SaveState.ERROR -> "Save failed" to MaterialTheme.colorScheme.error
        SaveState.TOO_LARGE -> "" to TextSecondary
    }
    if (text.isNotEmpty()) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
private fun LineNumberGutter(text: String) {
    val lineCount = remember(text) { text.count { it == '\n' } + 1 }
    Column(
        modifier = Modifier
            .background(Color(0xFF0D1119))
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .widthIn(min = 32.dp)
    ) {
        for (i in 1..lineCount) {
            Text(
                "$i",
                style = EditorTextStyle,
                color = TextSecondary
            )
        }
    }
}

private class SyntaxVisualTransformation(
    private val language: com.efajtahamid.weblab.data.model.EditorLanguage
) : VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): TransformedText {
        val highlighted = SyntaxHighlighter.highlight(text.text, language)
        return TransformedText(highlighted, OffsetMapping.Identity)
    }
}

@Composable
private fun CodingToolbar(onSymbol: (String) -> Unit, onTab: () -> Unit) {
    Surface(tonalElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onTab) { Text("TAB", style = EditorTextStyle) }
            toolbarSymbols.forEach { symbol ->
                TextButton(onClick = { onSymbol(symbol) }) {
                    Text(symbol, style = EditorTextStyle)
                }
            }
        }
    }
}
