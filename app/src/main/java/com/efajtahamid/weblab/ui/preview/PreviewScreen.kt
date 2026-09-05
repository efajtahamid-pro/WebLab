package com.efajtahamid.weblab.ui.preview

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(viewModel: PreviewViewModel) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Preview") },
                actions = {
                    ViewportSelector(state.viewport, viewModel::setViewport)
                    IconButton(onClick = { viewModel.reload() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Reload")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
            when {
                viewModel.requiresLocalServer() && !state.serverRunning -> {
                    NoServerRunningMessage()
                }
                else -> {
                    val url = if (viewModel.requiresLocalServer()) viewModel.devServerUrl() else viewModel.staticEntryPointUrl()
                    if (url == null) {
                        Text("No index.html found in this project yet.")
                    } else {
                        WebPreview(url = url, reloadToken = state.reloadToken, viewport = state.viewport)
                    }
                }
            }
        }
    }
}

@Composable
private fun NoServerRunningMessage() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
        Text("Development Server", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "This project needs a running Node.js server to preview. Start it from the Terminal tab, " +
                "then come back here to see it live.",
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ViewportSelector(current: ViewportMode, onSelect: (ViewportMode) -> Unit) {
    Row {
        ViewportMode.values().forEach { mode ->
            FilterChip(
                selected = current == mode,
                onClick = { onSelect(mode) },
                label = { Text(mode.name.take(1) + mode.name.drop(1).lowercase()) },
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebPreview(url: String, reloadToken: Int, viewport: ViewportMode) {
    // JavaScript is enabled because it's required to preview real web projects,
    // but per spec section 22 the WebView never exposes Android APIs via
    // addJavascriptInterface — it can only render the project's own sandboxed files.
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.allowContentAccess = false
                settings.allowFileAccessFromFileURLs = false
                settings.allowUniversalAccessFromFileURLs = false
                loadUrl(url)
            }
        },
        update = { webView ->
            applyViewportMetaOverride(webView, viewport)
            webView.loadUrl(url)
        }
    )
}

private fun applyViewportMetaOverride(webView: WebView, viewport: ViewportMode) {
    // Desktop/tablet modes widen the layout viewport so responsive sites render
    // as they would on a larger screen, without needing a second WebView engine.
    val widthPx = when (viewport) {
        ViewportMode.MOBILE -> null
        ViewportMode.TABLET -> 1024
        ViewportMode.DESKTOP -> 1440
    }
    webView.settings.useWideViewPort = widthPx != null
    webView.settings.loadWithOverviewMode = widthPx != null
}
