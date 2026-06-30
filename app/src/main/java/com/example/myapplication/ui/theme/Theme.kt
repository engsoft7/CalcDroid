package com.example.myapplication.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// CalcDroid is intentionally a single, carefully tuned dark theme so the
// aurora palette always looks its best (dynamic colour would fight the design).
private val CalcDroidColors = darkColorScheme(
    primary = OperatorStart,
    onPrimary = DisplayPrimary,
    secondary = GlowTeal,
    background = NightBottom,
    onBackground = DisplayPrimary,
    surface = GlassDigit,
    onSurface = DisplayPrimary,
    error = ErrorText,
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CalcDroidColors,
        typography = Typography,
        content = content
    )
}
