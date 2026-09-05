package com.efajtahamid.weblab.ui.terminal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.efajtahamid.weblab.ui.theme.AccentCyan
import com.efajtahamid.weblab.ui.theme.AccentGreen
import com.efajtahamid.weblab.ui.theme.AccentRed
import com.efajtahamid.weblab.ui.theme.EditorTextStyle
import com.efajtahamid.weblab.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(viewModel: TerminalViewModel) {
    val state by viewModel.uiState.collectAsState()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.lines.size) {
        if (state.lines.isNotEmpty()) listState.animateScrollToItem(state.lines.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terminal") },
                actions = {
                    Text(
                        "/${state.cwd}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.historyUp()?.let { input = it } }) {
                        Icon(Icons.Filled.ArrowUpward, contentDescription = "Previous command")
                    }
                    IconButton(onClick = { viewModel.historyDown()?.let { input = it } ?: run { input = "" } }) {
                        Icon(Icons.Filled.ArrowDownward, contentDescription = "Next command")
                    }
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = { Text("$ node --version", style = EditorTextStyle) },
                        textStyle = EditorTextStyle,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            viewModel.submit(input); input = ""
                        }),
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                    )
                    IconButton(onClick = { viewModel.submit(input); input = "" }) {
                        Icon(Icons.Filled.Send, contentDescription = "Run", tint = AccentCyan)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(state.lines) { line ->
                val color = when (line.kind) {
                    LineKind.COMMAND -> AccentCyan
                    LineKind.OUTPUT -> MaterialTheme.colorScheme.onBackground
                    LineKind.ERROR -> AccentRed
                    LineKind.SYSTEM -> AccentGreen
                }
                Text(line.text, style = EditorTextStyle, color = color)
            }
        }
    }
}
