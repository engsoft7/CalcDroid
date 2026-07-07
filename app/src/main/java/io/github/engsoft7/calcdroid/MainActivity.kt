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
                    BeautifulCalculatorScreen()
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
fun BeautifulCalculatorScreen() {
    var displayValue by remember { mutableStateOf("0") }
    var firstOperand by remember { mutableStateOf("") }
    var operator by remember { mutableStateOf("") }

    val buttonRows = listOf(
        listOf("C", "+/-", "%", "/"),
        listOf("7", "8", "9", "*"),
        listOf("4", "5", "6", "-"),
        listOf("1", "2", "3", "+"),
        listOf("0", ".", "=")
    )

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
        // que o espaço disponível (reservando área mínima para o display),
        // para nenhuma linha do teclado ficar cortada embaixo.
        val cellWidth = (maxWidth - spacing * 3) / 4
        val cellMaxHeight = (maxHeight - 88.dp - spacing * 5) / 5
        val buttonHeight = minOf(cellWidth, cellMaxHeight).coerceAtLeast(40.dp)

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
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
                    fontSize = 60.sp, // Tamanho maior para o display
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
                            "+", "-", "*", "/", "=", "%", "+/-" -> Color(0xFFFFA500) // Laranja para operadores
                            "C" -> Color(0xFFD3D3D3) // Cinza claro para o C
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
                                    }

                                    "+/-" -> {
                                        displayValue = if (displayValue.startsWith("-")) {
                                            displayValue.drop(1)
                                        } else {
                                            "-$displayValue"
                                        }
                                    }

                                    "%" -> {
                                        displayValue = (displayValue.toDouble() / 100).toString()
                                    }

                                    "+", "-", "*", "/" -> {
                                        if (firstOperand.isEmpty()) {
                                            firstOperand = displayValue
                                            operator = button
                                            displayValue = "0"
                                        } else {
                                            val result = calculate(firstOperand, displayValue, operator)
                                            displayValue = result
                                            firstOperand = result
                                            operator = button
                                        }
                                    }

                                    "=" -> {
                                        if (firstOperand.isNotEmpty() && operator.isNotEmpty()) {
                                            displayValue = calculate(firstOperand, displayValue, operator)
                                            firstOperand = ""
                                            operator = ""
                                        }
                                    }

                                    else -> {
                                        if (displayValue == "0" && button != ".") {
                                            displayValue = button
                                        } else {
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
            fontSize = 30.sp, // Tamanho maior para os botões
            fontWeight = FontWeight.Medium, // Fonte mais grossa
            color = if (buttonColor == Color.White) Color.Black else Color.White // Cor do texto
        )
    }
}

fun calculate(first: String, second: String, operator: String): String {
    val num1 = first.toDouble()
    val num2 = second.toDouble()
    return when (operator) {
        "+" -> (num1 + num2).toString()
        "-" -> (num1 - num2).toString()
        "*" -> (num1 * num2).toString()
        "/" -> (num1 / num2).toString()
        else -> "0"
    }
}