package io.github.engsoft7.calcdroid

/**
 * Estado da calculadora de expressões (modos básica, científica e gráfico).
 *
 * É imutável e não depende de Android nem de Compose: cada tecla produz um
 * estado novo via [press], o que deixa toda a lógica do teclado coberta por
 * testes unitários comuns.
 */
data class CalcState(
    val expression: String = "",
    // Conta que gerou o resultado exibido ("2+3 ="); vazia fora disso.
    val lastExpression: String = "",
    // Após "=", um dígito ou função começa um cálculo novo; um operador
    // continua a conta em cima do resultado exibido.
    val justEvaluated: Boolean = false,
    // Expressão desenhada no modo gráfico.
    val plotExpression: String = "",
)

const val ERROR_TEXT = "Erro"
private const val MAX_LENGTH = 60

private const val BINARY_OPERATORS = "+-*/^"
// Operadores depois dos quais um "-" é o sinal do próximo número: 5×−3, 2^−1.
private const val SIGN_ALLOWED_AFTER = "*/^"
private val FUNCTION_TOKENS = listOf("sin(", "cos(", "tan(", "log(", "ln(", "√(")
private val CONTINUES_RESULT = setOf("+", "-", "*", "/", "^", "%", "!")
// Teclas que precisam de um operando à esquerda.
private val NEEDS_OPERAND = setOf("*", "/", "^", "%", "!", ")")
// Expoente que ficou pendurado ao apagar um resultado como "1.5E+12".
private val DANGLING_EXPONENT = Regex("E[+-]?$")

fun CalcState.press(key: String, graphMode: Boolean = false): CalcState = when (key) {
    "C" -> CalcState()
    "⌫" -> backspace()
    "=" -> evaluate(graphMode)
    else -> append(key)
}

private fun CalcState.backspace(): CalcState {
    // Apagar em cima de um resultado passa a editá-lo; a linha "... =" acima
    // deixaria de corresponder ao que se vê.
    val base = if (justEvaluated) copy(lastExpression = "", justEvaluated = false) else this
    if (expression == ERROR_TEXT) return base.copy(expression = "")
    // Funções entram inteiras ("sin("), então saem inteiras.
    val function = FUNCTION_TOKENS.firstOrNull { expression.endsWith(it) }
    val trimmed = expression.dropLast(function?.length ?: 1)
    return base.copy(expression = trimmed.replace(DANGLING_EXPONENT, ""))
}

private fun CalcState.evaluate(graphMode: Boolean): CalcState {
    if (expression.isEmpty() || expression == ERROR_TEXT) return this
    val closed = autoCloseParentheses(expression)
    if (graphMode) return copy(expression = closed, plotExpression = closed)
    val result = try {
        formatNumber(ExpressionEvaluator.evaluate(closed))
    } catch (e: Exception) {
        ERROR_TEXT
    }
    return copy(expression = result, lastExpression = "$closed =", justEvaluated = true)
}

private fun CalcState.append(key: String): CalcState {
    var state = this
    if (justEvaluated) {
        state = if (expression == ERROR_TEXT || key !in CONTINUES_RESULT) {
            // Cálculo novo: some também a conta anterior, que só faz sentido
            // em cima do resultado que ela produziu.
            state.copy(expression = "", lastExpression = "", justEvaluated = false)
        } else {
            state.copy(justEvaluated = false)
        }
    }
    val expr = state.expression
    val last = expr.lastOrNull()

    var toAppend = when (key) {
        "sin", "cos", "tan", "ln", "log", "√" -> "$key("
        else -> key
    }

    if (key == ".") {
        val currentNumber = expr.takeLastWhile { it.isDigit() || it == '.' }
        // Impede um segundo ponto no mesmo número, inclusive no expoente de
        // um resultado em notação científica ("1.5E+12.").
        if (currentNumber.contains('.')) return state
        if (expr.dropLast(currentNumber.length).contains(DANGLING_EXPONENT)) return state
        if (currentNumber.isEmpty()) toAppend = "0."
    }

    // "0" sozinho é substituído pelo dígito, evitando "007".
    if (expr == "0" && key.length == 1 && key[0].isDigit()) {
        return state.copy(expression = key)
    }

    // Pós-fixos e fechamento não fazem sentido sem operando à esquerda.
    if ((last == null || last == '(') && key in NEEDS_OPERAND) return state
    if (last != null && last in BINARY_OPERATORS && key in setOf("%", "!", ")")) return state
    // Só fecha parêntese que esteja aberto.
    if (key == ")" && expr.count { it == '(' } <= expr.count { it == ')' }) return state

    if (key.length == 1 && key[0] in BINARY_OPERATORS && last != null && last in BINARY_OPERATORS) {
        if (key == "-") {
            // "5×" + "−" vira "5×−" (sinal do próximo número); "5−" + "−"
            // fica como está.
            return if (last in SIGN_ALLOWED_AFTER) state.appendLimited("-") else state
        }
        // Dois operadores seguidos: o novo substitui o anterior, levando
        // junto um sinal pendurado ("5×−" + "+" vira "5+").
        var trimmed = expr.dropLast(1)
        if (last == '-' && trimmed.lastOrNull()?.let { it in SIGN_ALLOWED_AFTER } == true) {
            trimmed = trimmed.dropLast(1)
        }
        return state.copy(expression = trimmed + key)
    }

    return state.appendLimited(toAppend)
}

private fun CalcState.appendLimited(text: String): CalcState =
    if (expression.length + text.length <= MAX_LENGTH) copy(expression = expression + text) else this

/** Troca os operadores ASCII pelos símbolos usados no visor. */
fun prettyExpression(expression: String): String =
    expression.replace('*', '×').replace('/', '÷')
