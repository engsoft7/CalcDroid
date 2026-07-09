package io.github.engsoft7.calcdroid

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.engsoft7.calcdroid.ui.theme.CalcDroidTheme
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

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
                    var isScientific by rememberSaveable { mutableStateOf(false) }
                    BeautifulCalculatorScreen(
                        isScientific = isScientific,
                        onToggleMode = { isScientific = !isScientific }
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
    isScientific: Boolean = false,
    onToggleMode: () -> Unit = {}
) {
    var displayValue by remember { mutableStateOf("0") }
    var firstOperand by remember { mutableStateOf("") }
    var operator by remember { mutableStateOf("") }
    // Após "=", operador ou função, o próximo dígito inicia um número novo
    // em vez de ser concatenado ao resultado exibido.
    var startNewEntry by remember { mutableStateOf(false) }

    val scientificRows = listOf(
        listOf("sin", "cos", "tan", "√"),
        listOf("ln", "log", "x²", "^"),
        listOf("π", "e", "1/x", "!")
    )
    val basicRows = listOf(
        listOf("C", "+/-", "%", "/"),
        listOf("7", "8", "9", "*"),
        listOf("4", "5", "6", "-"),
        listOf("1", "2", "3", "+"),
        listOf("0", ".", "=")
    )
    val buttonRows = if (isScientific) scientificRows + basicRows else basicRows

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F0F0)) // Cor de fundo mais clara
            // Mantém o conteúdo fora da barra de status e da barra de
            // navegação nos aparelhos edge-to-edge (Android 15+).
            .safeDrawingPadding()
            .padding(16.dp)
    ) {
        val spacing = 12.dp
        // Teclas quadradas quando a tela permite, mas nunca mais altas do
        // que o espaço disponível (reservando área mínima para o display e
        // para o botão de troca de modo), para nenhuma linha do teclado
        // ficar cortada embaixo.
        val rowCount = buttonRows.size
        val cellWidth = (maxWidth - spacing * 3) / 4
        val cellMaxHeight = (maxHeight - 128.dp - spacing * rowCount) / rowCount
        val buttonHeight = minOf(cellWidth, cellMaxHeight).coerceAtLeast(40.dp)

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            // Botão para alternar entre a calculadora básica e a científica
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFFFA500))
                        .clickable { onToggleMode() }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (isScientific) "Básica" else "Científica",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }

            // Display ocupa o espaço restante, ancorando o teclado embaixo
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.BottomEnd
            ) {
                Text(
                    text = displayValue,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    // Fonte diminui conforme o número cresce, para o valor
                    // nunca estourar a largura do display.
                    fontSize = when {
                        displayValue.length <= 9 -> 60.sp
                        displayValue.length <= 13 -> 44.sp
                        else -> 32.sp
                    },
                    fontWeight = FontWeight.Light, // Fonte mais fina
                    color = Color.Black
                )
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
                            "+", "-", "*", "/", "=", "%", "+/-", "^" -> Color(0xFFFFA500) // Laranja para operadores
                            "C" -> Color(0xFFD3D3D3) // Cinza claro para o C
                            "sin", "cos", "tan", "√", "ln", "log", "x²",
                            "π", "e", "1/x", "!" -> Color(0xFF4A6572) // Azul acinzentado para funções científicas
                            else -> Color.White // Branco para números
                        }
                        CalculatorButton(
                            text = button,
                            buttonColor = buttonColor,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            onClick = {
                                when (button) {
                                    "C" -> {
                                        displayValue = "0"
                                        firstOperand = ""
                                        operator = ""
                                        startNewEntry = false
                                    }

                                    "+/-" -> {
                                        // Só inverte o sinal de valores numéricos (nunca "Erro" ou "0")
                                        if (displayValue.toDoubleOrNull() != null && displayValue != "0") {
                                            displayValue = if (displayValue.startsWith("-")) {
                                                displayValue.drop(1)
                                            } else {
                                                "-$displayValue"
                                            }
                                        }
                                    }

                                    "%" -> {
                                        displayValue.toDoubleOrNull()?.let {
                                            displayValue = formatNumber(it / 100)
                                            startNewEntry = true
                                        }
                                    }

                                    "sin", "cos", "tan", "√", "ln", "log",
                                    "x²", "1/x", "!" -> {
                                        if (displayValue.toDoubleOrNull() != null) {
                                            displayValue = applyScientificFunction(button, displayValue)
                                            startNewEntry = true
                                        }
                                    }

                                    "π" -> {
                                        displayValue = formatNumber(Math.PI)
                                        startNewEntry = true
                                    }

                                    "e" -> {
                                        displayValue = formatNumber(Math.E)
                                        startNewEntry = true
                                    }

                                    "+", "-", "*", "/", "^" -> {
                                        if (displayValue.toDoubleOrNull() != null) {
                                            if (firstOperand.isEmpty()) {
                                                firstOperand = displayValue
                                            } else if (!startNewEntry) {
                                                // Encadeamento: 2 + 3 + ... calcula a etapa anterior.
                                                // Com startNewEntry o usuário só está trocando o
                                                // operador, então nada é calculado.
                                                val result = calculate(firstOperand, displayValue, operator)
                                                displayValue = result
                                                if (result == "Erro") {
                                                    firstOperand = ""
                                                    operator = ""
                                                    startNewEntry = true
                                                    return@CalculatorButton
                                                }
                                                firstOperand = result
                                            }
                                            operator = button
                                            startNewEntry = true
                                        }
                                    }

                                    "=" -> {
                                        if (firstOperand.isNotEmpty() && operator.isNotEmpty() &&
                                            displayValue.toDoubleOrNull() != null
                                        ) {
                                            displayValue = calculate(firstOperand, displayValue, operator)
                                            firstOperand = ""
                                            operator = ""
                                            startNewEntry = true
                                        }
                                    }

                                    else -> {
                                        if (startNewEntry || displayValue == "0") {
                                            displayValue = if (button == ".") "0." else button
                                            startNewEntry = false
                                        } else if (button == "." && displayValue.contains(".")) {
                                            // Ignora segundo ponto decimal
                                        } else if (displayValue.length < 15) {
                                            displayValue += button
                                        }
                                    }
                                }
                            }
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

fun calculate(first: String, second: String, operator: String): String {
    val num1 = first.toDoubleOrNull() ?: return "Erro"
    val num2 = second.toDoubleOrNull() ?: return "Erro"
    val result = when (operator) {
        "+" -> num1 + num2
        "-" -> num1 - num2
        "*" -> num1 * num2
        "/" -> num1 / num2
        "^" -> num1.pow(num2)
        else -> return "0"
    }
    return formatNumber(result)
}

fun applyScientificFunction(function: String, value: String): String {
    val num = value.toDoubleOrNull() ?: return value
    val result = when (function) {
        // Funções trigonométricas recebem o ângulo em graus
        "sin" -> sin(Math.toRadians(num))
        "cos" -> cos(Math.toRadians(num))
        "tan" -> tan(Math.toRadians(num))
        "√" -> sqrt(num)
        "ln" -> ln(num)
        "log" -> log10(num)
        "x²" -> num * num
        "1/x" -> 1 / num
        "!" -> factorial(num)
        else -> num
    }
    return formatNumber(result)
}

fun factorial(n: Double): Double {
    // Fatorial só é definido para inteiros não negativos.
    if (n < 0 || n != floor(n)) return Double.NaN
    // Acima de 170! o resultado não cabe em um Double; retorna logo
    // infinito para não percorrer um loop gigante.
    if (n > 170) return Double.POSITIVE_INFINITY
    var result = 1.0
    for (i in 2..n.toInt()) {
        result *= i
    }
    return result
}

// Converte o resultado para um texto amigável ao display: arredonda para
// esconder o erro binário do Double (0.1+0.2, tan 45°...), remove o ".0"
// de valores inteiros e troca NaN/Infinity por "Erro".
fun formatNumber(value: Double): String {
    if (value.isNaN() || value.isInfinite()) return "Erro"
    if (abs(value) >= 1e12) {
        return String.format(Locale.US, "%.6E", value)
    }
    return BigDecimal(value)
        .setScale(10, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()
}