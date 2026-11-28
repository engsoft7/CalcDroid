package io.github.engsoft7.calcdroid

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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

// Cor da g(x): azul, legível no tema claro e no escuro, ao lado do laranja da f(x).
val SecondCurveColor = Color(0xFF3D9BE9)

/** Caminho da curva, levantando a "caneta" em descontinuidades. */
private fun curvePath(
    samples: List<Pair<Double, Double>>,
    yLo: Double, yHi: Double,
    px: (Double) -> Float, py: (Double) -> Float,
): Path {
    val span = yHi - yLo
    val path = Path()
    var penDown = false
    var prevY = Double.NaN
    for ((x, y) in samples) {
        // NaN, pontos muito fora da tela e saltos de assíntota (como em tan),
        // em que o sinal troca com uma diferença maior que a altura do gráfico.
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
    return path
}

@Composable
fun GraphView(
    plotExpression: String,
    degrees: Boolean,
    colors: CalcColors,
    decimalSep: Char,
    modifier: Modifier = Modifier,
    plotExpression2: String = "",
) {
    var viewport by rememberSaveable(plotExpression, stateSaver = ViewportSaver) { mutableStateOf(Viewport()) }
    // Ponto tocado (x); o y vem da própria função.
    var tappedX by rememberSaveable(plotExpression, plotExpression2) { mutableStateOf(Double.NaN) }
    var editingRange by rememberSaveable { mutableStateOf(false) }
    val hasF = plotExpression.isNotEmpty()
    val hasG = plotExpression2.isNotEmpty()

    fun eval(expr: String, x: Double): Double = try {
        ExpressionEvaluator.evaluate(expr, x, degrees)
    } catch (e: Exception) {
        Double.NaN
    }
    fun f(x: Double) = eval(plotExpression, x)
    fun g(x: Double) = eval(plotExpression2, x)

    fun sample(expr: String): List<Pair<Double, Double>> =
        if (expr.isEmpty()) emptyList() else {
            val n = 400
            (0..n).map { i ->
                val x = viewport.xMin + i * (viewport.xMax - viewport.xMin) / n
                x to eval(expr, x)
            }
        }
    val samplesF = remember(plotExpression, degrees, viewport.xMin, viewport.xMax) { sample(plotExpression) }
    val samplesG = remember(plotExpression2, degrees, viewport.xMin, viewport.xMax) { sample(plotExpression2) }
    // Zeros da f(x) e pontos em que as duas curvas se cruzam.
    val roots = remember(samplesF) { if (hasF) findRoots(::f, viewport.xMin, viewport.xMax) else emptyList() }
    val rootsG = remember(samplesG) { if (hasG) findRoots(::g, viewport.xMin, viewport.xMax) else emptyList() }
    val crossings = remember(samplesF, samplesG) {
        if (hasF && hasG) findRoots({ f(it) - g(it) }, viewport.xMin, viewport.xMax) else emptyList()
    }
    val (yLo, yHi) = remember(samplesF, samplesG, viewport) {
        if (viewport.autoY) graphYRange((samplesF + samplesG).map { it.second }) else viewport.yMin to viewport.yMax
    }
    val anything = hasF || hasG
    // Os gestos são criados uma vez; leem a faixa e os pontos atuais por aqui.
    val currentRange by rememberUpdatedState(yLo to yHi)
    val markers by rememberUpdatedState(roots + rootsG + crossings)
    fun label(v: Double) = axisLabel(v).replace('.', decimalSep)
    fun number(v: Double) = if (v.isFinite()) prettyNumber(formatNumber(v), decimalSep) else "—"

    val rootsText = roots.joinToString("; ") { label(it) }
    val description = stringResource(R.string.graph_desc, prettyExpression(plotExpression, decimalSep)) +
        (if (hasG) " g(x) = " + prettyExpression(plotExpression2, decimalSep) + "." else "") +
        (if (roots.isNotEmpty()) " " + stringResource(R.string.graph_roots, rootsText) else "")
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
                .pointerInput(plotExpression, plotExpression2) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        if (!anything) return@detectTransformGestures
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
                .pointerInput(plotExpression, plotExpression2) {
                    detectTapGestures { offset ->
                        if (!anything) return@detectTapGestures
                        val span = viewport.xMax - viewport.xMin
                        val x = viewport.xMin + offset.x / size.width * span
                        // Toque perto de uma raiz ou cruzamento "gruda" nele.
                        val snap = 24.dp.toPx() / size.width * span
                        tappedX = markers.minByOrNull { abs(it - x) }?.takeIf { abs(it - x) < snap } ?: x
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

            val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            if (samplesG.isNotEmpty()) {
                drawPath(curvePath(samplesG, yLo, yHi, ::px, ::py), color = SecondCurveColor, style = stroke)
            }
            if (samplesF.isNotEmpty()) {
                drawPath(curvePath(samplesF, yLo, yHi, ::px, ::py), color = colors.accent, style = stroke)
            }
            // Raízes (no eixo x) e cruzamentos: anéis pequenos.
            val ring = Stroke(width = 2.dp.toPx())
            for (r in roots) drawCircle(colors.accent, radius = 4.dp.toPx(), center = Offset(px(r), py(0.0)), style = ring)
            for (r in rootsG) drawCircle(SecondCurveColor, radius = 4.dp.toPx(), center = Offset(px(r), py(0.0)), style = ring)
            for (c in crossings) {
                val y = f(c)
                if (y.isFinite()) drawCircle(colors.text, radius = 4.dp.toPx(), center = Offset(px(c), py(y)), style = ring)
            }
            if (!tappedX.isNaN()) {
                drawLine(colors.axis, Offset(px(tappedX), 0f), Offset(px(tappedX), h), strokeWidth = 1.5f)
                listOfNotNull(
                    if (hasF) f(tappedX) to colors.accent else null,
                    if (hasG) g(tappedX) to SecondCurveColor else null,
                ).forEach { (ty, color) ->
                    if (ty.isFinite()) {
                        drawCircle(colors.text, radius = 6.dp.toPx(), center = Offset(px(tappedX), py(ty)))
                        drawCircle(color, radius = 4.dp.toPx(), center = Offset(px(tappedX), py(ty)))
                    }
                }
            }
        }
        if (!anything) {
            Text(
                text = stringResource(R.string.graph_hint),
                modifier = Modifier.align(Alignment.Center).padding(16.dp),
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
                val text = when {
                    hasF && hasG -> stringResource(R.string.graph_point2, label(tappedX), number(f(tappedX)), number(g(tappedX)))
                    hasF -> stringResource(R.string.graph_point, label(tappedX), number(f(tappedX)))
                    else -> stringResource(R.string.graph_point, label(tappedX), number(g(tappedX)))
                }
                Text(
                    text = text,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.chip)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    color = colors.text,
                    fontSize = 13.sp
                )
            }
            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (!viewport.isDefault || !tappedX.isNaN()) {
                    GraphChip(stringResource(R.string.graph_reset), colors, highlighted = true) {
                        viewport = Viewport()
                        tappedX = Double.NaN
                    }
                }
                GraphChip(stringResource(R.string.graph_range), colors, highlighted = editingRange) {
                    editingRange = !editingRange
                }
            }
            if (editingRange) {
                RangeEditor(
                    viewport = viewport,
                    colors = colors,
                    decimalSep = decimalSep,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp),
                    onApply = { lo, hi ->
                        viewport = Viewport(lo, hi)
                        editingRange = false
                    }
                )
            }
        }
    }
}

@Composable
private fun GraphChip(text: String, colors: CalcColors, highlighted: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (highlighted) colors.accent else colors.chip)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        color = if (highlighted) colors.keyText else colors.chipText,
        fontSize = 12.sp
    )
}

/** "x de [a] até [b] OK": escolhe o intervalo de x à mão. */
@Composable
private fun RangeEditor(
    viewport: Viewport,
    colors: CalcColors,
    decimalSep: Char,
    modifier: Modifier = Modifier,
    onApply: (Double, Double) -> Unit,
) {
    fun show(v: Double) = formatNumber(v).replace('.', decimalSep)
    var from by rememberSaveable { mutableStateOf(show(viewport.xMin)) }
    var to by rememberSaveable { mutableStateOf(show(viewport.xMax)) }
    fun parse(t: String) = t.replace(',', '.').toDoubleOrNull()
    val lo = parse(from)
    val hi = parse(to)
    val valid = lo != null && hi != null && lo < hi && hi - lo >= 1e-6
    val fromDesc = stringResource(R.string.graph_range_from)
    val toDesc = stringResource(R.string.graph_range_to)
    Row(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(colors.background)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(fromDesc, color = colors.secondaryText, fontSize = 13.sp)
        RangeField(from, { from = it }, colors, Modifier.semantics { contentDescription = fromDesc })
        Text(toDesc, color = colors.secondaryText, fontSize = 13.sp)
        RangeField(to, { to = it }, colors, Modifier.semantics { contentDescription = toDesc })
        Text(
            text = stringResource(R.string.graph_range_apply),
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(if (valid) colors.accent else colors.chip)
                .clickable(enabled = valid, role = Role.Button) { onApply(lo!!, hi!!) }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            color = if (valid) colors.keyText else colors.secondaryText,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun RangeField(value: String, onChange: (String) -> Unit, colors: CalcColors, modifier: Modifier = Modifier) {
    BasicTextField(
        value = value,
        onValueChange = { new ->
            if (new.length <= 10 && new.all { it.isDigit() || it == '.' || it == ',' || it == '-' }) onChange(new)
        },
        textStyle = TextStyle(fontSize = 15.sp, textAlign = TextAlign.Center, color = colors.text),
        cursorBrush = SolidColor(colors.accent),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = modifier
            .width(64.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.card)
            .padding(vertical = 6.dp, horizontal = 4.dp)
    )
}
