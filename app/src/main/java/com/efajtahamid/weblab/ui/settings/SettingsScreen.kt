package com.efajtahamid.weblab.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.efajtahamid.weblab.BuildConfig
import com.efajtahamid.weblab.runtime.NodeRuntimeManager
import com.efajtahamid.weblab.ui.theme.AccentCyan
import com.efajtahamid.weblab.ui.theme.AccentPurple

private const val HIRE_DEVELOPER_URL = "https://efajtahamid.monster"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            item { SectionTitle("Editor") }
            item {
                SliderSetting(
                    label = "Font size (${state.editor.fontSize}sp)",
                    value = state.editor.fontSize.toFloat(),
                    range = 10f..24f,
                    onChange = { viewModel.setFontSize(it.toInt()) }
                )
            }
            item { SwitchSetting("Word wrap", state.editor.wordWrap, viewModel::setWordWrap) }
            item { SwitchSetting("Show line numbers", state.editor.showLineNumbers, viewModel::setShowLineNumbers) }
            item {
                SliderSetting(
                    label = "Tab size (${state.editor.tabSize} spaces)",
                    value = state.editor.tabSize.toFloat(),
                    range = 2f..8f,
                    onChange = { viewModel.setTabSize(it.toInt()) }
                )
            }
            item { SwitchSetting("Auto indent", state.editor.autoIndent, viewModel::setAutoIndent) }

            item { SectionTitle("Appearance") }
            item {
                SingleChoiceRow(
                    options = AppearanceMode.values().toList(),
                    selected = state.appearance,
                    labelFor = { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                    onSelect = viewModel::setAppearance
                )
            }

            item { SectionTitle("Runtime") }
            item { RuntimeStatusCard(state.runtimeStatus, state.runtimeVersion) }
            item {
                SliderSetting(
                    label = "Default port (${state.defaultPort})",
                    value = state.defaultPort.toFloat(),
                    range = 1024f..9000f,
                    onChange = { viewModel.setDefaultPort(it.toInt()) }
                )
            }

            item { SectionTitle("Preview") }
            item { SwitchSetting("Auto refresh on save", state.autoRefreshPreview, viewModel::setAutoRefreshPreview) }

            item { SectionTitle("About") }
            item { AboutSection(context) }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = AccentCyan)
}

@Composable
private fun SwitchSetting(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SliderSetting(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

@Composable
private fun <T> SingleChoiceRow(options: List<T>, selected: T, labelFor: (T) -> String, onSelect: (T) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(labelFor(option)) }
            )
        }
    }
}

@Composable
private fun RuntimeStatusCard(status: NodeRuntimeManager.RuntimeStatus, version: String?) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (status == NodeRuntimeManager.RuntimeStatus.READY) AccentPurple else MaterialTheme.colorScheme.error,
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (status == NodeRuntimeManager.RuntimeStatus.READY) "Ready" else "Not installed",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                version?.let { "Node $it" } ?: "No bundled Node.js runtime asset was found on this device.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun AboutSection(context: android.content.Context) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Code, contentDescription = null, tint = AccentCyan)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("WebLab", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Turn your Android phone into a lightweight local web-development workstation.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            Divider()
            Spacer(Modifier.height(16.dp))
            Text(
                "Built by Dewan Efaj Tahamid Rifat",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text("AI Architect & Full-Stack AI Engineer", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(HIRE_DEVELOPER_URL))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Hire Developer")
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}
