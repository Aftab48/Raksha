package com.raksha.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val RakshaDarkColorScheme = darkColorScheme(
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
fun RakshaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RakshaDarkColorScheme,
        typography = RakshaTypography,
        shapes = RakshaShapes,
        content = content
    )
}
