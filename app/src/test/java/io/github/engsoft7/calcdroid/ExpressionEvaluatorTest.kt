package io.github.engsoft7.calcdroid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ExpressionEvaluatorTest {

    private fun eval(expression: String, x: Double = Double.NaN) =
        ExpressionEvaluator.evaluate(expression, x)

    @Test
    fun precedenciaEAssociatividade() {
        assertEquals(14.0, eval("2+3*4"), 1e-12)
        assertEquals(512.0, eval("2^3^2"), 1e-12) // 2^(3^2)
        assertEquals(-4.0, eval("-2^2"), 1e-12)
        assertEquals(0.5, eval("2^-1"), 1e-12)
        assertEquals(-15.0, eval("5*-3"), 1e-12)
    }

    @Test
    fun multiplicacaoImplicita() {
        assertEquals(285.0, eval("5+7(8*5)"), 1e-12)
        assertEquals(2 * Math.PI, eval("2π"), 1e-12)
        assertEquals(6.0, eval("(2)(3)"), 1e-12)
    }

    @Test
    fun funcoesEmGraus() {
        assertEquals(17.0, eval("2sin(30)+√16^2"), 1e-9) // 2·0,5 + √(16²)
        assertEquals(0.25, eval("sin(30)^2"), 1e-9)
        assertEquals(1.0, eval("tan(45)"), 1e-9)
        assertEquals(2.0, eval("log(100)"), 1e-12)
        assertEquals(1.0, eval("ln(e)"), 1e-12)
    }

    @Test
    fun posfixos() {
        assertEquals(120.0, eval("5!"), 1e-12)
        assertEquals(0.5, eval("50%"), 1e-12)
        assertEquals(5.0, eval("50*10%"), 1e-12)
    }

    @Test
    fun notacaoCientificaDosResultados() {
        assertEquals(1.5e12, eval("1.5E+12"), 1.0)
        assertEquals(1e12 + 1, eval("1E+12+1"), 1e-3)
        assertEquals(2e-7, eval("2E-7"), 1e-20)
        // 'e' minúsculo continua sendo a constante de Euler.
        assertEquals(2 * Math.E, eval("2e"), 1e-12)
    }

    @Test
    fun variavelX() {
        assertEquals(9.0, eval("x^2", 3.0), 1e-12)
        assertThrows(IllegalArgumentException::class.java) { eval("x+1") }
    }

    @Test
    fun expressoesMalformadas() {
        assertThrows(IllegalArgumentException::class.java) { eval("2+") }
        assertThrows(IllegalArgumentException::class.java) { eval("(2") }
        assertThrows(IllegalArgumentException::class.java) { eval("2)") }
        assertThrows(IllegalArgumentException::class.java) { eval("1E") }
        assertThrows(IllegalArgumentException::class.java) { eval("1..2") }
    }

    @Test
    fun formatNumberEscondeErroBinario() {
        assertEquals("0.3", formatNumber(0.1 + 0.2))
        assertEquals("5", formatNumber(5.0))
        assertEquals("-2.5", formatNumber(-2.5))
        assertEquals("Erro", formatNumber(Double.NaN))
        assertEquals("Erro", formatNumber(Double.POSITIVE_INFINITY))
    }

    @Test
    fun formatNumberGrandeEmNotacaoCientifica() {
        assertEquals("1E+12", formatNumber(1e12))
        assertEquals("1.234568E+14", formatNumber(123456789012345.0))
        assertEquals("-1.5E+13", formatNumber(-1.5e13))
        // O texto exibido volta a ser lido pelo avaliador.
        assertEquals(1.234568e14, eval(formatNumber(123456789012345.0)), 1.0)
    }

    @Test
    fun fatorial() {
        assertEquals(1.0, factorial(0.0), 0.0)
        assertEquals(3628800.0, factorial(10.0), 0.0)
        assert(factorial(-1.0).isNaN())
        assert(factorial(2.5).isNaN())
        assertEquals(Double.POSITIVE_INFINITY, factorial(171.0), 0.0)
    }

    @Test
    fun fechaParentesesAbertos() {
        assertEquals("sin(30)", autoCloseParentheses("sin(30"))
        assertEquals("((2+3))", autoCloseParentheses("((2+3"))
        assertEquals("2)", autoCloseParentheses("2)"))
    }
}
