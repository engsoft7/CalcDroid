package com.mayconlimasan.calcdroid

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import com.mayconlimasan.calcdroid.ui.theme.CalcDroidTheme

class MainActivity() : ComponentActivity(), Parcelable {
    constructor(parcel: Parcel) : this() {
    }

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

    val buttons = listOf(
        "C", "+/-", "%", "/",
        "7", "8", "9", "*",
        "4", "5", "6", "-",
        "1", "2", "3", "+",
        "0", ".", "="
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F0F0)) // Cor de fundo mais clara
            .padding(16.dp)
    ) {
        // Display
        Text(
            text = displayValue,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            textAlign = TextAlign.End,
            fontSize = 60.sp, // Tamanho maior para o display
            fontWeight = FontWeight.Light, // Fonte mais fina
            color = Color.Black
        )

        // Buttons
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp), // Mais espaçamento
            horizontalArrangement = Arrangement.spacedBy(12.dp), // Mais espaçamento
            contentPadding = PaddingValues(0.dp)
        ) {
            items(buttons.size) { index ->
                val button = buttons[index]
                val buttonColor = when (button) {
                    "+", "-", "*", "/", "=", "%", "+/-" -> Color(0xFFFFA500) // Laranja para operadores
                    "C" -> Color(0xFFD3D3D3) // Cinza claro para o C
                    else -> Color.White // Branco para números
                }
                CalculatorButton(
                    text = button,
                    buttonColor = buttonColor,
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
        }
    }
}

@Composable
fun CalculatorButton(text: String, buttonColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .shadow(4.dp, RoundedCornerShape(16.dp)) // Sombra
            .clip(RoundedCornerShape(16.dp)) // Cantos arredondados
            .background(buttonColor) // Cor do botão
            .aspectRatio(1f)
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