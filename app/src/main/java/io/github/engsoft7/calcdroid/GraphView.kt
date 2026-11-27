package io.github.engsoft7.calcdroid

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.engsoft7.calcdroid.ui.theme.CalcColors
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.sign

/**
 * Janela visível do gráfico. Com [yMin]/[yMax] NaN a faixa vertical é
 * automática (graphYRange); o primeiro gesto do usuário a fixa.
 */
data class Viewport(
    val xMin: Double = -10.0,
    val xMax: Double = 10.0,
    val yMin: Double = Double.NaN,
    val yMax: Double = Double.NaN,
) {
    val autoY get() = yMin.isNaN() || yMax.isNaN()
    val isDefault get() = this == Viewport()
}

private val ViewportSaver = listSaver<Viewport, Double>(
    save = { listOf(it.xMin, it.xMax, it.yMin, it.yMax) },
    restore = { Viewport(it[0], it[1], it[2], it[3]) }
)

/**
 * Aplica um gesto de pinça/arrasto: [zoom] > 1 aproxima em torno do ponto
 * ([fx], [fy]) — frações 0..1 da largura/altura — e [dxFrac]/[dyFrac] movem a
 * janela (arrastar para a direita mostra valores menores de x).
 */
fun Viewport.transformed(
    yLo: Double, yHi: Double,
    fx: Double, fy: Double,
    zoom: Double, dxFrac: Double, dyFrac: Double,
): Viewport {
    val w = xMax - xMin
    val h = yHi - yLo
    val cx = xMin + fx * w
    val cy = yHi - fy * h
    // Limita o zoom para não chegar a faixas degeneradas ou gigantes.
    val newW = (w / zoom).coerceIn(1e-6, 1e7)
    val newH = (h / zoom).coerceIn(1e-6, 1e9)
    val shiftX = -dxFrac * newW
    val shiftY = dyFrac * newH
    val left = cx - fx * newW + shiftX
    val top = cy + fy * newH + shiftY
    return Viewport(left, left + newW, top - newH, top)
}

@Composable
fun GraphView(
    plotExpression: String,
    degrees: Boolean,
    colors: CalcColors,
    decimalSep: Char,
    modifier: Modifier = Modifier,
) {
    var viewport by rememberSaveable(plotExpression, stateSaver = ViewportSaver) { mutableStateOf(Viewport()) }
    // Ponto tocado (x); o y vem da própria função.
    var tappedX by rememberSaveable(plotExpression) { mutableStateOf(Double.NaN) }

    fun f(x: Double): Double = try {
        ExpressionEvaluator.evaluate(plotExpression, x, degrees)
    } catch (e: Exception) {
        Double.NaN
    }

    val samples = remember(plotExpression, degrees, viewport.xMin, viewport.xMax) {
        if (plotExpression.isEmpty()) {
            emptyList()
        } else {
            val n = 400
            (0..n).map { i ->
                val x = viewport.xMin + i * (viewport.xMax - viewport.xMin) / n
                x to f(x)
            }
        }
    }
    val (yLo, yHi) = remember(samples, viewport) {
        if (viewport.autoY) graphYRange(samples.map { it.second }) else viewport.yMin to viewport.yMax
    }
    // Os gestos são criados uma vez; leem a faixa atual por aqui.
    val currentRange by rememberUpdatedState(yLo to yHi)
    fun label(v: Double) = axisLabel(v).replace('.', decimalSep)

    val description = stringResource(R.string.graph_desc, prettyExpression(plotExpression, decimalSep))
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(colors.card)
            .semantics { contentDescription = description }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(plotExpression) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        if (plotExpression.isEmpty()) return@detectTransformGestures
                        val w = size.width.toDouble()
                        val h = size.height.toDouble()
                        val current = viewport
                        val (lo, hi) = currentRange
                        viewport = current.transformed(
                            lo, hi,
                            fx = centroid.x / w, fy = centroid.y / h,
                            zoom = zoom.toDouble(),
                            dxFrac = pan.x / w, dyFrac = pan.y / h,
                        )
                    }
                }
                .pointerInput(plotExpression) {
                    detectTapGestures { offset ->
                        if (plotExpression.isEmpty()) return@detectTapGestures
                        tappedX = viewport.xMin + offset.x / size.width * (viewport.xMax - viewport.xMin)
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val xMin = viewport.xMin
            val xMax = viewport.xMax
            fun px(x: Double) = ((x - xMin) / (xMax - xMin) * w).toFloat()
            fun py(y: Double) = (h - (y - yLo) / (yHi - yLo) * h).toFloat()

            // Grade com passo "redondo" que acompanha o zoom.
            val stepX = niceStep(xMax - xMin)
            var gx = ceil(xMin / stepX) * stepX
            while (gx <= xMax) {
                drawLine(colors.grid, Offset(px(gx), 0f), Offset(px(gx), h))
                gx += stepX
            }
            val stepY = niceStep(yHi - yLo)
            var gy = ceil(yLo / stepY) * stepY
            while (gy <= yHi) {
                drawLine(colors.grid, Offset(0f, py(gy)), Offset(w, py(gy)))
                gy += stepY
            }
            if (xMin < 0 && xMax > 0) {
                drawLine(colors.axis, Offset(px(0.0), 0f), Offset(px(0.0), h), strokeWidth = 2f)
            }
            if (yLo < 0 && yHi > 0) {
                drawLine(colors.axis, Offset(0f, py(0.0)), Offset(w, py(0.0)), strokeWidth = 2f)
            }

            if (samples.isNotEmpty()) {
                val span = yHi - yLo
                val path = Path()
                var penDown = false
                var prevY = Double.NaN
                for ((x, y) in samples) {
                    // Levanta a "caneta" em descontinuidades: NaN, pontos muito
                    // fora da tela e saltos de assíntota (como em tan), em que o
                    // sinal troca com uma diferença maior que a altura do gráfico.
                    val inRange = y.isFinite() && y > yLo - span && y < yHi + span
                    val jump = penDown && abs(y - prevY) > span && sign(y) != sign(prevY)
                    if (inRange && !jump) {
                        if (penDown) path.lineTo(px(x), py(y)) else path.moveTo(px(x), py(y))
                        penDown = true
                    } else if (inRange) {
                        path.moveTo(px(x), py(y))
                        penDown = true
                    } else {
                        penDown = false
                    }
                    prevY = y
                }
                drawPath(
                    path = path,
                    color = colors.accent,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                if (!tappedX.isNaN()) {
                    val ty = f(tappedX)
                    drawLine(colors.axis, Offset(px(tappedX), 0f), Offset(px(tappedX), h), strokeWidth = 1.5f)
                    if (ty.isFinite()) {
                        drawCircle(colors.text, radius = 6.dp.toPx(), center = Offset(px(tappedX), py(ty)))
                        drawCircle(colors.accent, radius = 4.dp.toPx(), center = Offset(px(tappedX), py(ty)))
                    }
                }
            }
        }
        if (samples.isEmpty()) {
            Text(
                text = stringResource(R.string.graph_hint),
                modifier = Modifier.align(Alignment.Center),
                color = colors.secondaryText,
                fontSize = 15.sp
            )
        } else {
            Text(label(yHi), Modifier.align(Alignment.TopStart).padding(6.dp), colors.secondaryText, fontSize = 11.sp)
            Text(label(yLo), Modifier.align(Alignment.BottomStart).padding(start = 6.dp, bottom = 20.dp), colors.secondaryText, fontSize = 11.sp)
            Text(label(viewport.xMin), Modifier.align(Alignment.BottomStart).padding(6.dp), colors.secondaryText, fontSize = 11.sp)
            Text(
                label(viewport.xMax) + " (" + stringResource(if (degrees) R.string.unit_deg_suffix else R.string.unit_rad_suffix) + ")",
                Modifier.align(Alignment.BottomEnd).padding(6.dp),
                colors.secondaryText,
                fontSize = 11.sp
            )
            if (!tappedX.isNaN()) {
                val ty = f(tappedX)
                Text(
                    text = stringResource(
                        R.string.graph_point,
                        label(tappedX),
                        if (ty.isFinite()) prettyNumber(formatNumber(ty), decimalSep) else "—"
                    ),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.chip)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    color = colors.text,
                    fontSize = 13.sp
                )
            }
            if (!viewport.isDefault || !tappedX.isNaN()) {
                Text(
                    text = stringResource(R.string.graph_reset),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.accent)
                        .clickable(role = Role.Button) {
                            viewport = Viewport()
                            tappedX = Double.NaN
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    color = colors.keyText,
                    fontSize = 12.sp
                )
            }
        }
    }
}
