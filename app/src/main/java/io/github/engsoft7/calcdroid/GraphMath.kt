package io.github.engsoft7.calcdroid

import java.math.BigDecimal
import java.math.MathContext
import kotlin.math.abs
import kotlin.math.sign

/**
 * Faixa vertical [lo, hi] do gráfico a partir dos valores amostrados de f(x).
 *
 * A escala sai da faixa central dos valores (de 5% a 95%), e os extremos só
 * entram até uma faixa central de distância. Assim uma assíntota (tan, 1/x)
 * não achata o resto da curva, e uma função inteira acima de 100 (x+500)
 * continua visível, sem forçar o eixo x para dentro da tela.
 */
fun graphYRange(values: List<Double>): Pair<Double, Double> {
    val ys = values.filter { it.isFinite() }.sorted()
    if (ys.isEmpty()) return -5.0 to 5.0
    val qLo = ys[(ys.size - 1) * 5 / 100]
    val qHi = ys[(ys.size - 1) * 95 / 100]
    val band = qHi - qLo
    var lo = maxOf(ys.first(), qLo - band)
    var hi = minOf(ys.last(), qHi + band)
    // Função constante (ou quase, só ruído de ponto flutuante): abre a faixa
    // para a linha não ficar colada na borda.
    if (hi - lo < 1e-9 * maxOf(1.0, abs(hi))) {
        lo -= 1.0
        hi += 1.0
    }
    val pad = (hi - lo) * 0.08
    return (lo - pad) to (hi + pad)
}

/** Rótulo curto (3 algarismos significativos) para os valores do eixo y. */
fun axisLabel(value: Double): String {
    val rounded = BigDecimal(value).round(MathContext(3)).stripTrailingZeros()
    return if (abs(value) >= 1e6) rounded.toString() else rounded.toPlainString()
}

/**
 * Passo "redondo" (1, 2 ou 5 × 10ⁿ) para a grade, de forma que [span]
 * fique dividido em cerca de [divisions] partes.
 */
fun niceStep(span: Double, divisions: Int = 6): Double {
    if (!span.isFinite() || span <= 0) return 1.0
    val raw = span / divisions
    val magnitude = Math.pow(10.0, kotlin.math.floor(kotlin.math.log10(raw)))
    val fraction = raw / magnitude
    val nice = when {
        fraction < 1.5 -> 1.0
        fraction < 3.5 -> 2.0
        fraction < 7.5 -> 5.0
        else -> 10.0
    }
    return nice * magnitude
}

/**
 * Zeros de [f] em [xMin]..[xMax]: procura trocas de sinal em [samples]
 * intervalos e refina por bisseção. Descarta assíntotas (como em tan e 1/x),
 * onde o sinal troca mas a função não chega perto de zero.
 */
fun findRoots(f: (Double) -> Double, xMin: Double, xMax: Double, samples: Int = 400, limit: Int = 12): List<Double> {
    val roots = mutableListOf<Double>()
    val step = (xMax - xMin) / samples
    val tolerance = step * 1e-3
    var x0 = xMin
    var y0 = f(x0)
    for (i in 1..samples) {
        val x1 = xMin + i * step
        val y1 = f(x1)
        if (y0 == 0.0) {
            roots += x0
        } else if (y0.isFinite() && y1.isFinite() && y1 != 0.0 && sign(y0) != sign(y1)) {
            var a = x0
            var b = x1
            var fa = y0
            repeat(60) {
                val m = (a + b) / 2
                val fm = f(m)
                if (!fm.isFinite()) return@repeat
                if (sign(fm) == sign(fa)) { a = m; fa = fm } else b = m
            }
            val r = (a + b) / 2
            val fr = f(r)
            // Na assíntota |f| cresce ao refinar; num zero de verdade some.
            if (fr.isFinite() && abs(fr) < 1e-6 * maxOf(1.0, abs(y0), abs(y1)) &&
                abs(fr) <= minOf(abs(y0), abs(y1))
            ) roots += r
        }
        x0 = x1
        y0 = y1
        if (roots.size >= limit) break
    }
    if (y0 == 0.0 && roots.size < limit) roots += x0
    // Junta zeros repetidos (tocou o eixo exatamente na divisa de intervalos).
    return roots.fold(mutableListOf()) { acc, r ->
        if (acc.isEmpty() || abs(acc.last() - r) > tolerance + step / 2) acc += r
        acc
    }
}
