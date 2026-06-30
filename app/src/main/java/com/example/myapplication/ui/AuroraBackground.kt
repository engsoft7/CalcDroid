package com.example.myapplication.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.myapplication.ui.theme.GlowIndigo
import com.example.myapplication.ui.theme.GlowMagenta
import com.example.myapplication.ui.theme.GlowTeal
import com.example.myapplication.ui.theme.GlowViolet
import com.example.myapplication.ui.theme.NightBottom
import com.example.myapplication.ui.theme.NightMid
import com.example.myapplication.ui.theme.NightTop
import kotlin.math.sin

/**
 * A slow-drifting "aurora" backdrop: a deep night gradient with a few soft,
 * blurred colour blooms that gently float to give the screen depth and life
 * without distracting from the calculator itself.
 */
@Composable
fun AuroraBackground(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "aurora")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 26000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "aurora-phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(NightTop, NightMid, NightBottom))
            )
    ) {
        val w = size.width
        val h = size.height

        fun bloom(color: Color, cx: Float, cy: Float, radius: Float, alpha: Float) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)),
                    center = Offset(cx, cy),
                    radius = radius
                ),
                radius = radius,
                center = Offset(cx, cy)
            )
        }

        val driftX1 = sin(t.toDouble()).toFloat()
        val driftY1 = sin(t.toDouble() * 0.8 + 1.0).toFloat()
        val driftX2 = sin(t.toDouble() * 0.6 + 2.0).toFloat()
        val driftY2 = sin(t.toDouble() * 0.9 + 0.5).toFloat()

        bloom(GlowViolet, w * 0.20f + driftX1 * 30f, h * 0.12f + driftY1 * 24f, w * 0.55f, 0.30f)
        bloom(GlowIndigo, w * 0.85f + driftX2 * 26f, h * 0.22f + driftY2 * 20f, w * 0.50f, 0.24f)
        bloom(GlowTeal, w * 0.10f - driftX2 * 22f, h * 0.78f - driftY1 * 18f, w * 0.45f, 0.18f)
        bloom(GlowMagenta, w * 0.90f - driftX1 * 18f, h * 0.85f + driftY2 * 22f, w * 0.50f, 0.16f)
    }
}
