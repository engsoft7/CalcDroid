package io.github.engsoft7.calcdroid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NewFeaturesTest {

    private fun eval(e: String, degrees: Boolean = true) = ExpressionEvaluator.evaluate(e, degrees = degrees)
    private fun type(vararg keys: String, start: CalcState = CalcState(), degrees: Boolean = true) =
        keys.fold(start) { s, k -> s.press(k, degrees = degrees) }

    @Test
    fun porcentagemComoNoCelular() {
        assertEquals(90.0, eval("100-10%"), 1e-9)
        assertEquals(55.0, eval("50+10%"), 1e-9)
        assertEquals(5.0, eval("50*10%"), 1e-9)
        assertEquals(0.1, eval("10%"), 1e-12)
        assertEquals(5.1, eval("10%+5"), 1e-9)
        assertEquals(180.0, eval("2*(100-10%)"), 1e-9)
        // % dentro de uma multiplicação não vira fração do termo anterior.
        assertEquals(200.2, eval("200+10%*2"), 1e-9)
        assertEquals("90", type("1", "0", "0", "-", "1", "0", "%", "=").expression)
    }

    @Test
    fun grausERadianos() {
        assertEquals(0.5, eval("sin(30)"), 1e-12)
        assertEquals(1.0, eval("sin(π/2)", degrees = false), 1e-12)
        assertEquals(30.0, eval("asin(0.5)"), 1e-9)
        assertEquals(Math.PI / 6, eval("asin(0.5)", degrees = false), 1e-12)
        assertEquals(45.0, eval("atan(1)"), 1e-9)
        assertEquals(60.0, eval("acos(0.5)"), 1e-9)
        assertTrue(eval("asin(2)").isNaN())
        assertEquals("1", type("sin", "9", "0", "=").expression)
        assertEquals("0.8939966636", type("sin", "9", "0", "=", degrees = false).expression)
    }

    @Test
    fun moduloERestoEPotencias() {
        assertEquals(7.0, eval("abs(-7)"), 0.0)
        assertEquals(1.0, eval("7mod3"), 0.0)
        assertEquals(2.0, eval("-7mod3"), 0.0)
        assertEquals(6.0, eval("2+7mod3*4"), 0.0) // mod tem a precedência de * e /
        assertEquals("25", type("5", "x²", "=").expression)
        assertEquals("0.25", type("4", "x⁻¹", "=").expression)
        assertEquals("1", type("7", "mod", "3", "=").expression)
        assertEquals("", type("mod").expression)
        assertEquals("5+", type("5", "+", "x²").expression)
        assertEquals("5", type("5", "x⁻¹", "⌫").expression)
        assertEquals("7", type("7", "mod", "⌫").expression)
    }

    @Test
    fun memoria() {
        val s = type("1", "0", "M+", "C", "5", "M+", "C")
        assertEquals(15.0, s.memory, 0.0)
        assertEquals("15", s.press("MR").expression)
        assertEquals("2*15", type("2", "*", "MR", start = s).expression)
        val m = type("2", "0", "M-", start = s)
        assertEquals(-5.0, m.memory, 0.0)
        assertEquals("3(-5)", type("C", "3", "MR", start = m).expression)
        assertEquals(0.0, m.press("MC").memory, 0.0)
        // Conta inválida não mexe na memória.
        assertEquals(15.0, type("5", "+", "M+", start = s.press("C")).memory, 0.0)
        assertNull(CalcState(expression = "5+").currentValue())
    }

    @Test
    fun historico() {
        val a = HistoryEntry("2+3", "5")
        val b = HistoryEntry("7*6", "42")
        val list = addToHistory(addToHistory(emptyList(), a), b)
        assertEquals(listOf(b, a), list)
        assertEquals(list, addToHistory(list, b)) // não repete a mesma conta seguida
        assertEquals(list, decodeHistory(encodeHistory(list)))
        assertEquals(HISTORY_LIMIT, (1..80).fold(emptyList<HistoryEntry>()) { l, i ->
            addToHistory(l, HistoryEntry("$i", "$i"))
        }.size)
        assertEquals(emptyList<HistoryEntry>(), decodeHistory("lixo sem tab\n\n"))
    }

    @Test
    fun visorComVirgulaENomes() {
        assertEquals("1,5×2", prettyExpression("1.5*2"))
        assertEquals("sin⁻¹(0,5)", prettyExpression("asin(0.5)"))
        assertEquals("4⁻¹", prettyExpression("4^(-1)"))
        assertEquals("7 mod 3", prettyExpression("7mod3"))
        assertEquals("|x|(-2)", prettyExpression("abs(-2)"))
    }
}
