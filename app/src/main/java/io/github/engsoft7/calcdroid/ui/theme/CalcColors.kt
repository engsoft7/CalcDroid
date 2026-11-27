package io.github.engsoft7.calcdroid.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Cores da calculadora; a versão escura segue o modo escuro do sistema. */
data class CalcColors(
    val background: Color,
    val text: Color,
    val secondaryText: Color,
    val chip: Color,
    val chipText: Color,
    val card: Color,
    val numberKey: Color,
    val numberKeyText: Color,
    val grayKey: Color,
    val grayKeyText: Color,
    val functionKey: Color,
    val operatorKey: Color,
    val keyText: Color,
    val grid: Color,
    val axis: Color,
    val accent: Color,
)

private val Orange = Color(0xFFFFA500)

val LightCalcColors = CalcColors(
    background = Color(0xFFF0F0F0),
    text = Color.Black,
    secondaryText = Color(0xFF555555),
    chip = Color(0xFFDDDDDD),
    chipText = Color(0xFF555555),
    card = Color.White,
    numberKey = Color.White,
    numberKeyText = Color.Black,
    grayKey = Color(0xFFD3D3D3),
    // Texto escuro: o branco sobre este cinza quase não se lia.
    grayKeyText = Color(0xFF3A3A3A),
    functionKey = Color(0xFF4A6572),
    operatorKey = Orange,
    keyText = Color.White,
    grid = Color(0xFFEAEAEA),
    axis = Color(0xFFB5B5B5),
    accent = Orange,
)

val DarkCalcColors = CalcColors(
    background = Color(0xFF121212),
    text = Color(0xFFEDEDED),
    secondaryText = Color(0xFFAAAAAA),
    chip = Color(0xFF2C2C2C),
    chipText = Color(0xFFBBBBBB),
    card = Color(0xFF1E1E1E),
    numberKey = Color(0xFF2A2A2A),
    numberKeyText = Color.White,
    grayKey = Color(0xFF4A4A4A),
    grayKeyText = Color.White,
    functionKey = Color(0xFF37474F),
    operatorKey = Orange,
    keyText = Color.White,
    grid = Color(0xFF2E2E2E),
    axis = Color(0xFF6A6A6A),
    accent = Orange,
)

@Composable
fun calcColors(): CalcColors = if (isSystemInDarkTheme()) DarkCalcColors else LightCalcColors
