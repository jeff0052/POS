package com.developer.pos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF0B6E4F),
    secondary = Color(0xFFCE6A1C),
    background = Color(0xFFF6F4EE),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1F2933),
    onSurface = Color(0xFF1F2933)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF63C29A),
    secondary = Color(0xFFFFB36B)
)

@Composable
fun DeveloperPosTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
