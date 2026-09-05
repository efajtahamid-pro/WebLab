package com.efajtahamid.weblab.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val WebLabDarkColors = darkColorScheme(
    background = CharcoalBackground,
    surface = CharcoalSurface,
    surfaceVariant = CharcoalSurfaceRaised,
    primary = AccentCyan,
    secondary = AccentPurple,
    error = AccentRed,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onPrimary = CharcoalBackground,
    onSecondary = CharcoalBackground,
    outline = CharcoalBorder
)

private val WebLabLightColors = lightColorScheme(
    primary = AccentCyan,
    secondary = AccentPurple,
    error = AccentRed
)

/**
 * WebLab defaults to dark regardless of system theme (spec section 26), but a
 * Settings toggle (see SettingsScreen) allows Light / Dark / System.
 */
@Composable
fun WebLabTheme(
    darkTheme: Boolean = true,
    dynamicSystemTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val useDark = if (dynamicSystemTheme) isSystemInDarkTheme() else darkTheme
    val colors = if (useDark) WebLabDarkColors else WebLabLightColors

    MaterialTheme(
        colorScheme = colors,
        typography = WebLabTypography,
        content = content
    )
}
