package com.example.myapplication.calc

import org.junit.Assert.assertEquals
import org.junit.Test

class CalculatorEngineTest {

    @Test
    fun respectsOperatorPrecedence() {
        assertEquals("24", CalculatorEngine.evaluate("12+3×4"))
        assertEquals("12", CalculatorEngine.evaluate("2+3×4−10÷5"))
    }

    @Test
    fun handlesBasicArithmetic() {
        assertEquals("4", CalculatorEngine.evaluate("2+2"))
        assertEquals("7", CalculatorEngine.evaluate("10−3"))
        assertEquals("4.5", CalculatorEngine.evaluate("9÷2"))
    }

    @Test
    fun handlesUnaryMinus() {
        assertEquals("−2", CalculatorEngine.evaluate("−5+3"))
        assertEquals("−15", CalculatorEngine.evaluate("5×−3"))
    }

    @Test
    fun handlesPercent() {
        assertEquals("0.5", CalculatorEngine.evaluate("50%"))
        assertEquals("20", CalculatorEngine.evaluate("200×10%"))
    }

    @Test
    fun handlesParentheses() {
        assertEquals("20", CalculatorEngine.evaluate("(2+3)×4"))
    }

    @Test
    fun groupsLargeResults() {
        assertEquals("1,000,000", CalculatorEngine.evaluate("1000×1000"))
    }

    @Test
    fun surfacesErrorsInsteadOfCrashing() {
        assertEquals(CalculatorEngine.ERROR, CalculatorEngine.evaluate("5÷0"))
        assertEquals(CalculatorEngine.ERROR, CalculatorEngine.evaluate("5+"))
        assertEquals(CalculatorEngine.ERROR, CalculatorEngine.evaluate("5++"))
    }

    @Test
    fun stripsTrailingZerosAndRoundsCleanly() {
        assertEquals("3", CalculatorEngine.evaluate("1.5+1.5"))
        assertEquals("0.3", CalculatorEngine.evaluate("0.1+0.2"))
    }

    @Test
    fun previewIsBlankWhileExpressionIsIncomplete() {
        assertEquals("", CalculatorEngine.preview("5+"))
        assertEquals("", CalculatorEngine.preview(""))
        assertEquals("", CalculatorEngine.preview("5÷0"))
        assertEquals("4", CalculatorEngine.preview("2+2"))
    }

    @Test
    fun formatsZeroAndNegativeZeroAsZero() {
        assertEquals("0", CalculatorEngine.format(0.0))
        assertEquals("0", CalculatorEngine.format(-0.0))
    }

    @Test
    fun formatsVeryLargeValuesInScientificNotation() {
        assertEquals("1E20", CalculatorEngine.format(1.0e20))
    }
}
