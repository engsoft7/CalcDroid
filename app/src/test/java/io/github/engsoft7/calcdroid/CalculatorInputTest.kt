package io.github.engsoft7.calcdroid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorInputTest {

    private fun type(vararg keys: String, start: CalcState = CalcState(), graph: Boolean = false) =
        keys.fold(start) { state, key -> state.press(key, graphMode = graph) }

    @Test
    fun contaSimples() {
        val s = type("2", "+", "3", "=")
        assertEquals("5", s.expression)
        assertEquals("2+3 =", s.lastExpression)
        assertTrue(s.justEvaluated)
    }

    @Test
    fun menosDepoisDeVezesViraSinal() {
        assertEquals("5*-", type("5", "*", "-").expression)
        assertEquals("-15", type("5", "*", "-", "3", "=").expression)
        assertEquals("0.5", type("2", "^", "-", "1", "=").expression)
        // Um segundo "-" não acumula, e outro operador troca os dois.
        assertEquals("5*-", type("5", "*", "-", "-").expression)
        assertEquals("5+", type("5", "*", "-", "+").expression)
        assertEquals("5-", type("5", "-", "-").expression)
    }

    @Test
    fun operadorSubstituiOAnterior() {
        assertEquals("5*", type("5", "+", "*").expression)
        assertEquals("5/", type("5", "-", "/").expression)
    }

    @Test
    fun continuaSobreOResultado() {
        val s = type("2", "+", "3", "=", "*", "4")
        assertEquals("5*4", s.expression)
        assertEquals("2+3 =", s.lastExpression) // a conta anterior segue visível
        assertEquals("20", s.press("=").expression)
    }

    @Test
    fun digitoDepoisDoResultadoComecaDoZero() {
        val s = type("2", "+", "3", "=", "7")
        assertEquals("7", s.expression)
        assertEquals("", s.lastExpression)
    }

    @Test
    fun continuaSobreResultadoGrande() {
        val s = type("2", "0", "0", "0", "0", "0", "0", "*", "1", "0", "0", "0", "0", "0", "0", "=")
        assertEquals("2E+12", s.expression)
        assertEquals("1E+12", type("/", "2", "=", start = s).expression)
    }

    @Test
    fun apagarResultadoEmNotacaoCientificaNaoDeixaExpoentePendurado() {
        val s = CalcState(expression = "1.5E+12", justEvaluated = true, lastExpression = "x =")
        val once = s.press("⌫")
        assertEquals("1.5E+1", once.expression)
        assertEquals("", once.lastExpression)
        assertFalse(once.justEvaluated)
        assertEquals("1.5", once.press("⌫").expression)
    }

    @Test
    fun pontoDecimal() {
        assertEquals("0.", type(".").expression)
        assertEquals("1.5", type("1", ".", "5", ".").expression)
        assertEquals("2+0.", type("2", "+", ".").expression)
        // Expoente de resultado não aceita ponto.
        assertEquals("1E+12", CalcState(expression = "1E+12").press(".").expression)
    }

    @Test
    fun zeroAEsquerdaESubstituido() {
        assertEquals("5", type("0", "5").expression)
        assertEquals("0.5", type("0", ".", "5").expression)
    }

    @Test
    fun teclasSemOperandoSaoIgnoradas() {
        assertEquals("", type("*").expression)
        assertEquals("", type("!").expression)
        assertEquals("(", type("(", ")").expression)
        assertEquals("5", type("5", ")").expression)
        assertEquals("5+", type("5", "+", "!").expression)
        assertEquals("(5)", type("(", "5", ")").expression)
    }

    @Test
    fun funcoesEntramESaemInteiras() {
        assertEquals("sin(", type("sin").expression)
        assertEquals("2", type("2", "√", "⌫").expression)
        assertEquals("2sin(30)", type("2", "sin", "3", "0", "=").lastExpression.removeSuffix(" ="))
    }

    @Test
    fun erroELimpar() {
        val s = type("5", "/", "0", "=")
        assertEquals(ERROR_TEXT, s.expression)
        assertEquals("", s.press("⌫").expression)
        assertEquals("7", s.press("7").expression)
        assertEquals(CalcState(), s.press("C"))
    }

    @Test
    fun modoGraficoGuardaAExpressaoParaDesenhar() {
        val s = type("sin", "x", "=", graph = true)
        assertEquals("sin(x)", s.plotExpression)
        assertEquals("sin(x)", s.expression)
        assertFalse(s.justEvaluated)
    }

    @Test
    fun limiteDeTamanho() {
        val s = (1..100).fold(CalcState()) { state, _ -> state.press("9") }
        assertEquals(60, s.expression.length)
    }

    @Test
    fun visorUsaSimbolos() {
        assertEquals("5×3÷2-1", prettyExpression("5*3/2-1"))
    }

    @Test
    fun tecladoDePaisagemTemTodasAsTeclas() {
        val sci = listOf(
            listOf("sin", "cos", "tan", "√"),
            listOf("ln", "log", "(", ")"),
            listOf("π", "e", "^", "!")
        )
        val basic = listOf(
            listOf("C", "⌫", "%", "/"),
            listOf("7", "8", "9", "*"),
            listOf("4", "5", "6", "-"),
            listOf("1", "2", "3", "+"),
            listOf("0", ".", "=")
        )
        val rows = sideBySideRows(sci, basic)
        assertEquals(5, rows.size)
        assertTrue(rows.all { it.size == 7 })
        assertEquals((sci + basic).flatten().sorted(), rows.flatten().filter { it.isNotEmpty() }.sorted())
        // As teclas científicas ficam alinhadas por baixo, ao lado dos números.
        assertEquals(listOf("", "", "", "C", "⌫", "%", "/"), rows[0])
        assertEquals(listOf("e", "^", "!", "0", ".", "=", ""), rows[4])
    }
}
