package io.github.engsoft7.calcdroid

import android.content.Context

/** Uma conta já feita: o que foi digitado e o resultado. */
data class HistoryEntry(val expression: String, val result: String)

const val HISTORY_LIMIT = 50

/**
 * Histórico em texto simples, uma conta por linha ("expressão\tresultado").
 * Expressões e resultados nunca têm tab nem quebra de linha, porque só
 * vêm do teclado do app.
 */
fun encodeHistory(entries: List<HistoryEntry>): String =
    entries.take(HISTORY_LIMIT).joinToString("\n") { "${it.expression}\t${it.result}" }

fun decodeHistory(text: String): List<HistoryEntry> =
    text.lineSequence()
        .mapNotNull { line ->
            val parts = line.split('\t')
            if (parts.size == 2 && parts[0].isNotEmpty()) HistoryEntry(parts[0], parts[1]) else null
        }
        .take(HISTORY_LIMIT)
        .toList()

/** Novo item no topo, sem repetir a mesma conta logo em seguida. */
fun addToHistory(entries: List<HistoryEntry>, entry: HistoryEntry): List<HistoryEntry> =
    if (entries.firstOrNull() == entry) entries else (listOf(entry) + entries).take(HISTORY_LIMIT)

/** O que sobrevive a fechar o app: histórico, memória e graus/radianos. */
class CalcPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("calcdroid", Context.MODE_PRIVATE)

    var history: List<HistoryEntry>
        get() = decodeHistory(prefs.getString("history", "") ?: "")
        set(value) = prefs.edit().putString("history", encodeHistory(value)).apply()

    var memory: Double
        get() = prefs.getString("memory", null)?.toDoubleOrNull() ?: 0.0
        set(value) = prefs.edit().putString("memory", value.toString()).apply()

    var degrees: Boolean
        get() = prefs.getBoolean("degrees", true)
        set(value) = prefs.edit().putBoolean("degrees", value).apply()

    /**
     * Conta as contas feitas e diz quando pedir a avaliação na Play: uma
     * única vez, depois de [REVIEW_AFTER] resultados (uso de verdade, não
     * logo na primeira abertura).
     */
    fun countCalculationAndCheckReview(): Boolean {
        if (prefs.getBoolean("review_asked", false)) return false
        val count = prefs.getInt("calculations", 0) + 1
        val ask = shouldAskReview(count)
        prefs.edit().putInt("calculations", count).putBoolean("review_asked", ask).apply()
        return ask
    }
}

const val REVIEW_AFTER = 25

fun shouldAskReview(calculations: Int): Boolean = calculations >= REVIEW_AFTER
