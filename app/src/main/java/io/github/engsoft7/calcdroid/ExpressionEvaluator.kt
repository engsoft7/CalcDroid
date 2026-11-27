package io.github.engsoft7.calcdroid

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
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
 *   termo     := unário (('*' | '/' | 'mod') unário | unário-implícito)*  // 7(8*5), 2π
 *   unário    := ('-' | '+') unário | potência
 *   potência  := pósfixo ('^' unário)?                             // associativa à direita
 *   pósfixo   := átomo ('!' | '%')*                                // fatorial e porcentagem
 *   átomo     := número | π | e | x | '(' expressão ')' | função
 *   número    := dígitos ('.' dígitos)? ('E' ('+' | '-')? dígitos)?    // 1.5E+12
 *   função    := nome '(' expressão ')'                            // sin(30)^2 = (sin 30)²
 *              | nome unário                                       // √16^2 = √(16²)
 *
 * Porcentagem como nas calculadoras de celular: somada ou subtraída, ela é
 * uma fração do que vem antes (100-10% = 90, 50+10% = 55); em qualquer outro
 * lugar vale /100 (50*10% = 5, 10% = 0,1).
 *
 * As funções trigonométricas usam graus ou radianos conforme [degrees].
 * Expressões malformadas lançam IllegalArgumentException.
 */
object ExpressionEvaluator {

    // "asin" antes de "sin" não é preciso (a busca é pela posição atual), mas
    // as inversas ficam juntas para leitura.
    private val FUNCTIONS = listOf("asin", "acos", "atan", "abs", "sin", "cos", "tan", "log", "ln", "√")

    fun evaluate(expression: String, xValue: Double = Double.NaN, degrees: Boolean = true): Double {
        val parser = Parser(expression.replace(" ", ""), xValue, degrees)
        val value = parser.parseExpression()
        if (!parser.atEnd()) {
            throw IllegalArgumentException("Símbolo inesperado em '${parser.rest()}'")
        }
        return value
    }

    private class Parser(
        private val text: String,
        private val xValue: Double,
        private val degrees: Boolean
    ) {
        private var pos = 0
        // O último pós-fixo lido foi um '%', e nada (^, *, /) veio depois dele.
        private var lastWasPercent = false

        fun atEnd() = pos >= text.length
        fun rest(): String = text.substring(pos)
        private fun peek(): Char? = text.getOrNull(pos)

        fun parseExpression(): Double {
            var value = parseTerm()
            while (true) {
                val sign = when (peek()) {
                    '+' -> 1.0
                    '-' -> -1.0
                    else -> return value
                }
                pos++
                val term = parseTerm()
                // a ± b% vira a ± a·b/100 (term já vem dividido por 100).
                value += sign * if (lastWasPercent) value * term else term
                lastWasPercent = false
            }
        }

        private fun parseTerm(): Double {
            var value = parseUnary()
            var percentOnly = lastWasPercent
            while (true) {
                val c = peek() ?: break
                when {
                    c == '*' -> { pos++; value *= parseUnary() }
                    c == '/' -> { pos++; value /= parseUnary() }
                    text.startsWith("mod", pos) -> {
                        pos += 3
                        val divisor = parseUnary()
                        // Resto com o sinal do divisor, como na matemática (-7 mod 3 = 2).
                        value = value - divisor * kotlin.math.floor(value / divisor)
                    }
                    startsFactor(c) -> value *= parseUnary()
                    else -> break
                }
                percentOnly = false
            }
            lastWasPercent = percentOnly
            return value
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
                val result = base.pow(parseUnary())
                lastWasPercent = false
                return result
            }
            return base
        }

        private fun parsePostfix(): Double {
            var value = parseAtom()
            var percent = false
            while (true) {
                when (peek()) {
                    '!' -> { pos++; value = factorial(value); percent = false }
                    '%' -> { pos++; value /= 100; percent = true }
                    else -> break
                }
            }
            lastWasPercent = percent
            return value
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
            // Notação científica, como nos resultados grandes ("1.5E+12"),
            // para dar para continuar a conta em cima deles. Só o 'E'
            // maiúsculo: o 'e' minúsculo é a constante de Euler.
            if (peek() == 'E') {
                var p = pos + 1
                if (text.getOrNull(p) == '+' || text.getOrNull(p) == '-') p++
                if (text.getOrNull(p)?.isDigit() == true) {
                    pos = p
                    while (peek()?.isDigit() == true) pos++
                }
            }
            val token = text.substring(start, pos)
            return token.toDoubleOrNull()
                ?: throw IllegalArgumentException("Número inválido: $token")
        }

        private fun toRadians(angle: Double) = if (degrees) Math.toRadians(angle) else angle
        private fun fromRadians(angle: Double) = if (degrees) Math.toDegrees(angle) else angle

        private fun parseFunction(): Double {
            for (name in FUNCTIONS) {
                if (text.startsWith(name, pos)) {
                    pos += name.length
                    // Argumento entre parênteses encerra a aplicação, para o
                    // expoente/pós-fixo seguinte agir sobre o resultado:
                    // sin(30)^2 = (sin 30)². Sem parênteses o argumento
                    // estende até o unário: √16^2 = √(16²).
                    val arg = if (peek() == '(') {
                        pos++
                        val value = parseExpression()
                        if (peek() != ')') throw IllegalArgumentException("Falta fechar parêntese")
                        pos++
                        value
                    } else {
                        parseUnary()
                    }
                    lastWasPercent = false
                    return when (name) {
                        "sin" -> sin(toRadians(arg))
                        "cos" -> cos(toRadians(arg))
                        "tan" -> tan(toRadians(arg))
                        "asin" -> fromRadians(kotlin.math.asin(arg))
                        "acos" -> fromRadians(kotlin.math.acos(arg))
                        "atan" -> fromRadians(kotlin.math.atan(arg))
                        "abs" -> abs(arg)
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
        // 7 algarismos significativos em notação científica, sem zeros
        // sobrando: 1.234568E+12, 1E+15. O avaliador lê esse formato de
        // volta, então dá para continuar a conta em cima do resultado.
        return BigDecimal(value).round(MathContext(7)).stripTrailingZeros().toString()
    }
    return BigDecimal(value)
        .setScale(10, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()
}
