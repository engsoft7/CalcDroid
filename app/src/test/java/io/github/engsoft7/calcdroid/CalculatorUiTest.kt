package io.github.engsoft7.calcdroid

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.engsoft7.calcdroid.ui.theme.CalcDroidTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Testes de tela sem emulador (Robolectric): tocam nas teclas como o
 * usuário e conferem o que aparece. Rodam junto dos testes unitários no CI.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "en-w411dp-h891dp-port")
class CalculatorUiTest {

    @get:Rule
    val rule = createComposeRule()

    private fun start(mode: String = MODE_BASIC) {
        rule.setContent {
            CalcDroidTheme {
                var m by remember { mutableStateOf(mode) }
                BeautifulCalculatorScreen(mode = m, onModeChange = { m = it })
            }
        }
    }

    /**
     * Toca nas teclas pela descrição do TalkBack. A "2nd" é procurada pelo
     * começo, porque a descrição dela termina com ", on"/", off".
     */
    private fun keys(vararg descriptions: String) = descriptions.forEach {
        rule.onNodeWithContentDescription(it, substring = it == "Second function").performClick()
    }

    @Test
    fun contaBasicaEmIngles() {
        start()
        keys("7", "Multiply", "6", "Equals")
        rule.onNodeWithText("42").assertIsDisplayed()
        rule.onNodeWithText("7×6 =").assertIsDisplayed()
    }

    @Test
    fun separadorDecimalEmIngles() {
        start()
        keys("5", "Divide", "2", "Equals")
        rule.onNodeWithText("2.5").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "pt-rBR-w411dp-h891dp-port")
    fun separadorDecimalEmPortugues() {
        start()
        keys("5", "Dividir", "2", "Igual")
        rule.onNodeWithText("2,5").assertIsDisplayed()
        rule.onNodeWithText("Básica").assertIsDisplayed()
    }

    @Test
    fun resultadoAoVivoEnquantoDigita() {
        start()
        keys("1", "2", "Multiply", "3")
        rule.onNodeWithText("12×3").assertIsDisplayed()
        rule.onNodeWithText("36").assertIsDisplayed()   // prévia em cinza
        keys("Equals")
        rule.onNodeWithText("36").assertIsDisplayed()
        rule.onNodeWithText("12×3 =").assertIsDisplayed()
    }

    @Test
    fun alternaFracao() {
        start()
        keys("1", "Divide", "3", "Equals")
        rule.onNodeWithText("0.3333333333").assertIsDisplayed()
        rule.onNodeWithText("Fraction").performClick()
        rule.onNodeWithText("1/3").assertIsDisplayed()
        rule.onNodeWithText("Decimal").performClick()
        rule.onNodeWithText("0.3333333333").assertIsDisplayed()
    }

    @Test
    fun colaContaCopiadaDeOutroApp() {
        start()
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val cm = context.getSystemService(android.content.ClipboardManager::class.java)
        cm.setPrimaryClip(android.content.ClipData.newPlainText("x", "1,234.5 × 2"))
        rule.onNodeWithText("0").performTouchInput { longClick() }
        rule.onNodeWithText("Paste").performClick()
        rule.onNodeWithText("1234.5×2").assertIsDisplayed()
        rule.onNodeWithText("2469").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "es-w411dp-h891dp-port")
    fun emEspanhol() {
        start()
        keys("5", "Dividir", "2", "Igual")
        rule.onNodeWithText("2,5").assertIsDisplayed()
        rule.onNodeWithText("Gráfica").assertIsDisplayed()
        rule.onNodeWithText("Historial").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "fr-w411dp-h891dp-port")
    fun emFrances() {
        start()
        keys("5", "Diviser", "2", "Égal")
        rule.onNodeWithText("2,5").assertIsDisplayed()
        rule.onNodeWithText("Basique").assertIsDisplayed()
        rule.onNodeWithText("Historique").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "en-w800dp-h1280dp-port")
    fun tabletEmPeMostraVisorETeclado() {
        start(MODE_SCIENTIFIC)
        keys("7", "Plus", "8", "Equals")
        rule.onNodeWithText("15").assertIsDisplayed()
        rule.onNodeWithContentDescription("Equals").assertIsDisplayed()
    }

    @Test
    fun porcentagemComoNoCelular() {
        start()
        keys("1", "0", "0", "Minus", "1", "0", "Percent", "Equals")
        rule.onNodeWithText("90").assertIsDisplayed()
    }

    @Test
    fun historicoMostraAsContas() {
        start()
        keys("2", "Plus", "2", "Equals", "Multiply", "1", "0", "Equals")
        rule.onNodeWithText("History").performClick()
        rule.onNodeWithText("2+2 =").assertIsDisplayed()
        rule.onNodeWithText("4×10 =").assertIsDisplayed()
        // Tocar numa conta a devolve ao visor e fecha o painel.
        rule.onNodeWithText("2+2 =").performClick()
        rule.onNodeWithText("2+2").assertIsDisplayed()
    }

    @Test
    fun memoria() {
        start()
        keys("8", "Add to memory", "Clear", "2", "Multiply", "Recall memory", "Equals")
        rule.onNodeWithText("16").assertIsDisplayed()
        rule.onNodeWithText("M").assertIsDisplayed()
    }

    @Test
    fun cientificaComSegundaFuncaoERadianos() {
        start(MODE_SCIENTIFIC)
        keys("Second function", "Arcsine", "1", "Equals")
        rule.onNodeWithText("90").assertIsDisplayed()
        rule.onNodeWithText("DEG").performClick()
        keys("Clear", "Second function", "Arcsine", "1", "Equals")
        rule.onNodeWithText("1.5707963268").assertIsDisplayed()
    }

    @Test
    fun graficoDesenhaAFuncao() {
        start(MODE_GRAPH)
        keys("Sine", "Variable x", "Close parenthesis", "Equals")
        rule.onNodeWithContentDescription("Graph of f(x) = sin(x)", substring = true).assertIsDisplayed()
        rule.onNodeWithText("f(x) = sin(x)").assertIsDisplayed()
    }

    @Test
    fun graficoComDuasFuncoesERaizes() {
        start(MODE_GRAPH)
        keys("Variable x", "Equals")
        rule.onNodeWithText("g(x)").performClick()
        keys("1", "Equals")
        rule.onNodeWithText("g(x) = 1").assertIsDisplayed()
        rule.onNodeWithContentDescription("g(x) = 1.", substring = true).assertIsDisplayed()
        rule.onNodeWithContentDescription("Zeros of f: 0.", substring = true).assertIsDisplayed()
        // Voltar para a f(x) traz a expressão dela ao visor.
        rule.onNodeWithText("f(x)").performClick()
        rule.onNodeWithText("f(x) = x").assertIsDisplayed()
    }

    @Test
    fun graficoComIntervaloEscolhido() {
        start(MODE_GRAPH)
        keys("Variable x", "Equals")
        rule.onNodeWithText("Range").performClick()
        rule.onNodeWithContentDescription("x from").performTextReplacement("0")
        rule.onNodeWithContentDescription("to").performTextReplacement("5")
        rule.onNodeWithText("OK").performClick()
        rule.onNodeWithText("5 (degrees)").assertIsDisplayed()
    }

    @Test
    fun modoEstatistica() {
        start()
        rule.onNodeWithText("Statistics").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Values (x)").performTextInput("2 4 4 4 5 5 7 9")
        rule.onNodeWithText("Mean").assertIsDisplayed()
        rule.onNodeWithText("4.5").assertIsDisplayed()   // mediana
        rule.onNodeWithText("40").assertIsDisplayed()    // soma
        rule.onNodeWithContentDescription("y values (optional, for regression)").performTextInput("1 2 3 4 5 6 7 8")
        rule.onNodeWithText("Linear regression").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun modoFinanceiroParcelas() {
        start(MODE_FINANCE)
        rule.onNodeWithText("Installments").performClick()
        rule.onNodeWithContentDescription("Loan amount").performTextInput("10000")
        rule.onNodeWithContentDescription("Interest rate (% per period)").performTextInput("2")
        rule.onNodeWithContentDescription("Number of installments").performTextInput("12")
        rule.onNodeWithText("945.60").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun modoFinanceiroJurosCompostos() {
        start(MODE_FINANCE)
        rule.onNodeWithContentDescription("Initial amount").performTextInput("1000")
        rule.onNodeWithContentDescription("Interest rate (% per period)").performTextInput("1")
        rule.onNodeWithContentDescription("Periods").performTextInput("10")
        rule.onNodeWithText("1,104.62").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun conversorDeUnidades() {
        start(MODE_CONVERTER)
        // Padrão: 1 km em milhas.
        rule.onNodeWithText("0.6213711922").assertIsDisplayed()
        rule.onNodeWithText("Temperature").performClick()
        keys("Clear", "1", "0", "0")
        rule.onNodeWithText("212").assertIsDisplayed()
    }

    @Test
    fun conversorDeBases() {
        start(MODE_CONVERTER)
        // "Bases" fica no fim da fileira de categorias, que rola de lado.
        rule.onNodeWithText("Bases").performScrollTo().performClick()
        keys("Clear", "2", "5", "5")
        rule.onNodeWithText("11111111").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "en-w360dp-h640dp-port")
    fun matrizResolveSistema() {
        // Tela baixa: o resultado nasce abaixo dos botões e só aparece se a
        // tela rolar sozinha até ele.
        start(MODE_MATRIX)
        fun cell(name: String, value: String) =
            rule.onNodeWithContentDescription(name).performTextReplacement(value)
        cell("Matrix A, row 1, column 1", "2")
        cell("Matrix A, row 1, column 2", "1")
        cell("Matrix A, row 2, column 1", "1")
        cell("Matrix A, row 2, column 2", "3")
        cell("Vector b, entry 1", "3")
        cell("Vector b, entry 2", "5")
        rule.onNodeWithText("A·x=b").performScrollTo().performClick()
        // O resultado fica abaixo dos botões; a tela rola sozinha até ele.
        rule.onNodeWithText("x₁ = 0.8").assertIsDisplayed()
        rule.onNodeWithText("x₂ = 1.4").assertIsDisplayed()
    }

    @Test
    fun matriz4x4() {
        start(MODE_MATRIX)
        rule.onNodeWithText("4×4").performClick()
        rule.onNodeWithContentDescription("Matrix A, row 4, column 4").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "en-w891dp-h411dp-land")
    fun paisagemMostraTodasAsTeclas() {
        start(MODE_SCIENTIFIC)
        rule.onNodeWithContentDescription("Equals").assertIsDisplayed()
        rule.onNodeWithContentDescription("Remainder").assertIsDisplayed()
        rule.onNodeWithContentDescription("Clear memory").assertIsDisplayed()
    }
}

/**
 * Abre a Activity de verdade, com preferências, para pegar falhas na
 * inicialização. A tela padrão do Robolectric (320×470dp) também cobre
 * celulares pequenos e tela dividida: o resultado tem que caber no visor.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "en")
class MainActivityUiTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun abreEFazUmaConta() {
        rule.onNodeWithContentDescription("9").performClick()
        rule.onNodeWithContentDescription("Plus").performClick()
        rule.onNodeWithContentDescription("1").performClick()
        rule.onNodeWithContentDescription("Equals").performClick()
        rule.onNodeWithText("10").assertIsDisplayed()
    }

    @Test
    fun pedeAvaliacaoUmaVezDepoisDeUsoDeVerdade() {
        repeat(REVIEW_AFTER - 1) {
            rule.onNodeWithContentDescription("1").performClick()
            rule.onNodeWithContentDescription("Equals").performClick()
        }
        rule.onNodeWithText("Enjoying CalcDroid?").assertDoesNotExist()
        rule.onNodeWithContentDescription("1").performClick()
        rule.onNodeWithContentDescription("Equals").performClick()
        rule.onNodeWithText("Enjoying CalcDroid?").assertIsDisplayed()
        rule.onNodeWithText("Not now").performClick()
        rule.onNodeWithText("Enjoying CalcDroid?").assertDoesNotExist()
        // Não volta a pedir.
        rule.onNodeWithContentDescription("Equals").performClick()
        rule.onNodeWithText("Enjoying CalcDroid?").assertDoesNotExist()
    }
}

/** Widget: usa a mesma lógica do app e mostra a prévia do resultado. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "en")
class CalcWidgetTest {
    @Test
    fun contaNoWidget() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        var state = CalcState()
        listOf("1", "2", "*", "3").forEach { state = state.press(it) }
        org.junit.Assert.assertEquals("12×3" to "= 36", CalcWidget.displayTexts(context, state))
        state = state.press("=")
        CalcWidget.save(context, state)
        val loaded = CalcWidget.load(context)
        org.junit.Assert.assertEquals("36" to "12×3 =", CalcWidget.displayTexts(context, loaded))
    }
}
