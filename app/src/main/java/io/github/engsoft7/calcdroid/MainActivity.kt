package io.github.engsoft7.calcdroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CalcDroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BeautifulCalculatorScreen()
                }
            }
        }
    }
}

const val ERROR_DISPLAY = "Erro"
const val MAX_INPUT_LENGTH = 12

@Composable
fun BeautifulCalculatorScreen() {
    var displayValue by rememberSaveable { mutableStateOf("0") }
    var firstOperand by rememberSaveable { mutableStateOf("") }
    var operator by rememberSaveable { mutableStateOf("") }
    // Quando true, o próximo dígito substitui o display (após operador, "=", "%" ou erro).
    var startNewInput by rememberSaveable { mutableStateOf(false) }

    val buttonRows = listOf(
        listOf("C", "+/-", "%", "/"),
        listOf("7", "8", "9", "*"),
        listOf("4", "5", "6", "-"),
        listOf("1", "2", "3", "+"),
        listOf("0", ".", "=")
    )

    val darkTheme = isSystemInDarkTheme()
    val backgroundColor = if (darkTheme) Color(0xFF17181A) else Color(0xFFF0F0F0)
    val displayTextColor = if (darkTheme) Color.White else Color.Black
    val numberButtonColor = if (darkTheme) Color(0xFF2E3033) else Color.White
    val numberTextColor = if (darkTheme) Color.White else Color.Black
    val clearButtonColor = if (darkTheme) Color(0xFF5A5D61) else Color(0xFFD3D3D3)
    val operatorButtonColor = Color(0xFFFFA500)

    fun onButtonClick(button: String) {
        when (button) {
            "C" -> {
                displayValue = "0"
                firstOperand = ""
                operator = ""
                startNewInput = false
            }

            "+/-" -> {
                if (displayValue != ERROR_DISPLAY && displayValue != "0") {
                    displayValue = if (displayValue.startsWith("-")) {
                        displayValue.drop(1)
                    } else {
                        "-$displayValue"
                    }
                }
            }

            "%" -> {
                val value = displayValue.toDoubleOrNull()
                if (value != null) {
                    displayValue = formatResult(value / 100)
                    startNewInput = true
                }
            }

            "+", "-", "*", "/" -> {
                if (displayValue == ERROR_DISPLAY) return
                when {
                    firstOperand.isEmpty() -> {
                        firstOperand = displayValue
                        operator = button
                        startNewInput = true
                    }
                    // Operador trocado antes de digitar o segundo operando.
                    startNewInput -> operator = button
                    else -> {
                        val result = calculate(firstOperand, displayValue, operator)
                        displayValue = result
                        if (result == ERROR_DISPLAY) {
                            firstOperand = ""
                            operator = ""
                        } else {
                            firstOperand = result
                            operator = button
                        }
                        startNewInput = true
                    }
                }
            }

            "=" -> {
                if (firstOperand.isNotEmpty() && operator.isNotEmpty() &&
                    displayValue != ERROR_DISPLAY
                ) {
                    displayValue = calculate(firstOperand, displayValue, operator)
                    firstOperand = ""
                    operator = ""
                    startNewInput = true
                }
            }

            else -> { // dígitos e "."
                if (displayValue == ERROR_DISPLAY || startNewInput) {
                    displayValue = if (button == ".") "0." else button
                    startNewInput = false
                } else if (button == "." && displayValue.contains(".")) {
                    // Ignora ponto decimal duplicado.
                } else if (displayValue.length >= MAX_INPUT_LENGTH) {
                    // Display cheio; ignora novos dígitos.
                } else if (displayValue == "0" && button != ".") {
                    displayValue = button
                } else {
                    displayValue += button
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp)
    ) {
        // Display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Text(
                text = displayValue,
                textAlign = TextAlign.End,
                maxLines = 1,
                fontSize = when {
                    displayValue.length <= 8 -> 64.sp
                    displayValue.length <= 12 -> 48.sp
                    else -> 36.sp
                },
                fontWeight = FontWeight.Light,
                color = displayTextColor
            )
        }

        // Botões
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val spacing = 12.dp
            val cellSize = (maxWidth - spacing * 3) / 4
            Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                buttonRows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                        row.forEach { button ->
                            val isOperator =
                                button in listOf("+", "-", "*", "/", "=", "%", "+/-")
                            val buttonColor = when {
                                isOperator -> operatorButtonColor
                                button == "C" -> clearButtonColor
                                else -> numberButtonColor
                            }
                            val textColor = when {
                                isOperator -> Color.White
                                button == "C" && !darkTheme -> Color.Black
                                else -> numberTextColor
                            }
                            // O "0" ocupa duas células, completando a grade 4x5.
                            val buttonWidth =
                                if (button == "0") cellSize * 2 + spacing else cellSize
                            CalculatorButton(
                                text = when (button) {
                                    "*" -> "×"
                                    "/" -> "÷"
                                    else -> button
                                },
                                buttonColor = buttonColor,
                                textColor = textColor,
                                modifier = Modifier
                                    .width(buttonWidth)
                                    .height(cellSize),
                                onClick = { onButtonClick(button) }
                            )
                        }
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
    textColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(buttonColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 30.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

fun calculate(first: String, second: String, operator: String): String {
    val num1 = first.toDoubleOrNull() ?: return ERROR_DISPLAY
    val num2 = second.toDoubleOrNull() ?: return ERROR_DISPLAY
    val result = when (operator) {
        "+" -> num1 + num2
        "-" -> num1 - num2
        "*" -> num1 * num2
        "/" -> num1 / num2
        else -> return ERROR_DISPLAY
    }
    return formatResult(result)
}

fun formatResult(value: Double): String {
    if (value.isNaN() || value.isInfinite()) return ERROR_DISPLAY
    if (abs(value) >= 1e12) {
        return String.format(Locale.US, "%.6g", value)
    }
    // Arredonda para eliminar ruído de ponto flutuante (ex.: 0.1 + 0.2)
    // e remove zeros à direita para não exibir "4.0".
    return BigDecimal(value)
        .setScale(10, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()
}
