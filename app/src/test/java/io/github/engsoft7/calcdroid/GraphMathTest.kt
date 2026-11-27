package io.github.engsoft7.calcdroid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.tan

class GraphMathTest {

    private val xs = (0..300).map { -10.0 + it * 20.0 / 300 }

    @Test
    fun funcaoAcimaDeCemContinuaVisivel() {
        // Antes a faixa era limitada a ±100 e sempre incluía o zero, então
        // x+500 (490..510) ficava inteira fora da tela.
        val (lo, hi) = graphYRange(xs.map { it + 500 })
        assertTrue("lo=$lo", lo in 480.0..490.0)
        assertTrue("hi=$hi", hi in 510.0..520.0)
    }

    @Test
    fun parabolaCabeInteira() {
        val (lo, hi) = graphYRange(xs.map { it * it })
        assertTrue(lo < 0.0) // eixo x visível
        assertTrue(hi >= 100.0) // pontas da parábola visíveis
    }

    @Test
    fun assintotaNaoAchataACurva() {
        // tan em radianos explode perto de ±π/2; a escala deve seguir o miolo.
        val ys = xs.map { tan(it) }
        val (lo, hi) = graphYRange(ys)
        assertTrue("lo=$lo", lo > -100)
        assertTrue("hi=$hi", hi < 100)
        val (lo2, hi2) = graphYRange(xs.map { if (it == 0.0) Double.POSITIVE_INFINITY else 1 / it })
        assertTrue(hi2 - lo2 < 10)
        assertTrue(lo2 < -1 && hi2 > 1)
    }

    @Test
    fun constanteGanhaMargem() {
        val (lo, hi) = graphYRange(xs.map { 3.0 })
        assertTrue(lo < 3.0 && hi > 3.0)
        assertEquals(2.0 * 1.16, hi - lo, 1e-9)
    }

    @Test
    fun semValoresUsaFaixaPadrao() {
        assertEquals(-5.0 to 5.0, graphYRange(listOf(Double.NaN)))
        assertEquals(-5.0 to 5.0, graphYRange(emptyList()))
    }

    @Test
    fun rotulosCurtos() {
        assertEquals("108", axisLabel(108.4))
        assertEquals("0.174", axisLabel(0.17364))
        assertEquals("-2.5", axisLabel(-2.5))
        assertEquals("1.23E+7", axisLabel(12345678.0))
    }
}
