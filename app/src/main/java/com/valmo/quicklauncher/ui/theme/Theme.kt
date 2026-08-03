package com.valmo.quicklauncher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val QuickLauncherColors = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
)

@Composable
fun QuickLauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = QuickLauncherColors,
        typography = QuickLauncherTypography,
        content = content,
    )
}

