package io.github.engsoft7.calcdroid

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.engsoft7.calcdroid.ui.theme.CalcColors
import io.github.engsoft7.calcdroid.ui.theme.CalcDroidTheme
import io.github.engsoft7.calcdroid.ui.theme.calcColors
import kotlinx.coroutines.launch
import java.text.DecimalFormatSymbols
import kotlin.math.ceil

const val MODE_BASIC = "basic"
const val MODE_SCIENTIFIC = "sci"
const val MODE_GRAPH = "graph"
const val MODE_MATRIX = "matrix"
const val MODE_CONVERTER = "converter"

private val MODES = listOf(
    MODE_BASIC to R.string.mode_basic,
    MODE_SCIENTIFIC to R.string.mode_scientific,
    MODE_GRAPH to R.string.mode_graph,
    MODE_MATRIX to R.string.mode_matrix,
    MODE_CONVERTER to R.string.mode_converter,
)

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

// Rótulo exibido na tecla quando difere do código usado na expressão
// (o "." vira o separador decimal do idioma, em Keypad).
private val KEY_LABELS = mapOf(
    "*" to "×", "/" to "÷", "M-" to "M−",
    "asin" to "sin⁻¹", "acos" to "cos⁻¹", "atan" to "tan⁻¹", "abs" to "|x|"
)

// O que o TalkBack lê em cada tecla; dígitos e letras são lidos como estão.
private val KEY_DESCRIPTIONS: Map<String, Int> = mapOf(
    "C" to R.string.key_clear,
    "AC" to R.string.key_clear,
    "⌫" to R.string.key_backspace,
    "%" to R.string.key_percent,
    "/" to R.string.key_divide,
    "*" to R.string.key_multiply,
    "-" to R.string.key_minus,
    "+" to R.string.key_plus,
    "=" to R.string.key_equals,
    "." to R.string.key_decimal,
    "^" to R.string.key_power,
    "√" to R.string.key_sqrt,
    "!" to R.string.key_factorial,
    "(" to R.string.key_open_paren,
    ")" to R.string.key_close_paren,
    "π" to R.string.key_pi,
    "e" to R.string.key_e,
    "x" to R.string.key_x,
    "sin" to R.string.key_sin,
    "cos" to R.string.key_cos,
    "tan" to R.string.key_tan,
    "asin" to R.string.key_asin,
    "acos" to R.string.key_acos,
    "atan" to R.string.key_atan,
    "abs" to R.string.key_abs,
    "ln" to R.string.key_ln,
    "log" to R.string.key_log,
    "x²" to R.string.key_square,
    "x⁻¹" to R.string.key_inverse,
    "mod" to R.string.key_mod,
    "2nd" to R.string.key_second,
    "MC" to R.string.key_mc,
    "MR" to R.string.key_mr,
    "M+" to R.string.key_mplus,
    "M-" to R.string.key_mminus,
    "±" to R.string.key_plusminus,
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

/** Números no visor com o separador decimal do idioma. */
fun prettyNumber(text: String, decimalSep: Char = ','): String = text.replace('.', decimalSep)

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

/**
 * Abre a página do app na Play Store (ou no navegador, se não houver Play).
 * Usado no lugar da biblioteca de In-App Review, que traz uma Activity com
 * setSystemUiVisibility, API descontinuada que a Play Console sinaliza.
 */
private fun openPlayStorePage(context: Context) {
    val id = context.packageName
    val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$id"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(market)
    } catch (e: ActivityNotFoundException) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$id"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
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
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val decimalSep = remember(locale) { DecimalFormatSymbols.getInstance(locale).decimalSeparator }
    // Cada modo guarda o próprio estado (matrizes, conversor) ao trocar de aba.
    val stateHolder = rememberSaveableStateHolder()

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
    var showReviewDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(calc.memory) { prefs?.memory = calc.memory }
    LaunchedEffect(degrees) { prefs?.degrees = degrees }

    val onKey: (String) -> Unit = { key ->
        haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
        if (key == "2nd") {
            secondFunctions = !secondFunctions
        } else {
            val after = calc.press(key, graphMode = mode == MODE_GRAPH, degrees = degrees)
            if (key == "=" && after.justEvaluated && after.expression != ERROR_TEXT) {
                history = addToHistory(history, HistoryEntry(after.lastExpression.removeSuffix(" ="), after.expression))
                prefs?.history = history
                if (prefs?.countCalculationAndCheckReview() == true) showReviewDialog = true
            }
            calc = after
            // A segunda função vale para uma tecla, como nas calculadoras.
            if (key in SECOND_FUNCTIONS.values) secondFunctions = false
        }
    }

    Box(Modifier.fillMaxSize()) {
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
            // Seletor de modo (com rolagem horizontal para telas estreitas). Os
            // últimos modos ficavam cortados na borda; a linha rola até o
            // escolhido.
            val modeScroll = rememberScrollState()
            LaunchedEffect(mode) {
                val index = MODES.indexOfFirst { it.first == mode }
                modeScroll.animateScrollTo(if (index >= MODES.size - 2) modeScroll.maxValue else 0)
            }
            Row(
                modifier = Modifier.horizontalScroll(modeScroll),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MODES.forEach { (id, label) ->
                    Chip(
                        label = stringResource(label),
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
            if (mode == MODE_BASIC || mode == MODE_SCIENTIFIC || mode == MODE_GRAPH) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (mode == MODE_SCIENTIFIC || mode == MODE_GRAPH) {
                        // Mostra a unidade em uso; tocar alterna.
                        val desc = stringResource(if (degrees) R.string.degrees_desc else R.string.radians_desc)
                        Chip(
                            label = stringResource(if (degrees) R.string.degrees_short else R.string.radians_short),
                            selected = false,
                            colors = colors,
                            small = true,
                            modifier = Modifier
                                .semantics { contentDescription = desc }
                                .clickable(role = Role.Switch) { degrees = !degrees }
                        )
                    }
                    if (mode == MODE_BASIC || mode == MODE_SCIENTIFIC) {
                        Chip(
                            label = stringResource(R.string.history),
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

            val contentModifier = Modifier
                .weight(1f)
                .fillMaxWidth()
            stateHolder.SaveableStateProvider(mode) {
                when (mode) {
                    MODE_MATRIX -> MatrixScreen(colors, decimalSep, contentModifier)
                    MODE_CONVERTER -> ConverterScreen(colors, decimalSep, contentModifier)
                    else -> CalculatorArea(
                        mode = mode,
                        calc = calc,
                        degrees = degrees,
                        secondFunctions = secondFunctions,
                        showHistory = showHistory,
                        history = history,
                        colors = colors,
                        decimalSep = decimalSep,
                        onKey = onKey,
                        onPickHistory = { entry ->
                            calc = calc.copy(expression = entry.expression, lastExpression = "", justEvaluated = false)
                            showHistory = false
                        },
                        onClearHistory = {
                            history = emptyList()
                            prefs?.history = emptyList()
                        },
                        modifier = contentModifier
                    )
                }
            }
        }
        if (showReviewDialog) {
            // Pedido de avaliação, uma única vez depois de uso de verdade.
            // Desenhado na própria tela: o Dialog/AlertDialog do Compose abre
            // outra janela e traz setDecorFitsSystemWindows/setSystemUiVisibility,
            // APIs descontinuadas que a Play Console sinaliza (ver build.yml).
            ReviewPrompt(
                colors = colors,
                onRate = {
                    showReviewDialog = false
                    openPlayStorePage(context)
                },
                onDismiss = { showReviewDialog = false }
            )
        }
    }
}

@Composable
private fun ReviewPrompt(colors: CalcColors, onRate: () -> Unit, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            // Tocar fora fecha, como num diálogo.
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(colors.card)
                .clickable(enabled = false) { }
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.review_title), fontSize = 20.sp, fontWeight = FontWeight.Medium, color = colors.text)
            Text(stringResource(R.string.review_text), fontSize = 15.sp, color = colors.secondaryText)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                Text(
                    stringResource(R.string.review_later),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(role = Role.Button, onClick = onDismiss)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    color = colors.secondaryText,
                    fontSize = 15.sp
                )
                Text(
                    stringResource(R.string.review_rate),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.accent)
                        .clickable(role = Role.Button, onClick = onRate)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    color = colors.keyText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/** Visor (ou gráfico, ou histórico) e teclado dos modos de expressão. */
@Composable
private fun CalculatorArea(
    mode: String,
    calc: CalcState,
    degrees: Boolean,
    secondFunctions: Boolean,
    showHistory: Boolean,
    history: List<HistoryEntry>,
    colors: CalcColors,
    decimalSep: Char,
    onKey: (String) -> Unit,
    onPickHistory: (HistoryEntry) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier) {
        val extraRows = when (mode) {
            MODE_SCIENTIFIC -> SCIENTIFIC_ROWS
            MODE_GRAPH -> GRAPH_ROWS
            else -> emptyList()
        }.map { row ->
            row.map { key -> if (secondFunctions) SECOND_FUNCTIONS[key] ?: key else key }
        }
        val memoryRow = if (mode == MODE_GRAPH) null else MEMORY_ROW
        if (showHistory && mode != MODE_GRAPH) {
            // O histórico ocupa a área toda (visor e teclado): no lugar só do
            // visor cabia uma conta. Tocar numa conta a traz de volta.
            HistoryPanel(history, colors, decimalSep, onPickHistory, onClearHistory, Modifier.fillMaxSize())
        } else if (maxWidth > maxHeight) {
            // Paisagem (ou janela larga e baixa): visor/gráfico à esquerda e
            // teclado à direita. Empilhados, as linhas do modo científico não
            // caberiam na altura.
            val rows = if (extraRows.isEmpty()) BASIC_ROWS else sideBySideRows(extraRows, BASIC_ROWS)
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CalcScreenArea(
                    mode, calc, degrees, colors, decimalSep,
                    Modifier
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
                            listOf(memoryRow), onKey, colors, decimalSep,
                            rowHeight = 30.dp, spacing = 8.dp, compact = true, secondActive = secondFunctions
                        )
                    }
                    Keypad(
                        rows, onKey, colors, decimalSep,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        spacing = 8.dp, compact = true, secondActive = secondFunctions
                    )
                }
            }
        } else {
            val rows = extraRows + BASIC_ROWS
            val rowCount = rows.size
            // Teclas quadradas quando a tela permite, mas nunca mais altas do
            // que o espaço disponível (reservando área mínima para o visor —
            // ou para o gráfico — e para a linha de memória), para nenhuma
            // linha ficar cortada embaixo.
            fun cellMaxHeight(spacing: Dp, memoryKey: Dp, display: Dp): Dp {
                val memoryHeight = if (memoryRow != null) memoryKey + spacing else 0.dp
                val reserved = display + memoryHeight
                return (maxHeight - reserved - spacing * rowCount) / rowCount
            }
            // Em telas baixas (celulares pequenos, tela dividida) as teclas e
            // os espaços encolhem antes do visor: com altura mínima fixa de
            // 40dp o visor chegava a sumir e o resultado ficava fora dele.
            val normalDisplay = if (mode == MODE_GRAPH) 200.dp else 100.dp
            val tight = cellMaxHeight(12.dp, 34.dp, normalDisplay) < 40.dp
            val spacing = if (tight) 6.dp else 12.dp
            val memoryKeyHeight = if (tight) 28.dp else 34.dp
            // Muito apertado (modo científico em tela dividida): o visor
            // aceita ficar menor, mas nunca some.
            val display = if (tight) (if (mode == MODE_GRAPH) 140.dp else 72.dp) else normalDisplay
            val cellWidth = (maxWidth - spacing * 3) / 4
            val buttonHeight = minOf(cellWidth, cellMaxHeight(spacing, memoryKeyHeight, display)).coerceAtLeast(20.dp)

            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(spacing)) {
                CalcScreenArea(
                    mode, calc, degrees, colors, decimalSep,
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
                if (memoryRow != null) {
                    Keypad(
                        listOf(memoryRow), onKey, colors, decimalSep,
                        rowHeight = memoryKeyHeight, spacing = spacing, compact = true, secondActive = secondFunctions
                    )
                }
                Keypad(
                    rows, onKey, colors, decimalSep,
                    rowHeight = buttonHeight, spacing = spacing, compact = tight, secondActive = secondFunctions
                )
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
private fun CalcScreenArea(
    mode: String,
    calc: CalcState,
    degrees: Boolean,
    colors: CalcColors,
    decimalSep: Char,
    modifier: Modifier = Modifier
) {
    if (mode == MODE_GRAPH) {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GraphView(
                plotExpression = calc.plotExpression,
                degrees = degrees,
                colors = colors,
                decimalSep = decimalSep,
                modifier = Modifier.weight(1f)
            )
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                val graphText = prettyExpression(calc.expression, decimalSep).ifEmpty { "..." }
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
        ExpressionDisplay(calc, colors, decimalSep, modifier)
    }
}

@Composable
private fun ExpressionDisplay(calc: CalcState, colors: CalcColors, decimalSep: Char, modifier: Modifier = Modifier) {
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val errorText = stringResource(R.string.error)
    val copiedText = stringResource(R.string.copied)
    // Copia o texto cru (com * e /, ponto decimal), que outros apps entendem
    // melhor do que os símbolos do visor.
    val rawText = calc.expression.ifEmpty { "0" }
    val displayText = if (rawText == ERROR_TEXT) errorText else prettyExpression(rawText, decimalSep)

    fun copyToClipboard() {
        scope.launch {
            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("CalcDroid", rawText)))
        }
        // Do Android 13 em diante o próprio sistema confirma a cópia; um
        // Toast ali só duplicaria o aviso.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(context, copiedText, Toast.LENGTH_SHORT).show()
        }
    }

    val copyLabel = stringResource(R.string.copy)
    val memoryDesc = stringResource(R.string.memory_desc, prettyNumber(formatNumber(calc.memory), decimalSep))
    // Visor ocupa o espaço restante, ancorando o teclado embaixo
    BoxWithConstraints(
        modifier = modifier
            .pointerInput(rawText) {
                detectTapGestures(onLongPress = { copyToClipboard() })
            }
            // Mesma ação para quem usa TalkBack (toque duplo e segurar).
            .semantics {
                onLongClick(label = copyLabel) {
                    copyToClipboard()
                    true
                }
            },
        contentAlignment = Alignment.BottomEnd
    ) {
        val displayHeight = maxHeight
        if (calc.memory != 0.0) {
            // Indica que há um valor guardado na memória.
            Text(
                text = "M",
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .semantics { contentDescription = memoryDesc },
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = colors.accent
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            if (calc.lastExpression.isNotEmpty()) {
                Text(
                    text = prettyExpression(calc.lastExpression, decimalSep),
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
                // para não estourar a largura do visor.
                fontSize = minOf(
                    when {
                        displayText.length <= 9 -> 60f
                        displayText.length <= 14 -> 44f
                        displayText.length <= 24 -> 32f
                        displayText.length <= 40 -> 24f
                        else -> 20f
                    },
                    // Visor baixo (tela pequena ou dividida): a fonte cabe na
                    // altura que sobra depois da linha da conta anterior.
                    ((displayHeight.value - if (calc.lastExpression.isNotEmpty()) 26f else 0f) * 0.75f).coerceAtLeast(16f)
                ).sp,
                fontWeight = FontWeight.Light,
                color = colors.text
            )
        }
    }
}

/** Lista das últimas contas; tocar numa a traz de volta para o visor. */
@Composable
private fun HistoryPanel(
    entries: List<HistoryEntry>,
    colors: CalcColors,
    decimalSep: Char,
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
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.history),
                modifier = Modifier.weight(1f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = colors.text
            )
            if (entries.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.clear),
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
                Text(stringResource(R.string.history_empty), color = colors.secondaryText, fontSize = 15.sp)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                            text = prettyExpression(entry.expression, decimalSep) + " =",
                            fontSize = 14.sp,
                            color = colors.secondaryText,
                            maxLines = 1
                        )
                        Text(
                            text = prettyExpression(entry.result, decimalSep),
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
    decimalSep: Char,
    modifier: Modifier = Modifier,
    rowHeight: Dp? = null,
    spacing: Dp = 12.dp,
    compact: Boolean = false,
    secondActive: Boolean = false
) {
    val columns = rows.maxOf { it.size }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing)) {
        rows.forEach { row ->
            val rowModifier = if (rowHeight != null) Modifier.height(rowHeight) else Modifier.weight(1f)
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
                            label = if (key == ".") decimalSep.toString() else KEY_LABELS[key] ?: key,
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

private fun keyColor(key: String, colors: CalcColors, highlighted: Boolean): Color =
    when {
        highlighted -> colors.operatorKey
        key in setOf("+", "-", "*", "/", "=", "%", "^") -> colors.operatorKey
        key in setOf("C", "AC", "⌫", "±") || key in MEMORY_ROW -> colors.grayKey
        key.length == 1 && (key[0].isDigit() || key == "." || key[0] in 'A'..'F') -> colors.numberKey
        else -> colors.functionKey
    }

@Composable
private fun CalculatorButton(
    key: String,
    label: String,
    colors: CalcColors,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    highlighted: Boolean = false,
    onClick: () -> Unit
) {
    val buttonColor = keyColor(key, colors, highlighted)
    val textColor = when (buttonColor) {
        colors.numberKey -> colors.numberKeyText
        colors.grayKey -> colors.grayKeyText
        else -> colors.keyText
    }
    val description = KEY_DESCRIPTIONS[key]?.let { stringResource(it) } ?: key
    val state = if (key == "2nd") stringResource(if (highlighted) R.string.key_on else R.string.key_off) else ""
    Box(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(buttonColor)
            .clickable(role = Role.Button, onClick = onClick)
            // O TalkBack lê "Multiplicar, botão" em vez do símbolo "×".
            .semantics { contentDescription = description + state },
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
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}
