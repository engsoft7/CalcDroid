package io.github.engsoft7.calcdroid

import android.content.ClipData
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.engsoft7.calcdroid.ui.theme.CalcColors
import io.github.engsoft7.calcdroid.ui.theme.CalcDroidTheme
import io.github.engsoft7.calcdroid.ui.theme.calcColors
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.sign

const val MODE_BASIC = "basic"
const val MODE_SCIENTIFIC = "sci"
const val MODE_GRAPH = "graph"
const val MODE_MATRIX = "matrix"

// "2nd" troca estas teclas pelas inversas.
private val SECOND_FUNCTIONS = mapOf("sin" to "asin", "cos" to "acos", "tan" to "atan", "√" to "abs")

private val SCIENTIFIC_ROWS = listOf(
    listOf("2nd", "sin", "cos", "tan"),
    listOf("ln", "log", "√", "^"),
    listOf("(", ")", "x²", "x⁻¹"),
    listOf("π", "e", "!", "mod")
)
private val GRAPH_ROWS = listOf(
    listOf("sin", "cos", "tan", "√"),
    listOf("ln", "log", "(", ")"),
    listOf("π", "e", "^", "x")
)
private val BASIC_ROWS = listOf(
    listOf("C", "⌫", "%", "/"),
    listOf("7", "8", "9", "*"),
    listOf("4", "5", "6", "-"),
    listOf("1", "2", "3", "+"),
    listOf("0", ".", "=")
)
private val MEMORY_ROW = listOf("MC", "MR", "M+", "M-")

// Rótulo exibido na tecla quando difere do código usado na expressão.
private val KEY_LABELS = mapOf(
    "*" to "×", "/" to "÷", "." to ",", "M-" to "M−",
    "asin" to "sin⁻¹", "acos" to "cos⁻¹", "atan" to "tan⁻¹", "abs" to "|x|"
)

// O que o TalkBack lê em cada tecla; dígitos são lidos como estão.
private val KEY_DESCRIPTIONS = mapOf(
    "C" to "Limpar",
    "⌫" to "Apagar",
    "%" to "Porcentagem",
    "/" to "Dividir",
    "*" to "Multiplicar",
    "-" to "Menos",
    "+" to "Mais",
    "=" to "Igual",
    "." to "Vírgula decimal",
    "^" to "Potência",
    "√" to "Raiz quadrada",
    "!" to "Fatorial",
    "(" to "Abre parêntese",
    ")" to "Fecha parêntese",
    "π" to "Pi",
    "e" to "Número de Euler",
    "x" to "Variável x",
    "sin" to "Seno",
    "cos" to "Cosseno",
    "tan" to "Tangente",
    "asin" to "Arco seno",
    "acos" to "Arco cosseno",
    "atan" to "Arco tangente",
    "abs" to "Módulo",
    "ln" to "Logaritmo natural",
    "log" to "Logaritmo na base 10",
    "x²" to "Ao quadrado",
    "x⁻¹" to "Inverso",
    "mod" to "Resto da divisão",
    "2nd" to "Segunda função",
    "MC" to "Limpar memória",
    "MR" to "Usar memória",
    "M+" to "Somar à memória",
    "M-" to "Subtrair da memória"
)

private val CalcStateSaver = listSaver<CalcState, Any>(
    save = { listOf(it.expression, it.lastExpression, it.justEvaluated, it.plotExpression, it.memory) },
    restore = {
        CalcState(
            expression = it[0] as String,
            lastExpression = it[1] as String,
            justEvaluated = it[2] as Boolean,
            plotExpression = it[3] as String,
            memory = it[4] as Double
        )
    }
)

/** Números no visor com vírgula decimal (pt-BR). */
fun prettyNumber(text: String): String = text.replace('.', ',')

/**
 * Teclado de paisagem: as teclas científicas viram um bloco à esquerda do
 * teclado básico, alinhado por baixo, para que todas as linhas caibam na
 * altura reduzida da tela deitada. Posições vazias são "".
 */
internal fun sideBySideRows(
    extraRows: List<List<String>>,
    basicRows: List<List<String>>
): List<List<String>> {
    val keys = extraRows.flatten()
    val columns = ceil(keys.size.toDouble() / basicRows.size).toInt()
    val extra = keys.chunked(columns).map { it + List(columns - it.size) { "" } }
    val offset = basicRows.size - extra.size
    val basicColumns = basicRows.maxOf { it.size }
    return basicRows.mapIndexed { i, row ->
        val left = extra.getOrNull(i - offset) ?: List(columns) { "" }
        left + row + List(basicColumns - row.size) { "" }
    }
}

class MainActivity : ComponentActivity() {
    // Não chame enableEdgeToEdge() aqui. Com targetSdk 37 o sistema já aplica
    // edge-to-edge sozinho a partir da API 35, e por dentro aquela função usa
    // setStatusBarColor/setNavigationBarColor/setDecorFitsSystemWindows, que a
    // Play Console sinaliza como APIs descontinuadas. Sem a chamada, o R8
    // remove esse código e o aviso não aparece. Os insets continuam tratados
    // pelo safeDrawingPadding() da tela, e a cor dos ícones das barras vem do
    // tema (res/values-v35 e values-night-v35).
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = CalcPrefs(applicationContext)
        setContent {
            CalcDroidTheme {
                var mode by rememberSaveable { mutableStateOf(MODE_BASIC) }
                BeautifulCalculatorScreen(
                    mode = mode,
                    onModeChange = { mode = it },
                    prefs = prefs
                )
            }
        }
    }
}

@Composable
fun BeautifulCalculatorScreen(
    mode: String = MODE_BASIC,
    onModeChange: (String) -> Unit = {},
    prefs: CalcPrefs? = null
) {
    val colors = calcColors()
    val haptic = LocalHapticFeedback.current

    // ---- Estado da calculadora de expressões (básica/científico/gráfico),
    // preservado ao girar a tela (rememberSaveable). A lógica das teclas fica
    // em CalculatorInput.kt. Memória, graus/radianos e histórico também
    // sobrevivem a fechar o app (CalcPrefs). ----
    var calc by rememberSaveable(stateSaver = CalcStateSaver) {
        mutableStateOf(CalcState(memory = prefs?.memory ?: 0.0))
    }
    var degrees by rememberSaveable { mutableStateOf(prefs?.degrees ?: true) }
    var secondFunctions by rememberSaveable { mutableStateOf(false) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var history by remember { mutableStateOf(prefs?.history ?: emptyList()) }

    LaunchedEffect(calc.memory) { prefs?.memory = calc.memory }
    LaunchedEffect(degrees) { prefs?.degrees = degrees }

    val onKey: (String) -> Unit = { key ->
        haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
        if (key == "2nd") {
            secondFunctions = !secondFunctions
        } else {
            val before = calc
            val after = before.press(key, graphMode = mode == MODE_GRAPH, degrees = degrees)
            if (key == "=" && after.justEvaluated && after.expression != ERROR_TEXT) {
                history = addToHistory(history, HistoryEntry(after.lastExpression.removeSuffix(" ="), after.expression))
                prefs?.history = history
            }
            calc = after
            // A segunda função vale para uma tecla, como nas calculadoras.
            if (key in SECOND_FUNCTIONS.values) secondFunctions = false
        }
    }

    // ---- Estado do modo matriz (armazenado achatado 3×3; o 2×2 usa o
    // canto superior esquerdo, preservando os valores ao trocar o tamanho) ----
    var matrixSize by rememberSaveable { mutableStateOf(2) }
    val cellSaver = listSaver<SnapshotStateList<String>, String>(
        save = { it.toList() },
        restore = { it.toMutableStateList() }
    )
    val cellsA = rememberSaveable(saver = cellSaver) {
        mutableStateListOf("0", "0", "0", "0", "0", "0", "0", "0", "0")
    }
    val cellsB = rememberSaveable(saver = cellSaver) {
        mutableStateListOf("0", "0", "0", "0", "0", "0", "0", "0", "0")
    }
    // Resultado e mensagem também sobrevivem à rotação; o resultado é uma
    // matriz quadrada, então é salvo achatado e reconstruído pelo tamanho.
    val resultSaver = listSaver<List<List<String>>?, String>(
        save = { it?.flatten() ?: emptyList() },
        restore = { flat ->
            when (flat.size) {
                4 -> flat.chunked(2)
                9 -> flat.chunked(3)
                else -> null
            }
        }
    )
    var matrixResult by rememberSaveable(stateSaver = resultSaver) {
        mutableStateOf<List<List<String>>?>(null)
    }
    var matrixMessage by rememberSaveable { mutableStateOf<String?>(null) }

    fun runMatrixOp(op: String) {
        val n = matrixSize
        fun parse(cells: List<String>): Array<DoubleArray>? {
            val m = Array(n) { DoubleArray(n) }
            for (i in 0 until n) {
                for (j in 0 until n) {
                    m[i][j] = cells[i * 3 + j].toDoubleOrNull() ?: return null
                }
            }
            return m
        }

        fun toDisplay(m: Array<DoubleArray>): List<List<String>> =
            m.map { row -> row.map { prettyNumber(formatNumber(it)) } }

        matrixResult = null
        matrixMessage = null
        val a = parse(cellsA)
        if (a == null) {
            matrixMessage = "Valores inválidos na matriz A"
            return
        }
        when (op) {
            "A+B", "A-B", "A×B" -> {
                val b = parse(cellsB)
                if (b == null) {
                    matrixMessage = "Valores inválidos na matriz B"
                    return
                }
                val result = when (op) {
                    "A+B" -> MatrixOps.add(a, b)
                    "A-B" -> MatrixOps.subtract(a, b)
                    else -> MatrixOps.multiply(a, b)
                }
                matrixResult = toDisplay(result)
            }

            "det A" -> matrixMessage = "det A = ${prettyNumber(formatNumber(MatrixOps.determinant(a)))}"

            "A⁻¹" -> {
                val inv = MatrixOps.inverse(a)
                if (inv == null) {
                    matrixMessage = "A não tem inversa (det = 0)"
                } else {
                    matrixResult = toDisplay(inv)
                }
            }

            "Aᵀ" -> matrixResult = toDisplay(MatrixOps.transpose(a))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            // Mantém o conteúdo fora da barra de status e da barra de
            // navegação nos aparelhos edge-to-edge (Android 15+).
            .safeDrawingPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Seletor de modo e opções (com rolagem horizontal para telas estreitas)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(
                MODE_BASIC to "Básica",
                MODE_SCIENTIFIC to "Científica",
                MODE_GRAPH to "Gráfico",
                MODE_MATRIX to "Matriz"
            ).forEach { (id, label) ->
                Chip(
                    label = label,
                    selected = mode == id,
                    colors = colors,
                    modifier = Modifier.selectable(
                        selected = mode == id,
                        role = Role.Tab,
                        onClick = {
                            onModeChange(id)
                            showHistory = false
                        }
                    )
                )
            }
        }

        // Opções do modo, numa linha própria: na linha de modos, que rola de
        // lado, elas ficavam escondidas fora da tela nos celulares estreitos.
        if (mode != MODE_MATRIX) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (mode == MODE_SCIENTIFIC || mode == MODE_GRAPH) {
                    // Mostra a unidade em uso; tocar alterna.
                    Chip(
                        label = if (degrees) "GRAUS" else "RAD",
                        selected = false,
                        colors = colors,
                        small = true,
                        modifier = Modifier
                            .semantics {
                                contentDescription = if (degrees) "Ângulos em graus. Tocar muda para radianos"
                                else "Ângulos em radianos. Tocar muda para graus"
                            }
                            .clickable(role = Role.Switch) { degrees = !degrees }
                    )
                }
                if (mode == MODE_BASIC || mode == MODE_SCIENTIFIC) {
                    Chip(
                        label = "Histórico",
                        selected = showHistory,
                        colors = colors,
                        small = true,
                        modifier = Modifier.toggleable(
                            value = showHistory,
                            role = Role.Switch,
                            onValueChange = { showHistory = it }
                        )
                    )
                }
            }
        }

        if (mode == MODE_MATRIX) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tamanho:", fontSize = 14.sp, color = colors.secondaryText)
                    listOf(2, 3).forEach { n ->
                        val selected = matrixSize == n
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (selected) colors.functionKey else colors.chip)
                                .selectable(
                                    selected = selected,
                                    role = Role.RadioButton,
                                    onClick = {
                                        matrixSize = n
                                        matrixResult = null
                                        matrixMessage = null
                                    }
                                )
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${n}×${n}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (selected) colors.keyText else colors.chipText
                            )
                        }
                    }
                }

                MatrixGrid(label = "Matriz A", size = matrixSize, cells = cellsA, colors = colors)
                MatrixGrid(label = "Matriz B", size = matrixSize, cells = cellsB, colors = colors)

                listOf(
                    listOf("A+B", "A-B", "A×B"),
                    listOf("det A", "A⁻¹", "Aᵀ")
                ).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                                        runMatrixOp(op)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = op,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.keyText
                                )
                            }
                        }
                    }
                }

                matrixMessage?.let { message ->
                    Text(
                        text = message,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.text
                    )
                }

                matrixResult?.let { result ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Resultado",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.secondaryText
                        )
                        result.forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
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
                                            text = cell,
                                            // Valores longos (dízimas da inversa)
                                            // usam fonte menor para caber
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
        } else {
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val extraRows = when (mode) {
                    MODE_SCIENTIFIC -> SCIENTIFIC_ROWS
                    MODE_GRAPH -> GRAPH_ROWS
                    else -> emptyList()
                }.map { row ->
                    row.map { key -> if (secondFunctions) SECOND_FUNCTIONS[key] ?: key else key }
                }
                val memoryRow = if (mode == MODE_GRAPH) null else MEMORY_ROW
                if (showHistory && mode != MODE_GRAPH) {
                    // O histórico ocupa a área toda (visor e teclado): no
                    // lugar só do visor cabia uma conta. Tocar numa conta a
                    // traz de volta e fecha o painel.
                    HistoryPanel(
                        entries = history,
                        colors = colors,
                        onPick = { entry ->
                            calc = calc.copy(
                                expression = entry.expression,
                                lastExpression = "",
                                justEvaluated = false
                            )
                            showHistory = false
                        },
                        onClear = {
                            history = emptyList()
                            prefs?.history = emptyList()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (maxWidth > maxHeight) {
                    // Paisagem (ou janela larga e baixa): visor/gráfico à
                    // esquerda e teclado à direita. Empilhados, as linhas do
                    // modo científico não caberiam na altura.
                    val rows = if (extraRows.isEmpty()) {
                        BASIC_ROWS
                    } else {
                        sideBySideRows(extraRows, BASIC_ROWS)
                    }
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CalcScreenArea(
                            mode = mode,
                            calc = calc,
                            degrees = degrees,
                            colors = colors,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        Column(
                            modifier = Modifier
                                .weight(if (extraRows.isEmpty()) 1f else 1.6f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (memoryRow != null) {
                                Keypad(
                                    rows = listOf(memoryRow),
                                    onKey = onKey,
                                    colors = colors,
                                    rowHeight = 30.dp,
                                    spacing = 8.dp,
                                    compact = true,
                                    secondActive = secondFunctions
                                )
                            }
                            Keypad(
                                rows = rows,
                                onKey = onKey,
                                colors = colors,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                spacing = 8.dp,
                                compact = true,
                                secondActive = secondFunctions
                            )
                        }
                    }
                } else {
                    val spacing = 12.dp
                    val rows = extraRows + BASIC_ROWS
                    // Teclas quadradas quando a tela permite, mas nunca mais
                    // altas do que o espaço disponível (reservando área mínima
                    // para o display — ou para o gráfico — e para a linha de
                    // memória), para nenhuma linha ficar cortada embaixo.
                    val memoryHeight = if (memoryRow != null) 34.dp + spacing else 0.dp
                    val rowCount = rows.size
                    val reserved = (if (mode == MODE_GRAPH) 200.dp else 100.dp) + memoryHeight
                    val cellWidth = (maxWidth - spacing * 3) / 4
                    val cellMaxHeight = (maxHeight - reserved - spacing * rowCount) / rowCount
                    val buttonHeight = minOf(cellWidth, cellMaxHeight).coerceAtLeast(40.dp)

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        CalcScreenArea(
                            mode = mode,
                            calc = calc,
                            degrees = degrees,
                            colors = colors,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                        if (memoryRow != null) {
                            Keypad(
                                rows = listOf(memoryRow),
                                onKey = onKey,
                                colors = colors,
                                rowHeight = 34.dp,
                                spacing = spacing,
                                compact = true,
                                secondActive = secondFunctions
                            )
                        }
                        Keypad(
                            rows = rows,
                            onKey = onKey,
                            colors = colors,
                            rowHeight = buttonHeight,
                            spacing = spacing,
                            secondActive = secondFunctions
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Chip(
    label: String,
    selected: Boolean,
    colors: CalcColors,
    modifier: Modifier = Modifier,
    small: Boolean = false
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) colors.accent else colors.chip)
            .then(modifier)
            .padding(horizontal = 12.dp, vertical = if (small) 5.dp else 8.dp)
    ) {
        Text(
            text = label,
            fontSize = if (small) 12.sp else 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (selected) colors.keyText else colors.chipText
        )
    }
}

/** Visor da conta ou, no modo gráfico, o gráfico com a f(x) embaixo. */
@Composable
fun CalcScreenArea(
    mode: String,
    calc: CalcState,
    degrees: Boolean,
    colors: CalcColors,
    modifier: Modifier = Modifier
) {
    if (mode == MODE_GRAPH) {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GraphArea(
                plotExpression = calc.plotExpression,
                degrees = degrees,
                colors = colors,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                val graphText = prettyExpression(calc.expression).ifEmpty { "..." }
                Text(
                    text = "f(x) = $graphText",
                    maxLines = 1,
                    fontSize = when {
                        graphText.length <= 18 -> 24.sp
                        graphText.length <= 34 -> 16.sp
                        else -> 12.sp
                    },
                    fontWeight = FontWeight.Light,
                    color = colors.text
                )
            }
        }
    } else {
        ExpressionDisplay(calc = calc, colors = colors, modifier = modifier)
    }
}

@Composable
fun ExpressionDisplay(calc: CalcState, colors: CalcColors, modifier: Modifier = Modifier) {
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Copia o texto cru (com * e /, ponto decimal), que outros apps entendem
    // melhor do que os símbolos do visor.
    val rawText = calc.expression.ifEmpty { "0" }
    val displayText = prettyExpression(rawText)

    fun copyToClipboard() {
        scope.launch {
            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("CalcDroid", rawText)))
        }
        // Do Android 13 em diante o próprio sistema confirma a cópia; um
        // Toast ali só duplicaria o aviso.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(context, "Copiado!", Toast.LENGTH_SHORT).show()
        }
    }

    // Display ocupa o espaço restante, ancorando o teclado embaixo
    Box(
        modifier = modifier
            .pointerInput(rawText) {
                detectTapGestures(onLongPress = { copyToClipboard() })
            }
            // Mesma ação para quem usa TalkBack (toque duplo e segurar).
            .semantics {
                onLongClick(label = "Copiar") {
                    copyToClipboard()
                    true
                }
            },
        contentAlignment = Alignment.BottomEnd
    ) {
        if (calc.memory != 0.0) {
            // Indica que há um valor guardado na memória.
            Text(
                text = "M",
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .semantics { contentDescription = "Memória: ${prettyNumber(formatNumber(calc.memory))}" },
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = colors.accent
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            if (calc.lastExpression.isNotEmpty()) {
                Text(
                    text = prettyExpression(calc.lastExpression),
                    fontSize = 18.sp,
                    color = colors.secondaryText,
                    maxLines = 1
                )
            }
            Text(
                text = displayText,
                textAlign = TextAlign.End,
                maxLines = 3,
                // Fonte diminui conforme a expressão cresce,
                // para não estourar a largura do display.
                fontSize = when {
                    displayText.length <= 9 -> 60.sp
                    displayText.length <= 14 -> 44.sp
                    displayText.length <= 24 -> 32.sp
                    displayText.length <= 40 -> 24.sp
                    else -> 20.sp
                },
                fontWeight = FontWeight.Light,
                color = colors.text
            )
        }
    }
}

/** Lista das últimas contas; tocar numa a traz de volta para o visor. */
@Composable
fun HistoryPanel(
    entries: List<HistoryEntry>,
    colors: CalcColors,
    onPick: (HistoryEntry) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colors.card)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Histórico",
                modifier = Modifier.weight(1f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = colors.text
            )
            if (entries.isNotEmpty()) {
                Text(
                    text = "Limpar",
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(role = Role.Button, onClick = onClear)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    fontSize = 14.sp,
                    color = colors.accent
                )
            }
        }
        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("As contas feitas aparecem aqui.", color = colors.secondaryText, fontSize = 15.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(entries) { entry ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(role = Role.Button) { onPick(entry) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = prettyExpression(entry.expression) + " =",
                            fontSize = 14.sp,
                            color = colors.secondaryText,
                            maxLines = 1
                        )
                        Text(
                            text = prettyExpression(entry.result),
                            fontSize = 22.sp,
                            color = colors.text,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * Grade de teclas. Com [rowHeight] as linhas têm altura fixa (retrato); sem
 * ele, dividem igualmente a altura disponível (paisagem). Posições "" ficam
 * vazias, e linhas mais curtas são completadas para manter a largura de
 * tecla igual em todas.
 */
@Composable
fun Keypad(
    rows: List<List<String>>,
    onKey: (String) -> Unit,
    colors: CalcColors,
    modifier: Modifier = Modifier,
    rowHeight: Dp? = null,
    spacing: Dp = 12.dp,
    compact: Boolean = false,
    secondActive: Boolean = false
) {
    val columns = rows.maxOf { it.size }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing)) {
        rows.forEach { row ->
            val rowModifier = if (rowHeight != null) {
                Modifier.height(rowHeight)
            } else {
                Modifier.weight(1f)
            }
            Row(
                modifier = rowModifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                row.forEach { key ->
                    if (key.isEmpty()) {
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        CalculatorButton(
                            key = key,
                            colors = colors,
                            compact = compact || key in MEMORY_ROW,
                            highlighted = key == "2nd" && secondActive,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            onClick = { onKey(key) }
                        )
                    }
                }
                repeat(columns - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun GraphArea(
    plotExpression: String,
    degrees: Boolean,
    colors: CalcColors,
    modifier: Modifier = Modifier
) {
    // Amostra f(x) em 301 pontos no intervalo [-10, 10]
    val samples = remember(plotExpression, degrees) {
        if (plotExpression.isEmpty()) {
            emptyList()
        } else {
            (0..300).map { i ->
                val x = -10.0 + i * 20.0 / 300
                val y = try {
                    ExpressionEvaluator.evaluate(plotExpression, x, degrees)
                } catch (e: Exception) {
                    Double.NaN
                }
                x to y
            }
        }
    }
    val (yLo, yHi) = remember(samples) { graphYRange(samples.map { it.second }) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(colors.card)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            fun px(x: Double) = ((x + 10.0) / 20.0 * w).toFloat()
            fun py(y: Double) = (h - (y - yLo) / (yHi - yLo) * h).toFloat()

            var gx = -10
            while (gx <= 10) {
                val fx = px(gx.toDouble())
                drawLine(colors.grid, Offset(fx, 0f), Offset(fx, h))
                gx += 2
            }
            for (i in 1..5) {
                val fy = h * i / 6f
                drawLine(colors.grid, Offset(0f, fy), Offset(w, fy))
            }
            drawLine(colors.axis, Offset(px(0.0), 0f), Offset(px(0.0), h), strokeWidth = 2f)
            if (yLo < 0 && yHi > 0) {
                drawLine(colors.axis, Offset(0f, py(0.0)), Offset(w, py(0.0)), strokeWidth = 2f)
            }

            if (samples.isNotEmpty()) {
                val span = yHi - yLo
                val path = Path()
                var penDown = false
                var prevY = Double.NaN
                for ((x, y) in samples) {
                    // Levanta a "caneta" em descontinuidades: NaN, pontos muito
                    // fora da tela e saltos de assíntota (como em tan), em que o
                    // sinal troca de um ponto para o outro com uma diferença
                    // maior do que a altura inteira do gráfico.
                    val inRange = y.isFinite() && y > yLo - span && y < yHi + span
                    val jump = penDown && abs(y - prevY) > span && sign(y) != sign(prevY)
                    if (inRange && !jump) {
                        val fx = px(x)
                        val fy = py(y)
                        if (penDown) path.lineTo(fx, fy) else path.moveTo(fx, fy)
                        penDown = true
                    } else if (inRange) {
                        path.moveTo(px(x), py(y))
                        penDown = true
                    } else {
                        penDown = false
                    }
                    prevY = y
                }
                drawPath(
                    path = path,
                    color = colors.accent,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
        if (samples.isEmpty()) {
            Text(
                text = "Digite f(x) no teclado e aperte =",
                modifier = Modifier.align(Alignment.Center),
                color = colors.secondaryText,
                fontSize = 15.sp
            )
        } else {
            Text(
                text = prettyNumber(axisLabel(yHi)),
                modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
                color = colors.secondaryText,
                fontSize = 11.sp
            )
            Text(
                text = prettyNumber(axisLabel(yLo)),
                modifier = Modifier.align(Alignment.BottomStart).padding(6.dp),
                color = colors.secondaryText,
                fontSize = 11.sp
            )
            Text(
                text = "x: -10 a 10" + if (degrees) " (graus)" else " (rad)",
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                color = colors.secondaryText,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun MatrixGrid(label: String, size: Int, cells: MutableList<String>, colors: CalcColors) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = colors.secondaryText
        )
        for (i in 0 until size) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (j in 0 until size) {
                    val index = i * 3 + j
                    MatrixCell(
                        value = cells[index],
                        onChange = { cells[index] = it },
                        colors = colors,
                        modifier = Modifier
                            .weight(1f)
                            .semantics {
                                contentDescription = "$label, linha ${i + 1}, coluna ${j + 1}"
                            }
                    )
                }
            }
        }
    }
}

// Mostra o ponto decimal guardado na célula como vírgula, sem mudar o valor.
private val DecimalCommaTransformation = VisualTransformation { text ->
    TransformedText(AnnotatedString(text.text.replace('.', ',')), OffsetMapping.Identity)
}

@Composable
fun MatrixCell(value: String, onChange: (String) -> Unit, colors: CalcColors, modifier: Modifier = Modifier) {
    BasicTextField(
        value = value,
        onValueChange = { raw ->
            // O teclado numérico em pt-BR insere vírgula como separador
            // decimal; normaliza para ponto antes de validar.
            val new = raw.replace(',', '.')
            val validChars = new.all { it.isDigit() || it == '.' || it == '-' }
            val singleDot = new.count { it == '.' } <= 1
            // Sinal negativo só no início, e no máximo um
            val minusOk = !new.contains('-') || (new.startsWith("-") && new.count { it == '-' } == 1)
            if (new.length <= 8 && validChars && singleDot && minusOk) {
                onChange(new)
            }
        },
        textStyle = TextStyle(
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            color = colors.text
        ),
        cursorBrush = SolidColor(colors.accent),
        visualTransformation = DecimalCommaTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = modifier
            .shadow(2.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(colors.card)
            .padding(vertical = 12.dp, horizontal = 4.dp)
    )
}

private fun keyColor(key: String, colors: CalcColors, highlighted: Boolean): androidx.compose.ui.graphics.Color =
    when {
        highlighted -> colors.operatorKey
        key in setOf("+", "-", "*", "/", "=", "%", "^") -> colors.operatorKey
        key in setOf("C", "⌫") || key in MEMORY_ROW -> colors.grayKey
        key.length == 1 && (key[0].isDigit() || key == ".") -> colors.numberKey
        else -> colors.functionKey
    }

@Composable
fun CalculatorButton(
    key: String,
    colors: CalcColors,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    highlighted: Boolean = false,
    onClick: () -> Unit
) {
    val buttonColor = keyColor(key, colors, highlighted)
    val label = KEY_LABELS[key] ?: key
    val textColor = when (buttonColor) {
        colors.numberKey -> colors.numberKeyText
        colors.grayKey -> colors.grayKeyText
        else -> colors.keyText
    }
    Box(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(16.dp)) // Sombra
            .clip(RoundedCornerShape(16.dp)) // Cantos arredondados
            .background(buttonColor) // Cor do botão
            .clickable(role = Role.Button, onClick = onClick)
            // O TalkBack lê "Multiplicar, botão" em vez do símbolo "×".
            .semantics {
                contentDescription = (KEY_DESCRIPTIONS[key] ?: key) +
                    if (key == "2nd") (if (highlighted) ", ligada" else ", desligada") else ""
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            // A descrição acima já fala pela tecla; o texto não é lido de novo.
            modifier = Modifier.clearAndSetSemantics { },
            // Rótulos longos (sin, cos, log...) usam fonte menor para caber;
            // em paisagem as teclas são mais baixas, então tudo encolhe.
            fontSize = when {
                compact && label.length > 2 -> 15.sp
                compact -> 20.sp
                label.length > 3 -> 20.sp
                label.length > 2 -> 22.sp
                else -> 30.sp
            },
            fontWeight = FontWeight.Medium, // Fonte mais grossa
            color = textColor
        )
    }
}
