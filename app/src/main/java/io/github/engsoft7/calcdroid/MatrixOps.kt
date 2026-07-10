package io.github.engsoft7.calcdroid

import kotlin.math.abs

/**
 * Operações com matrizes quadradas (2×2 e 3×3 no app, mas o código é
 * genérico). Determinante e inversa usam eliminação de Gauss com pivô
 * parcial, estável o suficiente para os tamanhos usados aqui.
 */
object MatrixOps {

    fun add(a: Array<DoubleArray>, b: Array<DoubleArray>): Array<DoubleArray> =
        Array(a.size) { i -> DoubleArray(a.size) { j -> a[i][j] + b[i][j] } }

    fun subtract(a: Array<DoubleArray>, b: Array<DoubleArray>): Array<DoubleArray> =
        Array(a.size) { i -> DoubleArray(a.size) { j -> a[i][j] - b[i][j] } }

    fun multiply(a: Array<DoubleArray>, b: Array<DoubleArray>): Array<DoubleArray> {
        val n = a.size
        return Array(n) { i ->
            DoubleArray(n) { j ->
                var sum = 0.0
                for (k in 0 until n) sum += a[i][k] * b[k][j]
                sum
            }
        }
    }

    fun transpose(a: Array<DoubleArray>): Array<DoubleArray> =
        Array(a.size) { i -> DoubleArray(a.size) { j -> a[j][i] } }

    fun determinant(matrix: Array<DoubleArray>): Double {
        val n = matrix.size
        val m = Array(n) { matrix[it].clone() }
        var det = 1.0
        for (col in 0 until n) {
            var pivot = col
            for (row in col + 1 until n) {
                if (abs(m[row][col]) > abs(m[pivot][col])) pivot = row
            }
            if (abs(m[pivot][col]) < 1e-12) return 0.0
            if (pivot != col) {
                val tmp = m[pivot]; m[pivot] = m[col]; m[col] = tmp
                det = -det
            }
            det *= m[col][col]
            for (row in col + 1 until n) {
                val factor = m[row][col] / m[col][col]
                for (j in col until n) m[row][j] -= factor * m[col][j]
            }
        }
        return det
    }

    /** Retorna null quando a matriz é singular (determinante zero). */
    fun inverse(matrix: Array<DoubleArray>): Array<DoubleArray>? {
        val n = matrix.size
        // Matriz aumentada [A | I], reduzida por Gauss-Jordan até [I | A⁻¹]
        val m = Array(n) { i ->
            DoubleArray(2 * n) { j ->
                when {
                    j < n -> matrix[i][j]
                    j - n == i -> 1.0
                    else -> 0.0
                }
            }
        }
        for (col in 0 until n) {
            var pivot = col
            for (row in col + 1 until n) {
                if (abs(m[row][col]) > abs(m[pivot][col])) pivot = row
            }
            if (abs(m[pivot][col]) < 1e-12) return null
            if (pivot != col) {
                val tmp = m[pivot]; m[pivot] = m[col]; m[col] = tmp
            }
            val p = m[col][col]
            for (j in 0 until 2 * n) m[col][j] /= p
            for (row in 0 until n) {
                if (row == col) continue
                val factor = m[row][col]
                if (factor == 0.0) continue
                for (j in 0 until 2 * n) m[row][j] -= factor * m[col][j]
            }
        }
        return Array(n) { i -> DoubleArray(n) { j -> m[i][j + n] } }
    }
}
