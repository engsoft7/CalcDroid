package io.github.engsoft7.calcdroid

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Stage2Test {

    private fun unit(cat: String, id: String) = UnitConverter.category(cat).units.first { it.id == id }
    private fun conv(cat: String, v: Double, from: String, to: String) =
        UnitConverter.convert(v, unit(cat, from), unit(cat, to))

    @Test
    fun conversoesDeUnidade() {
        assertEquals(0.621371, conv("length", 1.0, "km", "mi"), 1e-6)
        assertEquals(30.48, conv("length", 1.0, "ft", "cm"), 1e-9)
        assertEquals(2.20462, conv("mass", 1.0, "kg", "lb"), 1e-5)
        assertEquals(212.0, conv("temperature", 100.0, "c", "f"), 1e-9)
        assertEquals(-40.0, conv("temperature", -40.0, "f", "c"), 1e-9)
        assertEquals(273.15, conv("temperature", 0.0, "c", "k"), 1e-9)
        assertEquals(10000.0, conv("area", 1.0, "ha", "m2"), 1e-9)
        assertEquals(3.785411784, conv("volume", 1.0, "gal", "l"), 1e-9)
        assertEquals(27.7778, conv("speed", 100.0, "kmh", "ms"), 1e-4)
        assertEquals(1440.0, conv("time", 1.0, "d", "min"), 1e-9)
        assertEquals(1024.0, conv("data", 1.0, "MiB", "KiB"), 1e-9)
        assertEquals(8.0, conv("data", 1.0, "B", "b"), 1e-9)
    }

    @Test
    fun todasAsCategoriasVoltamAoValor() {
        for (cat in UnitConverter.categories) {
            for (a in cat.units) for (b in cat.units) {
                val back = UnitConverter.convert(UnitConverter.convert(12.5, a, b), b, a)
                assertEquals("${cat.id} ${a.id}->${b.id}", 12.5, back, 1e-9)
            }
            assertTrue("${cat.id} precisa de 2+ unidades", cat.units.size >= 2)
        }
    }

    @Test
    fun basesNumericas() {
        assertEquals("FF", UnitConverter.convertBase("255", 10, 16))
        assertEquals("11111111", UnitConverter.convertBase("FF", 16, 2))
        assertEquals("-101", UnitConverter.convertBase("-5", 10, 2))
        assertEquals("777", UnitConverter.convertBase("511", 10, 8))
        assertNull(UnitConverter.convertBase("12", 2, 10))
        assertNull(UnitConverter.convertBase("", 10, 2))
        assertTrue(UnitConverter.isValidDigit('F', 16))
        assertFalse(UnitConverter.isValidDigit('2', 2))
    }

    @Test
    fun tecladoDoConversor() {
        assertEquals("5", converterInput("0", "5", null))
        assertEquals("1.", converterInput("1", ".", null))
        assertEquals("1.", converterInput("1.", ".", null))
        assertEquals("-12", converterInput("12", "±", null))
        assertEquals("12", converterInput("-12", "±", null))
        assertEquals("0", converterInput("7", "⌫", null))
        assertEquals("0", converterInput("-7", "⌫", null))
        assertEquals("0", converterInput("123", "AC", null))
        // Bases: só dígitos válidos e sem ponto.
        assertEquals("1", converterInput("1", "2", 2))
        assertEquals("1F", converterInput("1", "F", 16))
        assertEquals("1", converterInput("1", ".", 16))
    }

    @Test
    fun sistemaLinear() {
        val a = arrayOf(doubleArrayOf(2.0, 1.0, -1.0), doubleArrayOf(-3.0, -1.0, 2.0), doubleArrayOf(-2.0, 1.0, 2.0))
        val x = MatrixOps.solve(a, doubleArrayOf(8.0, -11.0, -3.0))!!
        assertArrayEquals(doubleArrayOf(2.0, 3.0, -1.0), x, 1e-9)
        val four = Array(4) { i -> DoubleArray(4) { j -> if (i == j) 2.0 else 0.0 } }
        assertArrayEquals(doubleArrayOf(1.0, 2.0, 3.0, 4.0), MatrixOps.solve(four, doubleArrayOf(2.0, 4.0, 6.0, 8.0))!!, 1e-12)
        assertNull(MatrixOps.solve(arrayOf(doubleArrayOf(1.0, 2.0), doubleArrayOf(2.0, 4.0)), doubleArrayOf(1.0, 2.0)))
    }

    @Test
    fun passoDaGrade() {
        assertEquals(2.0, niceStep(20.0), 1e-12) // -10..10: 20/6 ≈ 3,3 → passo 2, como a grade antiga
        assertEquals(0.2, niceStep(1.0), 1e-12)
        assertEquals(200.0, niceStep(1000.0), 1e-9)
        assertEquals(1.0, niceStep(Double.NaN), 0.0)
    }

    @Test
    fun zoomEArrastoDoGrafico() {
        val v = Viewport()
        // Zoom 2× no centro: a faixa x cai pela metade em torno do meio.
        val z = v.transformed(-5.0, 5.0, fx = 0.5, fy = 0.5, zoom = 2.0, dxFrac = 0.0, dyFrac = 0.0)
        assertEquals(-5.0, z.xMin, 1e-9)
        assertEquals(5.0, z.xMax, 1e-9)
        assertEquals(-2.5, z.yMin, 1e-9)
        assertEquals(2.5, z.yMax, 1e-9)
        assertFalse(z.autoY)
        // Arrastar meio gráfico para a direita mostra x menores.
        val p = v.transformed(-5.0, 5.0, fx = 0.5, fy = 0.5, zoom = 1.0, dxFrac = 0.5, dyFrac = 0.0)
        assertEquals(-20.0, p.xMin, 1e-9)
        assertEquals(0.0, p.xMax, 1e-9)
        assertTrue(Viewport().isDefault)
        assertFalse(p.isDefault)
    }

    @Test
    fun avaliacaoSoDepoisDeUsoDeVerdade() {
        assertFalse(shouldAskReview(1))
        assertFalse(shouldAskReview(REVIEW_AFTER - 1))
        assertTrue(shouldAskReview(REVIEW_AFTER))
    }

    @Test
    fun separadorDecimalDoIdioma() {
        assertEquals("1.5×2", prettyExpression("1.5*2", '.'))
        assertEquals("1,5×2", prettyExpression("1.5*2", ','))
        assertEquals("3.14", prettyNumber("3.14", '.'))
    }
}
