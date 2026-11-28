package io.github.engsoft7.calcdroid

import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Lê uma lista de números digitada à mão: separados por espaço, quebra de
 * linha ou ";" (e por "," quando a vírgula não é o separador decimal).
 * Devolve null se algum item não for número.
 */
fun parseNumberList(text: String, decimalSep: Char = ','): List<Double>? {
    val separators = if (decimalSep == ',') Regex("[\\s;]+") else Regex("[\\s;,]+")
    val items = text.trim().split(separators).filter { it.isNotEmpty() }
    return items.map { item ->
        val normalized = if (decimalSep == ',') item.replace(',', '.') else item
        normalized.toDoubleOrNull()?.takeIf { it.isFinite() } ?: return null
    }
}

data class Stats(
    val count: Int,
    val sum: Double,
    val mean: Double,
    val median: Double,
    val min: Double,
    val max: Double,
    // Desvio padrão populacional (divide por n) e amostral (n − 1).
    val stdDevPopulation: Double,
    val stdDevSample: Double,
)

fun stats(values: List<Double>): Stats? {
    if (values.isEmpty()) return null
    val n = values.size
    val sorted = values.sorted()
    val mean = values.sum() / n
    val median = if (n % 2 == 1) sorted[n / 2] else (sorted[n / 2 - 1] + sorted[n / 2]) / 2
    val squares = values.sumOf { (it - mean) * (it - mean) }
    return Stats(
        count = n,
        sum = values.sum(),
        mean = mean,
        median = median,
        min = sorted.first(),
        max = sorted.last(),
        stdDevPopulation = sqrt(squares / n),
        stdDevSample = if (n > 1) sqrt(squares / (n - 1)) else Double.NaN,
    )
}

/** Reta de mínimos quadrados y = a + b·x, com o coeficiente de correlação r. */
data class Regression(val intercept: Double, val slope: Double, val r: Double)

fun linearRegression(xs: List<Double>, ys: List<Double>): Regression? {
    if (xs.size != ys.size || xs.size < 2) return null
    val n = xs.size
    val mx = xs.sum() / n
    val my = ys.sum() / n
    var sxx = 0.0
    var syy = 0.0
    var sxy = 0.0
    for (i in 0 until n) {
        val dx = xs[i] - mx
        val dy = ys[i] - my
        sxx += dx * dx
        syy += dy * dy
        sxy += dx * dy
    }
    if (sxx == 0.0) return null
    val slope = sxy / sxx
    val r = if (syy == 0.0) Double.NaN else sxy / sqrt(sxx * syy)
    return Regression(my - slope * mx, slope, r)
}

object Finance {
    /**
     * Montante de [principal] a [ratePercent]% por período, durante [periods]
     * períodos, com [deposit] depositado ao fim de cada período.
     */
    fun compound(principal: Double, ratePercent: Double, periods: Double, deposit: Double = 0.0): Double {
        val i = ratePercent / 100
        if (i == 0.0) return principal + deposit * periods
        val growth = (1 + i).pow(periods)
        return principal * growth + deposit * (growth - 1) / i
    }

    /** Parcela fixa (tabela Price) de um financiamento de [amount] em [installments] vezes. */
    fun installment(amount: Double, ratePercent: Double, installments: Int): Double {
        if (installments <= 0) return Double.NaN
        val i = ratePercent / 100
        if (i == 0.0) return amount / installments
        return amount * i / (1 - (1 + i).pow(-installments))
    }

    /** Variação percentual de [from] para [to]; NaN se [from] for zero. */
    fun percentChange(from: Double, to: Double): Double =
        if (from == 0.0) Double.NaN else (to - from) / kotlin.math.abs(from) * 100
}
