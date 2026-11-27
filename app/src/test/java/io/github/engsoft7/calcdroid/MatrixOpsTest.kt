package io.github.engsoft7.calcdroid

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MatrixOpsTest {

    private fun m(vararg rows: DoubleArray) = arrayOf(*rows)
    private fun row(vararg v: Double) = v

    private fun assertMatrix(expected: Array<DoubleArray>, actual: Array<DoubleArray>) {
        assertEquals(expected.size, actual.size)
        expected.indices.forEach { assertArrayEquals(expected[it], actual[it], 1e-9) }
    }

    private val a = m(row(1.0, 2.0), row(3.0, 4.0))
    private val b = m(row(5.0, 6.0), row(7.0, 8.0))

    @Test
    fun somaSubtracaoMultiplicacao() {
        assertMatrix(m(row(6.0, 8.0), row(10.0, 12.0)), MatrixOps.add(a, b))
        assertMatrix(m(row(-4.0, -4.0), row(-4.0, -4.0)), MatrixOps.subtract(a, b))
        assertMatrix(m(row(19.0, 22.0), row(43.0, 50.0)), MatrixOps.multiply(a, b))
    }

    @Test
    fun transposta() {
        assertMatrix(m(row(1.0, 3.0), row(2.0, 4.0)), MatrixOps.transpose(a))
    }

    @Test
    fun determinante() {
        assertEquals(-2.0, MatrixOps.determinant(a), 1e-12)
        val c = m(row(2.0, 0.0, 1.0), row(1.0, 3.0, 2.0), row(1.0, 1.0, 2.0))
        assertEquals(6.0, MatrixOps.determinant(c), 1e-12)
        // Pivô zero na primeira posição exige troca de linhas.
        assertEquals(-1.0, MatrixOps.determinant(m(row(0.0, 1.0), row(1.0, 0.0))), 1e-12)
    }

    @Test
    fun inversaVezesOriginalDaIdentidade() {
        val c = m(row(2.0, 0.0, 1.0), row(1.0, 3.0, 2.0), row(1.0, 1.0, 2.0))
        val inv = MatrixOps.inverse(c)!!
        val identity = m(row(1.0, 0.0, 0.0), row(0.0, 1.0, 0.0), row(0.0, 0.0, 1.0))
        assertMatrix(identity, MatrixOps.multiply(c, inv))
    }

    @Test
    fun singularNaoTemInversa() {
        assertNull(MatrixOps.inverse(m(row(1.0, 2.0), row(2.0, 4.0))))
        assertEquals(0.0, MatrixOps.determinant(m(row(1.0, 2.0), row(2.0, 4.0))), 0.0)
    }
}
