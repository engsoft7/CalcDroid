package io.github.engsoft7.calcdroid

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.engsoft7.calcdroid.ui.theme.CalcDroidTheme
import java.util.Locale
import kotlin.math.abs

const val MODE_BASIC = "basic"
const val MODE_SCIENTIFIC = "sci"
const val MODE_GRAPH = "graph"
const val MODE_MATRIX = "matrix"

class MainActivity() : ComponentActivity(), Parcelable {
    constructor(parcel: Parcel) : this() {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalcDroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var mode by rememberSaveable { mutableStateOf(MODE_BASIC) }
                    BeautifulCalculatorScreen(
                        mode = mode,
                        onModeChange = { mode = it }
                    )
                }
            }
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {

    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MainActivity> {
        override fun createFromParcel(parcel: Parcel): MainActivity {
            return MainActivity(parcel)
        }

        override fun newArray(size: Int): Array<MainActivity?> {
            return arrayOfNulls(size)
        }
    }
}

@Composable
fun BeautifulCalculatorScreen(
    mode: String = MODE_BASIC,
    onModeChange: (String) -> Unit = {}
) {
    // ---- Estado da calculadora de expressões (básica/científica/gráfico) ----
    var expression by remember { mutableStateOf("") }
    var lastExpression by remember { mutableStateOf("") }
    // Após "=", um dígito ou função começa um cálculo novo; um operador
    // continua a conta em cima do resultado exibido.
    var justEvaluated by remember { mutableStateOf(false) }
    var plotExpression by remember { mutableStateOf("") }

    // ---- Estado do modo matriz (armazenado achatado 3×3; o 2×2 usa o
    // canto superior esquerdo, preservando os valores ao trocar o tamanho) ----
    var matrixSize by rememberSaveable { mutableStateOf(2) }
    val cellsA = remember { mutableStateListOf("0", "0", "0", "0", "0", "0", "0", "0", "0") }
    val cellsB = remember { mutableStateListOf("0", "0", "0", "0", "0", "0", "0", "0", "0") }
    var matrixResult by remember { mutableStateOf<List<List<String>>?>(null) }
    var matrixMessage by remember { mutableStateOf<String?>(null) }

    fun onKey(button: String) {
        when (button) {
            "C" -> {
                expression = ""
                lastExpression = ""
                plotExpression = ""
                justEvaluated = false
            }

            "⌫" -> {
                justEvaluated = false
                if (expression == "Erro") {
                    expression = ""
                    return
                }
                // Funções entram inteiras ("sin("), então saem inteiras
                for (function in listOf("sin(", "cos(", "tan(", "log(", "ln(", "√(")) {
                    if (expression.endsWith(function)) {
                        expression = expression.dropLast(function.length)
                        return
                    }
                }
                expression = expression.dropLast(1)
            }

            "=" -> {
                if (expression.isEmpty() || expression == "Erro") return
                val closed = autoCloseParentheses(expression)
                if (mode == MODE_GRAPH) {
                    expression = closed
                    plotExpression = closed
                } else {
                    val result = try {
                        formatNumber(ExpressionEvaluator.evaluate(closed))
                    } catch (e: Exception) {
                        "Erro"
                    }
                    lastExpression = "$closed ="
                    expression = result
                    justEvaluated = true
                }
            }

            else -> {
                if (justEvaluated) {
                    val continuesResult = button in setOf("+", "-", "*", "/", "^", "%", "!")
                    if (expression == "Erro" || !continuesResult) expression = ""
                    justEvaluated = false
                }
                var toAppend = when (button) {
                    "sin", "cos", "tan", "ln", "log" -> "$button("
                    "√" -> "√("
                    else -> button
                }
                if (button == ".") {
                    // Impede um segundo ponto no mesmo número
                    val currentNumber = expression.takeLastWhile { it.isDigit() || it == '.' }
                    if (currentNumber.contains('.')) return
                    if (currentNumber.isEmpty()) toAppend = "0."
                }
                // Pós-fixos e fechamento não fazem sentido em expressão vazia
                if (expression.isEmpty() && button in setOf("*", "/", "^", "%", "!", ")")) return
                // Dois operadores seguidos: o novo substitui o anterior
                if (button in setOf("+", "-", "*", "/", "^") &&
                    expression.isNotEmpty() && expression.last() in "+-*/^"
                ) {
                    expression = expression.dropLast(1) + button
                    return
                }
                if (expression.length + toAppend.length <= 60) {
                    expression += toAppend
                }
            }
        }
    }

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
            m.map { row -> row.map { formatNumber(it) } }

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

            "det A" -> matrixMessage = "det A = ${formatNumber(MatrixOps.determinant(a))}"

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
            .background(Color(0xFFF0F0F0)) // Cor de fundo mais clara
            // Mantém o conteúdo fora da barra de status e da barra de
            // navegação nos aparelhos edge-to-edge (Android 15+).
            .safeDrawingPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Seletor de modo
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                MODE_BASIC to "Básica",
                MODE_SCIENTIFIC to "Científica",
                MODE_GRAPH to "Gráfico",
                MODE_MATRIX to "Matriz"
            ).forEach { (id, label) ->
                val selected = mode == id
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (selected) Color(0xFFFFA500) else Color(0xFFDDDDDD))
                        .clickable { onModeChange(id) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (selected) Color.White else Color(0xFF555555)
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
                    Text("Tamanho:", fontSize = 14.sp, color = Color(0xFF555555))
                    listOf(2, 3).forEach { n ->
                        val selected = matrixSize == n
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (selected) Color(0xFF4A6572) else Color(0xFFDDDDDD))
                                .clickable {
                                    matrixSize = n
                                    matrixResult = null
                                    matrixMessage = null
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${n}×${n}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (selected) Color.White else Color(0xFF555555)
                            )
                        }
                    }
                }

                MatrixGrid(label = "Matriz A", size = matrixSize, cells = cellsA)
                MatrixGrid(label = "Matriz B", size = matrixSize, cells = cellsB)

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
                                    .background(Color(0xFFFFA500))
                                    .clickable { runMatrixOp(op) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = op,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
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
                        color = Color.Black
                    )
                }

                matrixResult?.let { result ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Resultado",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF555555)
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
                                            .background(Color.White)
                                            .padding(vertical = 10.dp, horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = cell,
                                            fontSize = 16.sp,
                                            color = Color.Black,
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
                val spacing = 12.dp
                val scientificRows = listOf(
                    listOf("sin", "cos", "tan", "√"),
                    listOf("ln", "log", "(", ")"),
                    listOf("π", "e", "^", "!")
                )
                val graphRows = listOf(
                    listOf("sin", "cos", "tan", "√"),
                    listOf("ln", "log", "(", ")"),
                    listOf("π", "e", "^", "x")
                )
                val basicRows = listOf(
                    listOf("C", "⌫", "%", "/"),
                    listOf("7", "8", "9", "*"),
                    listOf("4", "5", "6", "-"),
                    listOf("1", "2", "3", "+"),
                    listOf("0", ".", "=")
                )
                val buttonRows = when (mode) {
                    MODE_SCIENTIFIC -> scientificRows + basicRows
                    MODE_GRAPH -> graphRows + basicRows
                    else -> basicRows
                }
                // Teclas quadradas quando a tela permite, mas nunca mais altas
                // do que o espaço disponível (reservando área mínima para o
                // display — ou para o gráfico), para nenhuma linha do teclado
                // ficar cortada embaixo.
                val rowCount = buttonRows.size
                val reserved = if (mode == MODE_GRAPH) 200.dp else 100.dp
                val cellWidth = (maxWidth - spacing * 3) / 4
                val cellMaxHeight = (maxHeight - reserved - spacing * rowCount) / rowCount
                val buttonHeight = minOf(cellWidth, cellMaxHeight).coerceAtLeast(40.dp)

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    if (mode == MODE_GRAPH) {
                        GraphArea(
                            plotExpression = plotExpression,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            val graphText = expression.ifEmpty { "..." }
                            Text(
                                text = "f(x) = $graphText",
                                maxLines = 1,
                                fontSize = if (graphText.length <= 18) 24.sp else 16.sp,
                                fontWeight = FontWeight.Light,
                                color = Color.Black
                            )
                        }
                    } else {
                        // Display ocupa o espaço restante, ancorando o teclado embaixo
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                if (lastExpression.isNotEmpty()) {
                                    Text(
                                        text = lastExpression,
                                        fontSize = 18.sp,
                                        color = Color.Gray,
                                        maxLines = 1
                                    )
                                }
                                val displayText = expression.ifEmpty { "0" }
                                Text(
                                    text = displayText,
                                    textAlign = TextAlign.End,
                                    maxLines = 2,
                                    // Fonte diminui conforme a expressão cresce,
                                    // para não estourar a largura do display.
                                    fontSize = when {
                                        displayText.length <= 9 -> 60.sp
                                        displayText.length <= 14 -> 44.sp
                                        displayText.length <= 24 -> 32.sp
                                        else -> 24.sp
                                    },
                                    fontWeight = FontWeight.Light,
                                    color = Color.Black
                                )
                            }
                        }
                    }

                    // Buttons
                    buttonRows.forEach { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(buttonHeight),
                            horizontalArrangement = Arrangement.spacedBy(spacing)
                        ) {
                            row.forEach { button ->
                                val buttonColor = when (button) {
                                    "+", "-", "*", "/", "=", "%", "^" -> Color(0xFFFFA500) // Laranja para operadores
                                    "C", "⌫" -> Color(0xFFD3D3D3) // Cinza claro para C e apagar
                                    "sin", "cos", "tan", "√", "ln", "log",
                                    "π", "e", "x", "(", ")", "!" -> Color(0xFF4A6572) // Azul acinzentado para funções
                                    else -> Color.White // Branco para números
                                }
                                CalculatorButton(
                                    text = button,
                                    buttonColor = buttonColor,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    onClick = { onKey(button) }
                                )
                            }
                            // Completa a última linha (3 teclas) mantendo a mesma
                            // largura de tecla das demais linhas.
                            if (row.size < 4) {
                                Spacer(modifier = Modifier.weight((4 - row.size).toFloat()))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GraphArea(plotExpression: String, modifier: Modifier = Modifier) {
    // Amostra f(x) em 301 pontos no intervalo [-10, 10]
    val samples = remember(plotExpression) {
        if (plotExpression.isEmpty()) {
            emptyList()
        } else {
            (0..300).map { i ->
                val x = -10.0 + i * 20.0 / 300
                val y = try {
                    ExpressionEvaluator.evaluate(plotExpression, x)
                } catch (e: Exception) {
                    Double.NaN
                }
                x to y
            }
        }
    }
    // Faixa vertical ajustada aos valores amostrados (sempre incluindo o
    // eixo x e limitada a ±100 para funções que explodem, como tan)
    val finiteYs = samples.map { it.second }.filter { it.isFinite() && abs(it) <= 1e6 }
    var yLo = (finiteYs.minOrNull() ?: -5.0).coerceIn(-100.0, 0.0)
    var yHi = (finiteYs.maxOrNull() ?: 5.0).coerceIn(0.0, 100.0)
    if (yHi - yLo < 2.0) {
        yLo -= 1.0
        yHi += 1.0
    }
    val padY = (yHi - yLo) * 0.08
    yLo -= padY
    yHi += padY

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            fun px(x: Double) = ((x + 10.0) / 20.0 * w).toFloat()
            fun py(y: Double) = (h - (y - yLo) / (yHi - yLo) * h).toFloat()

            val gridColor = Color(0xFFEAEAEA)
            val axisColor = Color(0xFFB5B5B5)
            var gx = -10
            while (gx <= 10) {
                val fx = px(gx.toDouble())
                drawLine(gridColor, Offset(fx, 0f), Offset(fx, h))
                gx += 2
            }
            for (i in 1..5) {
                val fy = h * i / 6f
                drawLine(gridColor, Offset(0f, fy), Offset(w, fy))
            }
            drawLine(axisColor, Offset(px(0.0), 0f), Offset(px(0.0), h), strokeWidth = 2f)
            if (yLo < 0 && yHi > 0) {
                drawLine(axisColor, Offset(0f, py(0.0)), Offset(w, py(0.0)), strokeWidth = 2f)
            }

            if (samples.isNotEmpty()) {
                val span = yHi - yLo
                val path = Path()
                var penDown = false
                for ((x, y) in samples) {
                    // Levanta a "caneta" em descontinuidades (NaN ou saltos de
                    // assíntota, como em tan), para não ligar os dois ramos
                    if (y.isFinite() && y > yLo - span && y < yHi + span) {
                        val fx = px(x)
                        val fy = py(y)
                        if (penDown) path.lineTo(fx, fy) else path.moveTo(fx, fy)
                        penDown = true
                    } else {
                        penDown = false
                    }
                }
                drawPath(
                    path = path,
                    color = Color(0xFFFFA500),
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
                color = Color.Gray,
                fontSize = 15.sp
            )
        } else {
            Text(
                text = String.format(Locale.US, "%.1f", yHi),
                modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
                color = Color.Gray,
                fontSize = 11.sp
            )
            Text(
                text = String.format(Locale.US, "%.1f", yLo),
                modifier = Modifier.align(Alignment.BottomStart).padding(6.dp),
                color = Color.Gray,
                fontSize = 11.sp
            )
            Text(
                text = "x: -10 a 10",
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                color = Color.Gray,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun MatrixGrid(label: String, size: Int, cells: MutableList<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF555555)
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
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun MatrixCell(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    BasicTextField(
        value = value,
        onValueChange = { new ->
            if (new.length <= 8 && new.all { it.isDigit() || it == '.' || it == '-' }) {
                onChange(new)
            }
        },
        textStyle = TextStyle(
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            color = Color.Black
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = modifier
            .shadow(2.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .padding(vertical = 12.dp, horizontal = 4.dp)
    )
}

@Composable
fun CalculatorButton(
    text: String,
    buttonColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(16.dp)) // Sombra
            .clip(RoundedCornerShape(16.dp)) // Cantos arredondados
            .background(buttonColor) // Cor do botão
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            // Rótulos longos (sin, cos, log...) usam fonte menor para caber
            fontSize = if (text.length > 2) 22.sp else 30.sp,
            fontWeight = FontWeight.Medium, // Fonte mais grossa
            color = if (buttonColor == Color.White) Color.Black else Color.White // Cor do texto
        )
    }
}
