package io.github.engsoft7.calcdroid

import org.junit.Assert.assertEquals
import org.junit.Test

class CalculatorLogicTest {

    @Test
    fun `soma simples exibe inteiro sem ponto decimal`() {
        assertEquals("4", calculate("2", "2", "+"))
    }

    @Test
    fun `subtracao com resultado negativo`() {
        assertEquals("-1", calculate("2", "3", "-"))
    }

    @Test
    fun `multiplicacao de decimais arredonda ruido de ponto flutuante`() {
        // 0.1 + 0.2 em Double é 0.30000000000000004
        assertEquals("0.3", calculate("0.1", "0.2", "+"))
    }

    @Test
    fun `divisao exata exibe decimal limpo`() {
        assertEquals("2.5", calculate("5", "2", "/"))
    }

    @Test
    fun `divisao por zero retorna erro em vez de Infinity`() {
        assertEquals(ERROR_DISPLAY, calculate("5", "0", "/"))
    }

    @Test
    fun `zero dividido por zero retorna erro em vez de NaN`() {
        assertEquals(ERROR_DISPLAY, calculate("0", "0", "/"))
    }

    @Test
    fun `entrada invalida nao lanca excecao`() {
        assertEquals(ERROR_DISPLAY, calculate("1.2.3", "2", "+"))
        assertEquals(ERROR_DISPLAY, calculate("", "2", "+"))
        assertEquals(ERROR_DISPLAY, calculate("abc", "2", "+"))
    }

    @Test
    fun `operador desconhecido retorna erro`() {
        assertEquals(ERROR_DISPLAY, calculate("1", "2", "?"))
    }

    @Test
    fun `dizima periodica limitada a dez casas`() {
        assertEquals("0.3333333333", calculate("1", "3", "/"))
    }

    @Test
    fun `zero negativo normalizado para zero`() {
        assertEquals("0", formatResult(-0.0))
    }

    @Test
    fun `numeros muito grandes usam notacao cientifica`() {
        assertEquals(ERROR_DISPLAY, formatResult(Double.POSITIVE_INFINITY))
        assertEquals("1.00000e+15", formatResult(1e15))
    }
}
