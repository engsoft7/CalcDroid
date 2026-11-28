package io.github.engsoft7.calcdroid

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.engsoft7.calcdroid.ui.theme.CalcColors

private const val MAX = 4

// Células guardadas sempre em 4×4 (índice i*4+j); os tamanhos menores usam o
// canto superior esquerdo, preservando os valores ao trocar de tamanho.
private val CellSaver = listSaver<SnapshotStateList<String>, String>(
    save = { it.toList() },
    restore = { it.toMutableStateList() }
)

// Resultado: primeiro o número de colunas, depois as células linha a linha.
private val ResultSaver = listSaver<List<List<String>>?, String>(
    save = { r -> if (r == null) emptyList() else listOf(r.first().size.toString()) + r.flatten() },
    restore = { flat ->
        if (flat.isEmpty()) null else flat.drop(1).chunked(flat.first().toInt())
    }
)

@Composable
fun MatrixScreen(colors: CalcColors, decimalSep: Char, modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    val resources = LocalResources.current
    var size by rememberSaveable { mutableStateOf(2) }
    val cellsA = rememberSaveable(saver = CellSaver) { MutableList(MAX * MAX) { "0" }.toMutableStateList() }
    val cellsB = rememberSaveable(saver = CellSaver) { MutableList(MAX * MAX) { "0" }.toMutableStateList() }
    val vectorB = rememberSaveable(saver = CellSaver) { MutableList(MAX) { "0" }.toMutableStateList() }
    var result by rememberSaveable(stateSaver = ResultSaver) { mutableStateOf<List<List<String>>?>(null) }
    var resultTitle by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf<String?>(null) }

    val nameA = stringResource(R.string.matrix_a)
    val nameB = stringResource(R.string.matrix_b)
    val nameVec = stringResource(R.string.vector_b)

    fun show(value: Double) = prettyNumber(formatNumber(value), decimalSep)

    fun run(op: String) {
        val n = size
        fun parse(cells: List<String>): Array<DoubleArray>? {
            val m = Array(n) { DoubleArray(n) }
            for (i in 0 until n) for (j in 0 until n) {
                m[i][j] = cells[i * MAX + j].toDoubleOrNull() ?: return null
            }
            return m
        }
        fun toDisplay(m: Array<DoubleArray>) = m.map { row -> row.map { show(it) } }

        result = null
        message = null
        resultTitle = resources.getString(R.string.result)
        val a = parse(cellsA) ?: run {
            message = resources.getString(R.string.matrix_invalid, nameA)
            return
        }
        when (op) {
            "A+B", "A−B", "A×B" -> {
                val b = parse(cellsB) ?: run {
                    message = resources.getString(R.string.matrix_invalid, nameB)
                    return
                }
                result = toDisplay(
                    when (op) {
                        "A+B" -> MatrixOps.add(a, b)
                        "A−B" -> MatrixOps.subtract(a, b)
                        else -> MatrixOps.multiply(a, b)
                    }
                )
            }
            "det A" -> message = "det A = ${show(MatrixOps.determinant(a))}"
            "A⁻¹" -> {
                val inv = MatrixOps.inverse(a)
                if (inv == null) message = resources.getString(R.string.matrix_singular) else result = toDisplay(inv)
            }
            "Aᵀ" -> result = toDisplay(MatrixOps.transpose(a))
            "A·x=b" -> {
                val b = DoubleArray(n)
                for (i in 0 until n) {
                    b[i] = vectorB[i].toDoubleOrNull() ?: run {
                        message = resources.getString(R.string.matrix_invalid, nameVec)
                        return
                    }
                }
                val x = MatrixOps.solve(a, b)
                if (x == null) {
                    message = resources.getString(R.string.system_singular)
                } else {
                    resultTitle = resources.getString(R.string.system_solution)
                    result = x.mapIndexed { i, v -> listOf("x${subscript(i + 1)} = ${show(v)}") }
                }
            }
        }
    }

    // Depois de uma operação, rola até o resultado, que fica abaixo dos
    // botões e saía da tela sem ninguém perceber.
    val scroll = rememberScrollState()
    LaunchedEffect(result, message) {
        if (result != null || message != null) {
            // Espera o resultado entrar no layout; antes disso o limite de
            // rolagem ainda é o antigo.
            withFrameNanos { }
            scroll.animateScrollTo(scroll.maxValue)
        }
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.matrix_size), fontSize = 14.sp, color = colors.secondaryText)
            (2..MAX).forEach { n ->
                val selected = size == n
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) colors.functionKey else colors.chip)
                        .selectable(selected = selected, role = Role.RadioButton, onClick = {
                            size = n
                            result = null
                            message = null
                        })
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        "${n}×${n}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (selected) colors.keyText else colors.chipText
                    )
                }
            }
        }

        MatrixGrid(nameA, size, cellsA, colors, decimalSep)
        MatrixGrid(nameB, size, cellsB, colors, decimalSep)
        // Vetor b do sistema A·x = b, deitado para economizar espaço.
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(nameVec, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.secondaryText)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (i in 0 until size) {
                    val desc = stringResource(R.string.vector_cell_desc, nameVec, i + 1)
                    MatrixCell(
                        value = vectorB[i],
                        onChange = { vectorB[i] = it },
                        colors = colors,
                        decimalSep = decimalSep,
                        modifier = Modifier
                            .weight(1f)
                            .semantics { contentDescription = desc }
                    )
                }
            }
        }

        listOf(
            listOf("A+B", "A−B", "A×B"),
            listOf("det A", "A⁻¹", "Aᵀ"),
            listOf("A·x=b"),
        ).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { op ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .shadow(3.dp, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.operatorKey)
                            .clickable(role = Role.Button) {
                                haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                                run(op)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(op, fontSize = 17.sp, fontWeight = FontWeight.Medium, color = colors.keyText)
                    }
                }
            }
        }

        message?.let { Text(it, fontSize = 20.sp, fontWeight = FontWeight.Medium, color = colors.text) }

        result?.let { rows ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(resultTitle, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.secondaryText)
                rows.forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { cell ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.card)
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    cell,
                                    // Valores longos (dízimas da inversa) usam fonte menor.
                                    fontSize = if (cell.length > 8) 12.sp else 16.sp,
                                    color = colors.text,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun subscript(n: Int): String = n.toString().map { "₀₁₂₃₄₅₆₇₈₉"[it - '0'] }.joinToString("")

@Composable
private fun MatrixGrid(label: String, size: Int, cells: MutableList<String>, colors: CalcColors, decimalSep: Char) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.secondaryText)
        for (i in 0 until size) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (j in 0 until size) {
                    val index = i * MAX + j
                    val desc = stringResource(R.string.matrix_cell_desc, label, i + 1, j + 1)
                    MatrixCell(
                        value = cells[index],
                        onChange = { cells[index] = it },
                        colors = colors,
                        decimalSep = decimalSep,
                        modifier = Modifier
                            .weight(1f)
                            .semantics { contentDescription = desc }
                    )
                }
            }
        }
    }
}

@Composable
private fun MatrixCell(
    value: String,
    onChange: (String) -> Unit,
    colors: CalcColors,
    decimalSep: Char,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = { raw ->
            // O teclado numérico de alguns idiomas insere vírgula como
            // separador decimal; normaliza para ponto antes de validar.
            val new = raw.replace(',', '.')
            val validChars = new.all { it.isDigit() || it == '.' || it == '-' }
            val singleDot = new.count { it == '.' } <= 1
            // Sinal negativo só no início, e no máximo um
            val minusOk = !new.contains('-') || (new.startsWith("-") && new.count { it == '-' } == 1)
            if (new.length <= 8 && validChars && singleDot && minusOk) onChange(new)
        },
        textStyle = TextStyle(fontSize = 18.sp, textAlign = TextAlign.Center, color = colors.text),
        cursorBrush = SolidColor(colors.accent),
        // Mostra o ponto guardado na célula com o separador do idioma.
        visualTransformation = {
            TransformedText(AnnotatedString(it.text.replace('.', decimalSep)), OffsetMapping.Identity)
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = modifier
            .shadow(2.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(colors.card)
            .padding(vertical = 12.dp, horizontal = 4.dp)
    )
}
