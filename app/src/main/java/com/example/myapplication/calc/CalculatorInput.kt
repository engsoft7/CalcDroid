package com.example.myapplication.calc

import com.example.myapplication.calc.CalculatorEngine.DIVIDE
import com.example.myapplication.calc.CalculatorEngine.MINUS
import com.example.myapplication.calc.CalculatorEngine.MULTIPLY
import com.example.myapplication.calc.CalculatorEngine.PERCENT
import com.example.myapplication.calc.CalculatorEngine.PLUS

/**
 * Stateless helpers that transform the raw expression string in response to a
 * key press. Keeping these pure makes the editing rules easy to reason about
 * and to unit-test.
 */
object CalculatorInput {

    private val OPERATORS = charArrayOf(PLUS, MINUS, MULTIPLY, DIVIDE)

    private fun Char.isOperator() = this in OPERATORS

    /** The run of digits/dots at the end of the expression (the current number). */
    private fun trailingNumber(expr: String): String {
        var i = expr.length
        while (i > 0 && (expr[i - 1].isDigit() || expr[i - 1] == '.')) i--
        return expr.substring(i)
    }

    fun appendDigit(expr: String, digit: Char): String {
        val segment = trailingNumber(expr)
        // Collapse a lone leading zero so we never produce "007".
        if (segment == "0") return expr.dropLast(1) + digit
        return expr + digit
    }

    fun appendDecimal(expr: String): String {
        val segment = trailingNumber(expr)
        if (segment.contains('.')) return expr // only one dot per number
        if (segment.isEmpty()) return expr + "0." // after an operator or at the start
        return expr + "."
    }

    fun appendOperator(expr: String, op: Char): String {
        if (expr.isEmpty()) {
            // Only a leading minus makes sense.
            return if (op == MINUS) MINUS.toString() else expr
        }
        val last = expr.last()
        return when {
            last.isOperator() -> expr.dropLast(1) + op // swap a trailing operator
            else -> expr + op
        }
    }

    fun appendPercent(expr: String): String {
        if (expr.isEmpty()) return expr
        val last = expr.last()
        return if (last.isDigit() || last == '.' || last == ')' || last == PERCENT) {
            expr + PERCENT
        } else {
            expr
        }
    }

    fun backspace(expr: String): String = if (expr.isEmpty()) expr else expr.dropLast(1)

    fun toggleSign(expr: String): String {
        if (expr.isEmpty()) return MINUS.toString()
        var start = expr.length
        while (start > 0 && (expr[start - 1].isDigit() || expr[start - 1] == '.')) start--
        val segment = expr.substring(start)
        if (segment.isEmpty()) return expr // trailing operator: nothing to negate

        val precededByUnaryMinus = start > 0 && expr[start - 1] == MINUS &&
            (start - 1 == 0 || expr[start - 2].isOperator() || expr[start - 2] == '(')
        return if (precededByUnaryMinus) {
            expr.removeRange(start - 1, start) // already negative -> make positive
        } else {
            expr.substring(0, start) + MINUS + expr.substring(start)
        }
    }

    /**
     * Pretty-prints an expression for the display: groups the integer part of
     * each number with thousands separators and pads binary operators with
     * spaces. The internal expression is never grouped so parsing stays simple.
     */
    fun formatForDisplay(expr: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < expr.length) {
            val c = expr[i]
            when {
                c.isDigit() -> {
                    val start = i
                    while (i < expr.length && expr[i].isDigit()) i++
                    sb.append(groupDigits(expr.substring(start, i)))
                    if (i < expr.length && expr[i] == '.') {
                        sb.append('.')
                        i++
                        while (i < expr.length && expr[i].isDigit()) {
                            sb.append(expr[i]); i++
                        }
                    }
                }
                c.isOperator() -> { sb.append(' ').append(c).append(' '); i++ }
                else -> { sb.append(c); i++ }
            }
        }
        return sb.toString()
    }

    private fun groupDigits(digits: String): String {
        if (digits.length <= 3) return digits
        val sb = StringBuilder()
        val firstGroup = digits.length % 3
        if (firstGroup > 0) sb.append(digits, 0, firstGroup)
        var i = firstGroup
        while (i < digits.length) {
            if (sb.isNotEmpty()) sb.append(',')
            sb.append(digits, i, i + 3)
            i += 3
        }
        return sb.toString()
    }
}
