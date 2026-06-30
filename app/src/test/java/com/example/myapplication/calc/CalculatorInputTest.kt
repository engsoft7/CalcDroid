package com.example.myapplication.calc

import org.junit.Assert.assertEquals
import org.junit.Test

class CalculatorInputTest {

    @Test
    fun appendsDigitsAndCollapsesLeadingZero() {
        assertEquals("7", CalculatorInput.appendDigit("", '7'))
        assertEquals("5", CalculatorInput.appendDigit("0", '5'))
        assertEquals("123", CalculatorInput.appendDigit("12", '3'))
    }

    @Test
    fun appendsDecimalPointOnce() {
        assertEquals("0.", CalculatorInput.appendDecimal(""))
        assertEquals("5+0.", CalculatorInput.appendDecimal("5+"))
        assertEquals("1.5", CalculatorInput.appendDecimal("1.5"))
    }

    @Test
    fun appendOperatorSwapsTrailingOperator() {
        assertEquals("5×", CalculatorInput.appendOperator("5+", '×'))
        assertEquals("5+", CalculatorInput.appendOperator("5", '+'))
    }

    @Test
    fun onlyLeadingMinusIsAllowedOnEmptyExpression() {
        assertEquals("", CalculatorInput.appendOperator("", '×'))
        assertEquals("−", CalculatorInput.appendOperator("", '−'))
    }

    @Test
    fun appendsPercentOnlyAfterANumber() {
        assertEquals("50%", CalculatorInput.appendPercent("50"))
        assertEquals("5+", CalculatorInput.appendPercent("5+"))
    }

    @Test
    fun backspaceRemovesLastCharacter() {
        assertEquals("12", CalculatorInput.backspace("123"))
        assertEquals("", CalculatorInput.backspace(""))
    }

    @Test
    fun toggleSignFlipsTheCurrentNumber() {
        assertEquals("−5", CalculatorInput.toggleSign("5"))
        assertEquals("5", CalculatorInput.toggleSign("−5"))
        assertEquals("5+−3", CalculatorInput.toggleSign("5+3"))
        assertEquals("5+3", CalculatorInput.toggleSign("5+−3"))
    }

    @Test
    fun formatsExpressionForDisplay() {
        assertEquals("1,000,000", CalculatorInput.formatForDisplay("1000000"))
        assertEquals("12 + 3", CalculatorInput.formatForDisplay("12+3"))
        assertEquals("1,234.5", CalculatorInput.formatForDisplay("1234.5"))
    }
}
