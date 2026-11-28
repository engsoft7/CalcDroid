package io.github.engsoft7.calcdroid

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.engsoft7.calcdroid.ui.theme.CalcColors
import java.text.NumberFormat

/** Campo com rótulo em cima; [numeric] abre o teclado numérico. */
@Composable
private fun LabeledField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    colors: CalcColors,
    numeric: Boolean = true,
    hint: String? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.secondaryText)
        BasicTextField(
            value = value,
            onValueChange = { if (it.length <= if (numeric) 16 else 2000) onChange(it) },
            textStyle = TextStyle(fontSize = 18.sp, color = colors.text),
            cursorBrush = SolidColor(colors.accent),
            keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text),
            singleLine = numeric,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (numeric) 0.dp else 72.dp)
                .semantics { contentDescription = label }
                .shadow(2.dp, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(colors.card)
                .padding(12.dp)
        )
        if (hint != null) Text(hint, fontSize = 12.sp, color = colors.secondaryText)
    }
}

/** Cartão de resultados: rótulo à esquerda, valor à direita. */
@Composable
private fun ResultCard(rows: List<Pair<String, String>>, colors: CalcColors, message: String? = null) {
    if (rows.isEmpty() && message == null) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(colors.card)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (message != null) Text(message, color = colors.secondaryText, fontSize = 15.sp)
        rows.forEach { (label, value) ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(label, Modifier.weight(1f), color = colors.secondaryText, fontSize = 15.sp)
                Text(value, color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

/**
 * Número digitado num campo. Aceita o separador de milhar do idioma
 * ("1,000.50" em inglês, "1.000,50" em português) e, sem ele, tanto "," quanto
 * "." como decimal (o teclado numérico de alguns aparelhos só tem o ponto).
 */
fun parseField(text: String, decimalSep: Char = ','): Double? {
    var t = text.trim().replace(" ", "").replace("\u00A0", "")
    t = if (decimalSep == '.') t.replace(",", "")
    else if (t.contains('.') && t.contains(',')) t.replace(".", "").replace(',', '.')
    else t.replace(',', '.')
    return t.toDoubleOrNull()?.takeIf { it.isFinite() }
}

@Composable
fun StatsScreen(colors: CalcColors, decimalSep: Char, modifier: Modifier = Modifier) {
    var xs by rememberSaveable { mutableStateOf("") }
    var ys by rememberSaveable { mutableStateOf("") }
    fun show(v: Double) = if (v.isFinite()) prettyNumber(formatNumber(v), decimalSep) else "—"

    val xValues = parseNumberList(xs, decimalSep)
    val yValues = parseNumberList(ys, decimalSep)
    val invalid = stringResource(R.string.converter_invalid)
    val mismatch = stringResource(R.string.stats_size_mismatch)
    val rows = mutableListOf<Pair<String, String>>()
    var message: String? = null
    val s = xValues?.let { stats(it) }
    if (xValues == null || yValues == null) {
        message = invalid
    } else if (s != null) {
        rows += stringResource(R.string.stats_count) to s.count.toString()
        rows += stringResource(R.string.stats_sum) to show(s.sum)
        rows += stringResource(R.string.stats_mean) to show(s.mean)
        rows += stringResource(R.string.stats_median) to show(s.median)
        rows += stringResource(R.string.stats_min) to show(s.min)
        rows += stringResource(R.string.stats_max) to show(s.max)
        rows += stringResource(R.string.stats_sd_pop) to show(s.stdDevPopulation)
        rows += stringResource(R.string.stats_sd_sample) to show(s.stdDevSample)
        if (yValues.isNotEmpty()) {
            val reg = linearRegression(xValues, yValues)
            if (yValues.size != xValues.size) {
                message = mismatch
            } else if (reg != null) {
                val sign = if (reg.slope < 0) "−" else "+"
                rows += stringResource(R.string.stats_regression) to
                    "y = ${show(reg.intercept)} $sign ${show(kotlin.math.abs(reg.slope))}·x"
                rows += stringResource(R.string.stats_r) to show(reg.r)
            }
        }
    }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LabeledField(
            stringResource(R.string.stats_values), xs, { xs = it }, colors,
            numeric = false, hint = stringResource(R.string.stats_hint)
        )
        LabeledField(stringResource(R.string.stats_values_y), ys, { ys = it }, colors, numeric = false)
        ResultCard(rows, colors, message)
    }
}

private const val FIN_COMPOUND = "compound"
private const val FIN_INSTALLMENTS = "installments"
private const val FIN_PERCENT = "percent"

@Composable
fun FinanceScreen(colors: CalcColors, decimalSep: Char, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val money = remember(locale) {
        NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }
    fun show(v: Double) = if (v.isFinite()) money.format(v) else "—"
    fun showPercent(v: Double) = if (v.isFinite()) money.format(v) + "%" else "—"

    var tab by rememberSaveable { mutableStateOf(FIN_COMPOUND) }
    // Campos de cada cálculo (guardados ao trocar de aba ou girar a tela).
    var principal by rememberSaveable { mutableStateOf("") }
    var rate by rememberSaveable { mutableStateOf("") }
    var periods by rememberSaveable { mutableStateOf("") }
    var deposit by rememberSaveable { mutableStateOf("") }
    var loan by rememberSaveable { mutableStateOf("") }
    var loanRate by rememberSaveable { mutableStateOf("") }
    var count by rememberSaveable { mutableStateOf("") }
    var from by rememberSaveable { mutableStateOf("") }
    var to by rememberSaveable { mutableStateOf("") }

    val tabs = listOf(
        FIN_COMPOUND to stringResource(R.string.fin_compound),
        FIN_INSTALLMENTS to stringResource(R.string.fin_installments),
        FIN_PERCENT to stringResource(R.string.fin_percent),
    )
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEach { (id, label) ->
                val selected = tab == id
                Text(
                    text = label,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) colors.accent else colors.chip)
                        .selectable(selected = selected, role = Role.Tab) { tab = id }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    color = if (selected) colors.keyText else colors.chipText,
                    fontSize = 14.sp
                )
            }
        }
        when (tab) {
            FIN_COMPOUND -> {
                LabeledField(stringResource(R.string.fin_principal), principal, { principal = it }, colors)
                LabeledField(stringResource(R.string.fin_rate), rate, { rate = it }, colors)
                LabeledField(stringResource(R.string.fin_periods), periods, { periods = it }, colors)
                LabeledField(stringResource(R.string.fin_deposit), deposit, { deposit = it }, colors)
                val p = parseField(principal, decimalSep) ?: if (principal.isBlank()) 0.0 else null
                val i = parseField(rate, decimalSep)
                val n = parseField(periods, decimalSep)
                val d = parseField(deposit, decimalSep) ?: if (deposit.isBlank()) 0.0 else null
                if (p != null && i != null && n != null && d != null && n >= 0 && (p != 0.0 || d != 0.0)) {
                    val total = Finance.compound(p, i, n, d)
                    val invested = p + d * n
                    ResultCard(
                        listOf(
                            stringResource(R.string.fin_final_amount) to show(total),
                            stringResource(R.string.fin_invested) to show(invested),
                            stringResource(R.string.fin_interest) to show(total - invested),
                        ),
                        colors
                    )
                }
            }
            FIN_INSTALLMENTS -> {
                LabeledField(stringResource(R.string.fin_loan), loan, { loan = it }, colors)
                LabeledField(stringResource(R.string.fin_rate), loanRate, { loanRate = it }, colors)
                LabeledField(stringResource(R.string.fin_count), count, { count = it }, colors)
                val a = parseField(loan, decimalSep)
                val i = parseField(loanRate, decimalSep)
                val n = parseField(count, decimalSep)?.takeIf { it >= 1 && it <= 1200 && it == kotlin.math.floor(it) }?.toInt()
                if (a != null && i != null && n != null) {
                    val payment = Finance.installment(a, i, n)
                    ResultCard(
                        listOf(
                            stringResource(R.string.fin_payment) to show(payment),
                            stringResource(R.string.fin_total_paid) to show(payment * n),
                            stringResource(R.string.fin_interest) to show(payment * n - a),
                        ),
                        colors
                    )
                }
            }
            else -> {
                LabeledField(stringResource(R.string.fin_from), from, { from = it }, colors)
                LabeledField(stringResource(R.string.fin_to), to, { to = it }, colors)
                val a = parseField(from, decimalSep)
                val b = parseField(to, decimalSep)
                if (a != null && b != null) {
                    ResultCard(
                        listOf(
                            stringResource(R.string.fin_change) to showPercent(Finance.percentChange(a, b)),
                            stringResource(R.string.fin_difference) to show(b - a),
                        ),
                        colors
                    )
                }
            }
        }
    }
}
