package com.example.myapplication.calc

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

/**
 * Pure-Kotlin calculator core: tokenises and evaluates an infix expression that
 * respects operator precedence, unary signs, parentheses and postfix percent.
 *
 * The class is intentionally free of any Android dependency so the maths can be
 * unit-tested on a plain JVM.
 */
object CalculatorEngine {

    const val ERROR = "Erro"

    // Display glyphs used throughout the UI.
    const val DIVIDE = '÷' // ÷
    const val MULTIPLY = '×' // ×
    const val MINUS = '−' // − (true minus sign, not hyphen)
    const val PLUS = '+'
    const val PERCENT = '%'

    private val OPERATORS = charArrayOf(PLUS, MINUS, MULTIPLY, DIVIDE)

    /**
     * Evaluates [expression] and returns the value, or `null` when the
     * expression is incomplete or malformed (e.g. while the user is still
     * typing). A finite result is always returned for valid expressions; an
     * infinite result (division by zero) is surfaced as a non-null infinity so
     * callers can decide how to present it.
     */
    fun tryEvaluate(expression: String): Double? {
        val tokens = try {
            tokenize(expression)
        } catch (e: Exception) {
            return null
        }
        if (tokens.isEmpty()) return null
        return try {
            Parser(tokens).parse()
        } catch (e: Exception) {
            null
        }
    }

    /** A formatted live preview, or empty string when nothing sensible to show. */
    fun preview(expression: String): String {
        val value = tryEvaluate(expression) ?: return ""
        if (value.isNaN() || value.isInfinite()) return ""
        return format(value)
    }

    /** The committed result for the "=" key. Surfaces [ERROR] on bad input. */
    fun evaluate(expression: String): String {
        val value = tryEvaluate(expression) ?: return ERROR
        if (value.isNaN() || value.isInfinite()) return ERROR
        return format(value)
    }

    // --- Number formatting -------------------------------------------------

    // Use the app's true minus sign (U+2212) so results match the operator glyphs.
    private val symbols = DecimalFormatSymbols(Locale.US).apply { minusSign = MINUS }
    private val plainFormat = DecimalFormat("#,##0.##########", symbols)
    private val scientificFormat = DecimalFormat("0.######E0", symbols)

    fun format(value: Double): String {
        if (value == 0.0) return "0" // also collapses -0.0
        val magnitude = abs(value)
        return if (magnitude >= 1e15 || magnitude < 1e-9) {
            scientificFormat.format(value)
        } else {
            plainFormat.format(value)
        }
    }

    // --- Tokeniser ---------------------------------------------------------

    private sealed interface Token
    private data class Num(val value: Double) : Token
    private data class Op(val symbol: Char) : Token
    private object LParen : Token
    private object RParen : Token

    private fun normalizeOp(c: Char): Char = when (c) {
        '*' -> MULTIPLY
        '/' -> DIVIDE
        '-' -> MINUS
        else -> c
    }

    private fun tokenize(expr: String): List<Token> {
        val tokens = ArrayList<Token>()
        var i = 0
        while (i < expr.length) {
            val c = expr[i]
            when {
                c == ' ' || c == ',' -> i++ // ignore grouping separators / whitespace
                c.isDigit() || c == '.' -> {
                    val start = i
                    while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) i++
                    tokens.add(Num(expr.substring(start, i).toDouble()))
                }
                c == '(' -> { tokens.add(LParen); i++ }
                c == ')' -> { tokens.add(RParen); i++ }
                c == '+' || c == '-' || c == MINUS || c == '*' || c == MULTIPLY ||
                    c == '/' || c == DIVIDE || c == PERCENT -> {
                    tokens.add(Op(normalizeOp(c))); i++
                }
                else -> throw IllegalArgumentException("Unexpected character: $c")
            }
        }
        return tokens
    }

    // --- Recursive-descent parser -----------------------------------------
    //
    //   expr    := term (('+' | '−') term)*
    //   term    := factor (('×' | '÷') factor)*
    //   factor  := ('+' | '−') factor | postfix
    //   postfix := primary ('%')*
    //   primary := number | '(' expr ')'

    private class Parser(private val tokens: List<Token>) {
        private var pos = 0

        private fun peek(): Token? = tokens.getOrNull(pos)
        private fun advance(): Token = tokens[pos++]

        fun parse(): Double {
            val value = parseExpr()
            if (pos != tokens.size) throw IllegalStateException("Trailing tokens")
            return value
        }

        private fun parseExpr(): Double {
            var value = parseTerm()
            while (true) {
                val t = peek()
                if (t is Op && (t.symbol == PLUS || t.symbol == MINUS)) {
                    advance()
                    val rhs = parseTerm()
                    value = if (t.symbol == PLUS) value + rhs else value - rhs
                } else break
            }
            return value
        }

        private fun parseTerm(): Double {
            var value = parseFactor()
            while (true) {
                val t = peek()
                if (t is Op && (t.symbol == MULTIPLY || t.symbol == DIVIDE)) {
                    advance()
                    val rhs = parseFactor()
                    value = if (t.symbol == MULTIPLY) value * rhs else value / rhs
                } else break
            }
            return value
        }

        private fun parseFactor(): Double {
            val t = peek()
            if (t is Op && (t.symbol == MINUS || t.symbol == PLUS)) {
                advance()
                val operand = parseFactor()
                return if (t.symbol == MINUS) -operand else operand
            }
            return parsePostfix()
        }

        private fun parsePostfix(): Double {
            var value = parsePrimary()
            while (true) {
                val t = peek()
                if (t is Op && t.symbol == PERCENT) {
                    advance()
                    value /= 100.0
                } else break
            }
            return value
        }

        private fun parsePrimary(): Double {
            return when (val t = peek() ?: throw IllegalStateException("Unexpected end")) {
                is Num -> { advance(); t.value }
                is LParen -> {
                    advance()
                    val inner = parseExpr()
                    if (peek() is RParen) advance() else throw IllegalStateException("Expected )")
                    inner
                }
                else -> throw IllegalStateException("Unexpected token")
            }
        }
    }
}
