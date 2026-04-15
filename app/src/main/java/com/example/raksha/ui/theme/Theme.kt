package com.example.raksha.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = ColorPrimary,
    onPrimary = ColorTextInverse,
    primaryContainer = ColorPrimarySubtle,
    onPrimaryContainer = ColorPrimary,
    secondary = ColorSafe,
    onSecondary = ColorTextInverse,
    error = ColorDanger,
    onError = ColorTextPrimary,
    errorContainer = ColorDangerSubtle,
    background = ColorBackground,
    onBackground = ColorTextPrimary,
    surface = ColorSurface,
    onSurface = ColorTextPrimary,
    surfaceVariant = ColorSurfaceElevated,
    onSurfaceVariant = ColorTextSecondary,
    outline = ColorBorder
)

@Composable
fun RakshaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
