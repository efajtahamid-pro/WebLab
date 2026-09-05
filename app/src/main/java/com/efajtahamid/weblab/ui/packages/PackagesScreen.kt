package com.efajtahamid.weblab.ui.packages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackagesScreen(viewModel: PackagesViewModel) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Packages") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = viewModel::openInstallDialog,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Install Package") }
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            if (!state.runtimeReady) {
                item { RuntimeUnavailableNotice() }
            }
            if (state.scripts.isNotEmpty()) {
                item { Text("Scripts", style = MaterialTheme.typography.titleMedium) }
                items(state.scripts) { script ->
                    ListItem(
                        headlineContent = { Text(script) },
                        trailingContent = {
                            TextButton(onClick = { viewModel.runScript(script) }) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Run")
                            }
                        }
                    )
                }
                item { Divider(modifier = Modifier.padding(vertical = 8.dp)) }
            }

            item { Text("Dependencies", style = MaterialTheme.typography.titleMedium) }
            if (state.dependencies.isEmpty()) {
                item { Text("No dependencies installed yet.", style = MaterialTheme.typography.bodyMedium) }
            }
            items(state.dependencies) { pkg ->
                PackageRow(pkg.name, pkg.version) { viewModel.uninstallPackage(pkg.name) }
            }

            if (state.devDependencies.isNotEmpty()) {
                item { Text("Dev Dependencies", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
                items(state.devDependencies) { pkg ->
                    PackageRow(pkg.name, pkg.version) { viewModel.uninstallPackage(pkg.name) }
                }
            }

            item { Spacer(Modifier.height(72.dp)) }
        }
    }

    if (state.isInstallDialogOpen) {
        InstallPackageDialog(onDismiss = viewModel::dismissInstallDialog, onInstall = viewModel::installPackage)
    }
}

@Composable
private fun RuntimeUnavailableNotice() {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Node runtime not installed", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "npm commands need the bundled Node.js runtime asset. See Settings > Runtime for details.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun PackageRow(name: String, version: String, onRemove: () -> Unit) {
    ListItem(
        headlineContent = { Text(name) },
        supportingContent = { Text(version) },
        trailingContent = {
            IconButton(onClick = onRemove) { Icon(Icons.Filled.Delete, contentDescription = "Uninstall $name") }
        }
    )
}

@Composable
private fun InstallPackageDialog(onDismiss: () -> Unit, onInstall: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Install Package") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("express") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onInstall(name) }) { Text("Install") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
