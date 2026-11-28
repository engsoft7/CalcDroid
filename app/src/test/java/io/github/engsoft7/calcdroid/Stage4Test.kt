package io.github.engsoft7.calcdroid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.tan

class Stage4Test {

    @Test
    fun raizesDeUmPolinomio() {
        val roots = findRoots({ it * it - 4 }, -10.0, 10.0)
        assertEquals(2, roots.size)
        assertEquals(-2.0, roots[0], 1e-9)
        assertEquals(2.0, roots[1], 1e-9)
    }

    @Test
    fun raizesDoSeno() {
        val roots = findRoots({ sin(it) }, -10.0, 10.0)
        // -3π, -2π, -π, 0, π, 2π, 3π
        assertEquals(7, roots.size)
        assertEquals(PI, roots[4], 1e-9)
    }

    @Test
    fun raizNoPontoDaGrade() {
        // x = 0 cai exatamente numa amostra; não pode sair repetido.
        val roots = findRoots({ it }, -10.0, 10.0)
        assertEquals(listOf(0.0), roots)
    }

    @Test
    fun assintotaNaoEhRaiz() {
        assertTrue(findRoots({ 1 / it }, -10.0, 10.0).isEmpty())
        val roots = findRoots({ tan(it) }, -2.0, 2.0)
        assertEquals(1, roots.size)
        assertEquals(0.0, roots[0], 1e-9)
    }

    @Test
    fun intersecaoDeDuasFuncoes() {
        // x² = x + 2  →  x = -1 e x = 2
        val roots = findRoots({ it * it - (it + 2) }, -10.0, 10.0)
        assertEquals(-1.0, roots[0], 1e-9)
        assertEquals(2.0, roots[1], 1e-9)
    }

    @Test
    fun listaDeNumeros() {
        assertEquals(listOf(1.5, 2.0, 3.0), parseNumberList("1,5; 2\n3", ','))
        assertEquals(listOf(1.5, 2.0, 3.0), parseNumberList("1.5, 2 3", '.'))
        assertNull(parseNumberList("1 dois 3", ','))
        assertEquals(emptyList<Double>(), parseNumberList("  ", ','))
    }

    @Test
    fun estatisticas() {
        val s = stats(listOf(2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0))!!
        assertEquals(8, s.count)
        assertEquals(40.0, s.sum, 0.0)
        assertEquals(5.0, s.mean, 0.0)
        assertEquals(4.5, s.median, 0.0)
        assertEquals(2.0, s.stdDevPopulation, 1e-12)
        assertEquals(2.138089935, s.stdDevSample, 1e-9)
        assertEquals(3.0, stats(listOf(3.0, 1.0, 5.0))!!.median, 0.0)
        assertTrue(stats(listOf(1.0))!!.stdDevSample.isNaN())
        assertNull(stats(emptyList()))
    }

    @Test
    fun regressaoLinear() {
        val r = linearRegression(listOf(1.0, 2.0, 3.0, 4.0), listOf(3.0, 5.0, 7.0, 9.0))!!
        assertEquals(1.0, r.intercept, 1e-12)
        assertEquals(2.0, r.slope, 1e-12)
        assertEquals(1.0, r.r, 1e-12)
        assertNull(linearRegression(listOf(1.0, 1.0), listOf(2.0, 3.0)))
        assertNull(linearRegression(listOf(1.0, 2.0), listOf(2.0)))
    }

    @Test
    fun jurosCompostos() {
        assertEquals(1104.6221254, Finance.compound(1000.0, 1.0, 10.0), 1e-6)
        // Aportes de 100 por 12 meses a 1%: 1268,25
        assertEquals(1268.2503013, Finance.compound(0.0, 1.0, 12.0, 100.0), 1e-6)
        assertEquals(1500.0, Finance.compound(300.0, 0.0, 12.0, 100.0), 0.0)
    }

    @Test
    fun parcelas() {
        // 10.000 em 12x a 2% ao mês: 945,60
        assertEquals(945.5960, Finance.installment(10000.0, 2.0, 12), 1e-4)
        assertEquals(250.0, Finance.installment(1000.0, 0.0, 4), 0.0)
    }

    @Test
    fun variacaoPercentual() {
        assertEquals(25.0, Finance.percentChange(80.0, 100.0), 1e-12)
        assertEquals(-20.0, Finance.percentChange(100.0, 80.0), 1e-12)
        assertTrue(Finance.percentChange(0.0, 5.0).isNaN())
    }
}
