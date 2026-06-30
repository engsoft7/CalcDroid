package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.calc.CalculatorEngine
import com.example.myapplication.calc.CalculatorInput
import com.example.myapplication.ui.AuroraBackground
import com.example.myapplication.ui.BackspaceIcon
import com.example.myapplication.ui.CalcButton
import com.example.myapplication.ui.HistoryEntry
import com.example.myapplication.ui.HistoryIcon
import com.example.myapplication.ui.HistoryPanel
import com.example.myapplication.ui.KeyKind
import com.example.myapplication.ui.theme.AccentText
import com.example.myapplication.ui.theme.DisplayPrimary
import com.example.myapplication.ui.theme.DisplaySecondary
import com.example.myapplication.ui.theme.ErrorText
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CalcDroidScreen()
                }
            }
        }
    }
}

@Composable
fun CalcDroidScreen() {
    var expression by rememberSaveable { mutableStateOf("") }
    var resultShown by rememberSaveable { mutableStateOf(false) }
    // "expr|result" pairs; explicit Saver avoids relying on the runtime type of List<String>.
    val historyListSaver = remember {
        listSaver<List<String>, String>(save = { it }, restore = { it })
    }
    var history by rememberSaveable(stateSaver = historyListSaver) { mutableStateOf(listOf()) }
    var historyVisible by rememberSaveable { mutableStateOf(false) }

    val entries = remember(history) {
        history.map { val (e, r) = it.split("|", limit = 2); HistoryEntry(e, r) }
    }

    val display = if (expression.isEmpty()) "0" else CalculatorInput.formatForDisplay(expression)
    val preview = if (!resultShown && expression.isNotEmpty()) {
        CalculatorInput.formatForDisplay(CalculatorEngine.preview(expression))
    } else ""

    fun commit(newExpr: String, fromResult: Boolean = false) {
        expression = newExpr
        resultShown = fromResult
    }

    fun onDigit(d: Char) {
        val base = if (resultShown) "" else expression
        commit(CalculatorInput.appendDigit(base, d))
    }

    fun onDecimal() {
        val base = if (resultShown) "" else expression
        commit(CalculatorInput.appendDecimal(base))
    }

    fun onOperator(op: Char) {
        // After a normal "=" the result becomes the new first operand (chaining);
        // after an error there is nothing usable to chain from, so start over.
        val isErrorState = resultShown && expression == CalculatorEngine.ERROR
        val base = if (isErrorState) "" else expression
        resultShown = false
        commit(CalculatorInput.appendOperator(base, op))
    }

    fun onPercent() {
        commit(CalculatorInput.appendPercent(expression))
    }

    fun onToggleSign() {
        commit(CalculatorInput.toggleSign(expression))
    }

    fun onBackspace() {
        if (resultShown) {
            commit("")
        } else {
            commit(CalculatorInput.backspace(expression))
        }
    }

    fun onClear() {
        commit("")
    }

    fun onEquals() {
        if (expression.isEmpty()) return
        val result = CalculatorEngine.evaluate(expression)
        if (result != CalculatorEngine.ERROR) {
            history = history + "${CalculatorInput.formatForDisplay(expression)}|$result"
        }
        commit(result, fromResult = true)
    }

    fun onHistorySelect(entry: HistoryEntry) {
        commit(entry.result, fromResult = true)
        historyVisible = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AuroraBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            TopBar(
                historyVisible = historyVisible,
                onToggleHistory = { historyVisible = !historyVisible }
            )

            HistoryPanel(
                entries = entries,
                visible = historyVisible,
                onSelect = ::onHistorySelect,
                onClear = { history = emptyList() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )

            DisplayArea(
                expression = display,
                preview = preview,
                isError = expression.isNotEmpty() && resultShown && display == CalculatorEngine.ERROR,
                canDelete = expression.isNotEmpty(),
                onBackspace = ::onBackspace,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            Keypad(
                onDigit = ::onDigit,
                onDecimal = ::onDecimal,
                onOperator = ::onOperator,
                onPercent = ::onPercent,
                onToggleSign = ::onToggleSign,
                onClear = ::onClear,
                onEquals = ::onEquals,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun TopBar(
    historyVisible: Boolean,
    onToggleHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "CalcDroid",
            style = MaterialTheme.typography.titleLarge,
            color = DisplayPrimary,
            fontWeight = FontWeight.SemiBold
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (historyVisible) AccentText.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.06f))
                .clickable(onClick = onToggleHistory),
            contentAlignment = Alignment.Center
        ) {
            HistoryIcon(
                color = if (historyVisible) AccentText else DisplaySecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun DisplayArea(
    expression: String,
    preview: String,
    isError: Boolean,
    canDelete: Boolean,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(vertical = 12.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.End
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(40.dp)) {
            Text(
                text = preview,
                style = MaterialTheme.typography.headlineMedium,
                color = DisplaySecondary,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterEnd)
                    .padding(end = if (canDelete) 44.dp else 0.dp)
            )
            if (canDelete) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onBackspace),
                    contentAlignment = Alignment.Center
                ) {
                    BackspaceIcon(color = DisplaySecondary, modifier = Modifier.size(18.dp))
                }
            }
        }

        AnimatedContent(
            targetState = expression,
            transitionSpec = {
                (fadeIn() + slideInVertically { it / 4 }) togetherWith
                    (fadeOut() + slideOutVertically { -it / 4 })
            },
            label = "display"
        ) { text ->
            AutoSizeDisplayText(
                text = text,
                color = if (isError) ErrorText else DisplayPrimary
            )
        }
    }
}

/** Shrinks the font as the expression grows so long numbers never clip off-screen. */
@Composable
private fun AutoSizeDisplayText(text: String, color: Color) {
    val baseSize = 64
    val fontSize = remember(text) {
        val shrink = when {
            text.length <= 9 -> 0
            text.length <= 13 -> 16
            text.length <= 18 -> 28
            else -> 36
        }
        (baseSize - shrink).coerceAtLeast(26)
    }
    BasicText(
        text = text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(
            color = color,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.End
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun Keypad(
    onDigit: (Char) -> Unit,
    onDecimal: () -> Unit,
    onOperator: (Char) -> Unit,
    onPercent: () -> Unit,
    onToggleSign: () -> Unit,
    onClear: () -> Unit,
    onEquals: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = 12.dp
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        KeyRow(spacing) {
            FunctionKey("C", Modifier.weight(1f).aspectRatio(1f), onClick = onClear)
            FunctionKey("+/−", Modifier.weight(1f).aspectRatio(1f), onClick = onToggleSign)
            FunctionKey("%", Modifier.weight(1f).aspectRatio(1f), onClick = onPercent)
            OperatorKey(CalculatorEngine.DIVIDE.toString(), Modifier.weight(1f).aspectRatio(1f)) {
                onOperator(CalculatorEngine.DIVIDE)
            }
        }
        KeyRow(spacing) {
            DigitKey("7", Modifier.weight(1f).aspectRatio(1f), onClick = { onDigit('7') })
            DigitKey("8", Modifier.weight(1f).aspectRatio(1f), onClick = { onDigit('8') })
            DigitKey("9", Modifier.weight(1f).aspectRatio(1f), onClick = { onDigit('9') })
            OperatorKey(CalculatorEngine.MULTIPLY.toString(), Modifier.weight(1f).aspectRatio(1f)) {
                onOperator(CalculatorEngine.MULTIPLY)
            }
        }
        KeyRow(spacing) {
            DigitKey("4", Modifier.weight(1f).aspectRatio(1f), onClick = { onDigit('4') })
            DigitKey("5", Modifier.weight(1f).aspectRatio(1f), onClick = { onDigit('5') })
            DigitKey("6", Modifier.weight(1f).aspectRatio(1f), onClick = { onDigit('6') })
            OperatorKey(CalculatorEngine.MINUS.toString(), Modifier.weight(1f).aspectRatio(1f)) {
                onOperator(CalculatorEngine.MINUS)
            }
        }
        KeyRow(spacing) {
            DigitKey("1", Modifier.weight(1f).aspectRatio(1f), onClick = { onDigit('1') })
            DigitKey("2", Modifier.weight(1f).aspectRatio(1f), onClick = { onDigit('2') })
            DigitKey("3", Modifier.weight(1f).aspectRatio(1f), onClick = { onDigit('3') })
            OperatorKey(CalculatorEngine.PLUS.toString(), Modifier.weight(1f).aspectRatio(1f)) {
                onOperator(CalculatorEngine.PLUS)
            }
        }
        KeyRow(spacing) {
            DigitKey("0", Modifier.weight(2.18f).aspectRatio(2.18f), onClick = { onDigit('0') }, alignStart = true)
            DigitKey(".", Modifier.weight(1f).aspectRatio(1f), onClick = onDecimal)
            EqualsKey(Modifier.weight(1f).aspectRatio(1f), onClick = onEquals)
        }
    }
}

@Composable
private fun KeyRow(spacing: Dp, content: @Composable Row.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        content = content
    )
}

@Composable
private fun DigitKey(
    label: String,
    modifier: Modifier,
    onClick: () -> Unit,
    alignStart: Boolean = false
) {
    CalcButton(kind = KeyKind.DIGIT, onClick = onClick, modifier = modifier) {
        Text(
            text = label,
            fontSize = 28.sp,
            fontWeight = FontWeight.Medium,
            color = DisplayPrimary,
            modifier = if (alignStart) Modifier.padding(start = 28.dp) else Modifier
        )
    }
}

@Composable
private fun FunctionKey(label: String, modifier: Modifier, onClick: () -> Unit) {
    CalcButton(kind = KeyKind.FUNCTION, onClick = onClick, modifier = modifier) {
        Text(text = label, fontSize = 24.sp, fontWeight = FontWeight.Medium, color = AccentText)
    }
}

@Composable
private fun OperatorKey(label: String, modifier: Modifier, onClick: () -> Unit) {
    CalcButton(kind = KeyKind.OPERATOR, onClick = onClick, modifier = modifier) {
        Text(text = label, fontSize = 28.sp, fontWeight = FontWeight.Medium, color = Color.White)
    }
}

@Composable
private fun EqualsKey(modifier: Modifier, onClick: () -> Unit) {
    CalcButton(kind = KeyKind.EQUALS, onClick = onClick, modifier = modifier) {
        Text(text = "=", fontSize = 30.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}
