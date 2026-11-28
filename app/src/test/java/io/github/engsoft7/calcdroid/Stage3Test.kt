package io.github.engsoft7.calcdroid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class Stage3Test {

    @Test
    fun resultadoAoVivo() {
        assertEquals("5", CalcState(expression = "2+3").preview())
        assertEquals("0.5", CalcState(expression = "sin(30").preview())   // parêntese ainda aberto
        assertNull(CalcState(expression = "2+").preview())                 // conta incompleta
        assertNull(CalcState(expression = "42").preview())                 // número sozinho
        assertNull(CalcState(expression = "-1.5E+12").preview())
        assertNull(CalcState(expression = "5", justEvaluated = true).preview())
        assertNull(CalcState(expression = "5/0").preview())                // erro não aparece
        assertNull(CalcState().preview())
    }

    @Test
    fun colarContas() {
        assertEquals("2*3", sanitizePasted("2 × 3"))
        assertEquals("10/4-1", sanitizePasted("10 ÷ 4 − 1"))
        assertEquals("1234.5", sanitizePasted("1.234,5", ','))
        assertEquals("1234.5", sanitizePasted("1,234.5", '.'))
        assertEquals("12.5", sanitizePasted("12,5", ','))
        assertEquals("12.5", sanitizePasted("12.5", ','))   // ponto decimal num celular em pt
        assertEquals("12500", sanitizePasted("12.500", ','))  // milhar
        assertEquals("1234567", sanitizePasted("1,234,567", '.'))
        assertEquals("2.75", sanitizePasted("2,75", '.'))
        assertNull(sanitizePasted("olá"))
        assertNull(sanitizePasted(""))
        assertNull(sanitizePasted("2++"))
        assertNull(sanitizePasted("rm -rf"))
    }

    @Test
    fun colarNoVisor() {
        assertEquals("2+5", CalcState(expression = "2+").paste("5").expression)
        assertEquals("7", CalcState(expression = "9", justEvaluated = true, lastExpression = "4+5 =").paste("7").expression)
        assertEquals("", CalcState(expression = "9", justEvaluated = true).paste("7").lastExpression)
    }

    @Test
    fun fracoes() {
        assertEquals("1/3", toFraction(1.0 / 3))
        assertEquals("-3/4", toFraction(-0.75))
        assertEquals("22/7", toFraction(22.0 / 7))
        assertEquals("5/2", toFraction(2.5))
        assertNull(toFraction(3.0))
        assertNull(toFraction(Math.PI))
        assertNull(toFraction(Double.NaN))
        assertNull(toFraction(1e12 + 0.5))
    }

    private fun conv(cat: String, v: Double, from: String, to: String): Double {
        val units = UnitConverter.category(cat).units
        return UnitConverter.convert(v, units.first { it.id == from }, units.first { it.id == to })
    }

    @Test
    fun novasCategoriasDoConversor() {
        assertEquals(1.01325, conv("pressure", 1.0, "atm", "bar"), 1e-9)
        assertEquals(14.6959, conv("pressure", 1.0, "atm", "psi"), 1e-4)
        assertEquals(4.184, conv("energy", 1.0, "kcal", "kj"), 1e-9)
        assertEquals(3600.0, conv("energy", 1.0, "kwh", "kj"), 1e-9)
        assertEquals(1.0139, conv("power", 1.0, "hp", "cv"), 1e-4)
        assertEquals(Math.PI, conv("angle", 180.0, "deg", "rad"), 1e-12)
        assertEquals(400.0, conv("angle", 1.0, "turn", "grad"), 1e-9)
        // 10 km/L = 10 L/100 km; 8 L/100 km = 12,5 km/L; 30 mpg (US) ≈ 12,75 km/L
        assertEquals(10.0, conv("fuel", 10.0, "kml", "l100"), 1e-9)
        assertEquals(12.5, conv("fuel", 8.0, "l100", "kml"), 1e-9)
        assertEquals(12.754, conv("fuel", 30.0, "mpgus", "kml"), 1e-3)
    }
}
