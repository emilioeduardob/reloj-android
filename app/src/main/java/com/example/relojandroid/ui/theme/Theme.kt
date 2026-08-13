package com.example.relojandroid.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF00FF00),
    background = Color.Black,
    surface = Color.Black,
    onBackground = Color(0xFF00FF00),
    onSurface = Color(0xFF00FF00)
)

@Composable
fun RelojAndroidTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
