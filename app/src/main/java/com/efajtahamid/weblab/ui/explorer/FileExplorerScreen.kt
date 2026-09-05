package com.efajtahamid.weblab.ui.explorer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.efajtahamid.weblab.data.model.FileNode
import com.efajtahamid.weblab.ui.theme.AccentCyan
import com.efajtahamid.weblab.ui.theme.AccentPurple
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileExplorerScreen(
    projectRoot: File,
    viewModel: FileExplorerViewModel,
    onOpenFile: (relativePath: String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var searchActive by remember { mutableStateOf(false) }
    var contextMenuTarget by remember { mutableStateOf<FileNode?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Files") },
                actions = {
                    IconButton(onClick = { searchActive = !searchActive }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search project")
                    }
                    IconButton(onClick = { viewModel.requestNewFile("") }) {
                        Icon(Icons.Filled.NoteAdd, contentDescription = "New file")
                    }
                    IconButton(onClick = { viewModel.requestNewFolder("") }) {
                        Icon(Icons.Filled.CreateNewFolder, contentDescription = "New folder")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (searchActive) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::search,
                    placeholder = { Text("Search filenames and file contents") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(12.dp)
                )
                if (state.searchQuery.isNotBlank()) {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        item {
                            Text(
                                "${state.searchResults.size} matches",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                        items(state.searchResults) { match ->
                            ListItem(
                                headlineContent = { Text(match.relativePath + (match.lineNumber?.let { ":$it" } ?: "")) },
                                supportingContent = match.lineText?.let { { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
                                modifier = Modifier.clickable { onOpenFile(match.relativePath) }
                            )
                        }
                    }
                    return@Column
                }
            }

            val root = state.root
            if (root == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(root.children, key = { it.path }) { child ->
                        FileTreeItem(
                            node = child,
                            projectRoot = projectRoot,
                            depth = 0,
                            expandedPaths = state.expandedPaths,
                            onToggle = viewModel::toggleExpanded,
                            onOpenFile = onOpenFile,
                            onLongPress = { contextMenuTarget = it }
                        )
                    }
                }
            }
        }
    }

    contextMenuTarget?.let { node ->
        val relPath = node.file.relativeTo(projectRoot).path
        FileContextMenu(
            node = node,
            onDismiss = { contextMenuTarget = null },
            onRename = { viewModel.requestRename(relPath, node.name); contextMenuTarget = null },
            onDelete = { viewModel.delete(relPath); contextMenuTarget = null },
            onDuplicate = { viewModel.duplicate(relPath); contextMenuTarget = null },
            onNewFileInside = { viewModel.requestNewFile(relPath); contextMenuTarget = null },
            onNewFolderInside = { viewModel.requestNewFolder(relPath); contextMenuTarget = null }
        )
    }

    state.pendingAction?.let { action ->
        NameInputDialog(
            action = action,
            errorMessage = state.errorMessage,
            onDismiss = viewModel::dismissPendingAction,
            onConfirm = { name ->
                when (action) {
                    is PendingAction.NewFile -> viewModel.confirmNewFile(action.relativeDir, name)
                    is PendingAction.NewFolder -> viewModel.confirmNewFolder(action.relativeDir, name)
                    is PendingAction.Rename -> viewModel.confirmRename(action.relativePath, name)
                }
            }
        )
    }
}

@Composable
private fun FileTreeItem(
    node: FileNode,
    projectRoot: File,
    depth: Int,
    expandedPaths: Set<String>,
    onToggle: (String) -> Unit,
    onOpenFile: (String) -> Unit,
    onLongPress: (FileNode) -> Unit
) {
    val isExpanded = node.path in expandedPaths
    val relativePath = node.file.relativeTo(projectRoot).path

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (node.isDirectory) onToggle(node.path) else onOpenFile(relativePath)
            }
            .padding(start = (16 + depth * 16).dp, top = 10.dp, bottom = 10.dp, end = 16.dp)
    ) {
        Icon(
            imageVector = when {
                node.isDirectory && isExpanded -> Icons.Filled.FolderOpen
                node.isDirectory -> Icons.Filled.Folder
                else -> Icons.Filled.InsertDriveFile
            },
            contentDescription = null,
            tint = if (node.isDirectory) AccentPurple else AccentCyan,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(node.name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        IconButton(onClick = { onLongPress(node) }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Options for ${node.name}")
        }
    }

    if (node.isDirectory && isExpanded) {
        node.children.forEach { child ->
            FileTreeItem(child, projectRoot, depth + 1, expandedPaths, onToggle, onOpenFile, onLongPress)
        }
    }
}

@Composable
private fun FileContextMenu(
    node: FileNode,
    onDismiss: () -> Unit,
    onRename: (File) -> Unit,
    onDelete: (File) -> Unit,
    onDuplicate: (File) -> Unit,
    onNewFileInside: (File) -> Unit,
    onNewFolderInside: (File) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(node.name) },
        text = {
            Column {
                if (node.isDirectory) {
                    TextButton(onClick = { onNewFileInside(node.file) }) { Text("New file here") }
                    TextButton(onClick = { onNewFolderInside(node.file) }) { Text("New folder here") }
                }
                TextButton(onClick = { onRename(node.file) }) { Text("Rename") }
                TextButton(onClick = { onDuplicate(node.file) }) { Text("Duplicate") }
                TextButton(onClick = { onDelete(node.file) }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun NameInputDialog(
    action: PendingAction,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val title = when (action) {
        is PendingAction.NewFile -> "New File"
        is PendingAction.NewFolder -> "New Folder"
        is PendingAction.Rename -> "Rename"
    }
    var name by remember { mutableStateOf(if (action is PendingAction.Rename) action.currentName else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    isError = errorMessage != null,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage != null) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onConfirm(name) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
