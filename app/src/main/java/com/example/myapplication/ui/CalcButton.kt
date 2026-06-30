package com.example.myapplication.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.EqualsEnd
import com.example.myapplication.ui.theme.EqualsStart
import com.example.myapplication.ui.theme.GlassDigit
import com.example.myapplication.ui.theme.GlassDigitHi
import com.example.myapplication.ui.theme.GlassFunction
import com.example.myapplication.ui.theme.GlassFunctionHi
import com.example.myapplication.ui.theme.GlassStroke
import com.example.myapplication.ui.theme.OperatorEnd
import com.example.myapplication.ui.theme.OperatorStart

enum class KeyKind { DIGIT, FUNCTION, OPERATOR, EQUALS }

private val ButtonShape = RoundedCornerShape(percent = 32)

/**
 * A tactile, gradient-filled key. Scales down and emits a haptic tick on press,
 * brightens briefly, and (on operator/equals keys) casts a coloured glow.
 */
@Composable
fun CalcButton(
    kind: KeyKind,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "press-scale"
    )

    LaunchedEffect(pressed) {
        if (pressed) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    val background = when (kind) {
        KeyKind.DIGIT -> Brush.verticalGradient(listOf(GlassDigitHi, GlassDigit))
        KeyKind.FUNCTION -> Brush.verticalGradient(listOf(GlassFunctionHi, GlassFunction))
        KeyKind.OPERATOR -> Brush.linearGradient(listOf(OperatorStart, OperatorEnd))
        KeyKind.EQUALS -> Brush.linearGradient(listOf(EqualsStart, EqualsEnd))
    }
    val glowColor = when (kind) {
        KeyKind.OPERATOR -> OperatorStart
        KeyKind.EQUALS -> EqualsStart
        else -> Color.Transparent
    }
    val glow = if (kind == KeyKind.EQUALS) 22.dp else if (kind == KeyKind.OPERATOR) 12.dp else 0.dp

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = if (pressed) glow / 2 else glow,
                shape = ButtonShape,
                ambientColor = glowColor,
                spotColor = glowColor,
                clip = false
            )
            .clip(ButtonShape)
            .background(background)
            .then(
                if (kind == KeyKind.DIGIT || kind == KeyKind.FUNCTION) {
                    Modifier.border(1.dp, GlassStroke, ButtonShape)
                } else Modifier
            )
            // brief brightening overlay while held
            .drawWithContent {
                drawContent()
                if (pressed) drawRect(Color.White.copy(alpha = 0.12f))
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center,
        content = { content() }
    )
}

/** A crisp, vector backspace glyph drawn with Canvas (no icon dependency). */
@Composable
fun BackspaceIcon(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.drawWithContent {
            val w = size.width
            val h = size.height
            val stroke = (minOf(w, h) * 0.07f).coerceAtLeast(2f)
            val body = Path().apply {
                moveTo(w * 0.04f, h * 0.5f)
                lineTo(w * 0.34f, h * 0.18f)
                lineTo(w * 0.94f, h * 0.18f)
                lineTo(w * 0.94f, h * 0.82f)
                lineTo(w * 0.34f, h * 0.82f)
                close()
            }
            drawPath(
                body,
                color,
                style = Stroke(width = stroke, join = StrokeJoin.Round, cap = StrokeCap.Round)
            )
            drawLine(
                color, Offset(w * 0.54f, h * 0.38f), Offset(w * 0.80f, h * 0.62f),
                strokeWidth = stroke, cap = StrokeCap.Round
            )
            drawLine(
                color, Offset(w * 0.80f, h * 0.38f), Offset(w * 0.54f, h * 0.62f),
                strokeWidth = stroke, cap = StrokeCap.Round
            )
        }.fillMaxSize()
    )
}

/** A small clock glyph used for the history toggle. */
@Composable
fun HistoryIcon(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.drawWithContent {
            val r = minOf(size.width, size.height) * 0.42f
            val c = Offset(size.width / 2f, size.height / 2f)
            val stroke = r * 0.16f
            drawCircle(color, radius = r, center = c, style = Stroke(width = stroke))
            drawLine(color, c, c + Offset(0f, -r * 0.55f), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(color, c, c + Offset(r * 0.46f, 0f), strokeWidth = stroke, cap = StrokeCap.Round)
        }.fillMaxSize()
    )
}
