package io.github.engsoft7.calcdroid

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.E
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Avaliador de expressões numéricas completas, ex.: 5+7(8*5), 2sin(30)+√16^2.
 *
 * Gramática (precedência do mais fraco ao mais forte):
 *   expressão := termo (('+' | '-') termo)*
 *   termo     := unário (('*' | '/') unário | unário-implícito)*   // 7(8*5), 2π, 3sin(30)
 *   unário    := ('-' | '+') unário | potência
 *   potência  := pósfixo ('^' unário)?                             // associativa à direita
 *   pósfixo   := átomo ('!' | '%')*                                // fatorial e porcentagem
 *   átomo     := número | π | e | x | '(' expressão ')' | função unário
 *
 * As funções trigonométricas recebem o ângulo em graus. Expressões
 * malformadas lançam IllegalArgumentException.
 */
object ExpressionEvaluator {

    private val FUNCTIONS = listOf("sin", "cos", "tan", "log", "ln", "√")

    fun evaluate(expression: String, xValue: Double = Double.NaN): Double {
        val parser = Parser(expression.replace(" ", ""), xValue)
        val value = parser.parseExpression()
        if (!parser.atEnd()) {
            throw IllegalArgumentException("Símbolo inesperado em '${parser.rest()}'")
        }
        return value
    }

    private class Parser(private val text: String, private val xValue: Double) {
        private var pos = 0

        fun atEnd() = pos >= text.length
        fun rest(): String = text.substring(pos)
        private fun peek(): Char? = text.getOrNull(pos)

        fun parseExpression(): Double {
            var value = parseTerm()
            while (true) {
                when (peek()) {
                    '+' -> { pos++; value += parseTerm() }
                    '-' -> { pos++; value -= parseTerm() }
                    else -> return value
                }
            }
        }

        private fun parseTerm(): Double {
            var value = parseUnary()
            while (true) {
                val c = peek() ?: return value
                when {
                    c == '*' -> { pos++; value *= parseUnary() }
                    c == '/' -> { pos++; value /= parseUnary() }
                    startsFactor(c) -> value *= parseUnary()
                    else -> return value
                }
            }
        }

        // Um novo fator pode começar neste caractere? (multiplicação implícita)
        private fun startsFactor(c: Char): Boolean =
            c.isDigit() || c == '.' || c == '(' || c == 'π' || c == '√' || c.isLetter()

        private fun parseUnary(): Double = when (peek()) {
            '-' -> { pos++; -parseUnary() }
            '+' -> { pos++; parseUnary() }
            else -> parsePower()
        }

        private fun parsePower(): Double {
            val base = parsePostfix()
            if (peek() == '^') {
                pos++
                return base.pow(parseUnary())
            }
            return base
        }

        private fun parsePostfix(): Double {
            var value = parseAtom()
            while (true) {
                when (peek()) {
                    '!' -> { pos++; value = factorial(value) }
                    '%' -> { pos++; value /= 100 }
                    else -> return value
                }
            }
        }

        private fun parseAtom(): Double {
            val c = peek() ?: throw IllegalArgumentException("Expressão incompleta")
            return when {
                c.isDigit() || c == '.' -> parseNumber()
                c == '(' -> {
                    pos++
                    val value = parseExpression()
                    if (peek() != ')') throw IllegalArgumentException("Falta fechar parêntese")
                    pos++
                    value
                }
                c == 'π' -> { pos++; PI }
                c == 'e' -> { pos++; E }
                c == 'x' -> {
                    if (xValue.isNaN()) throw IllegalArgumentException("x só existe no modo gráfico")
                    pos++
                    xValue
                }
                else -> parseFunction()
            }
        }

        private fun parseNumber(): Double {
            val start = pos
            while (peek()?.let { it.isDigit() || it == '.' } == true) pos++
            val token = text.substring(start, pos)
            return token.toDoubleOrNull()
                ?: throw IllegalArgumentException("Número inválido: $token")
        }

        private fun parseFunction(): Double {
            for (name in FUNCTIONS) {
                if (text.startsWith(name, pos)) {
                    pos += name.length
                    val arg = parseUnary()
                    return when (name) {
                        "sin" -> sin(Math.toRadians(arg))
                        "cos" -> cos(Math.toRadians(arg))
                        "tan" -> tan(Math.toRadians(arg))
                        "log" -> log10(arg)
                        "ln" -> ln(arg)
                        else -> sqrt(arg)
                    }
                }
            }
            throw IllegalArgumentException("Símbolo inesperado: ${peek()}")
        }
    }
}

// Completa os parênteses deixados abertos, para "sin(30" valer como "sin(30)".
fun autoCloseParentheses(expression: String): String {
    val missing = expression.count { it == '(' } - expression.count { it == ')' }
    return expression + ")".repeat(missing.coerceAtLeast(0))
}

fun factorial(n: Double): Double {
    // Fatorial só é definido para inteiros não negativos.
    if (n < 0 || n != floor(n)) return Double.NaN
    // Acima de 170! o resultado não cabe em um Double; retorna logo
    // infinito para não percorrer um loop gigante.
    if (n > 170) return Double.POSITIVE_INFINITY
    var result = 1.0
    for (i in 2..n.toInt()) {
        result *= i
    }
    return result
}

// Converte o resultado para um texto amigável ao display: arredonda para
// esconder o erro binário do Double (0.1+0.2, tan 45°...), remove o ".0"
// de valores inteiros e troca NaN/Infinity por "Erro".
fun formatNumber(value: Double): String {
    if (value.isNaN() || value.isInfinite()) return "Erro"
    if (abs(value) >= 1e12) {
        return String.format(Locale.US, "%.6E", value)
    }
    return BigDecimal(value)
        .setScale(10, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()
}
