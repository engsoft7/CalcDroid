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
    // Valor guardado pelas teclas de memória (M+, M−, MR, MC).
    val memory: Double = 0.0,
)

const val ERROR_TEXT = "Erro"
private const val MAX_LENGTH = 60

private const val BINARY_OPERATORS = "+-*/^"
// Operadores depois dos quais um "-" é o sinal do próximo número: 5×−3, 2^−1.
private const val SIGN_ALLOWED_AFTER = "*/^"
// Tokens que entram e saem inteiros com uma tecla (apagar remove o token todo).
private val WHOLE_TOKENS = listOf(
    "asin(", "acos(", "atan(", "abs(", "sin(", "cos(", "tan(", "log(", "ln(", "√(", "^(-1)", "mod"
)
private val FUNCTION_KEYS = setOf("sin", "cos", "tan", "asin", "acos", "atan", "abs", "ln", "log", "√")
private val CONTINUES_RESULT = setOf("+", "-", "*", "/", "^", "%", "!", "x²", "x⁻¹", "mod")
// Teclas que precisam de um operando à esquerda.
private val NEEDS_OPERAND = setOf("*", "/", "^", "%", "!", ")", "x²", "x⁻¹", "mod")
private val POSTFIX_KEYS = setOf("%", "!", ")", "x²", "x⁻¹", "mod")
// Expoente que ficou pendurado ao apagar um resultado como "1.5E+12".
private val DANGLING_EXPONENT = Regex("E[+-]?$")

fun CalcState.press(key: String, graphMode: Boolean = false, degrees: Boolean = true): CalcState =
    when (key) {
        // Limpar não apaga a memória, como nas calculadoras de verdade.
        "C" -> CalcState(memory = memory)
        "⌫" -> backspace()
        "=" -> evaluate(graphMode, degrees)
        "MC" -> copy(memory = 0.0)
        "M+", "M-" -> {
            val value = currentValue(degrees)
            if (value == null) this
            else copy(memory = if (key == "M+") memory + value else memory - value)
        }
        "MR" -> recallMemory()
        else -> append(key)
    }

/** Valor do que está no visor, ou null se não for uma conta válida. */
fun CalcState.currentValue(degrees: Boolean = true): Double? {
    if (expression.isEmpty() || expression == ERROR_TEXT) return null
    return try {
        ExpressionEvaluator.evaluate(autoCloseParentheses(expression), degrees = degrees)
            .takeIf { it.isFinite() }
    } catch (e: Exception) {
        null
    }
}

private fun CalcState.recallMemory(): CalcState {
    val text = formatNumber(memory)
    // Depois de "=" (ou de um erro) a memória começa uma conta nova.
    val base = if (justEvaluated || expression == ERROR_TEXT) {
        copy(expression = "", lastExpression = "", justEvaluated = false)
    } else {
        this
    }
    // Um número negativo logo depois de outro número viraria subtração
    // ("5" + "-3" = "5-3"); entre parênteses fica multiplicação: 5(-3).
    val token = if (memory < 0 && base.expression.lastOrNull()?.let { it.isDigit() || it == ')' } == true) {
        "($text)"
    } else {
        text
    }
    return base.appendLimited(token)
}

private fun CalcState.backspace(): CalcState {
    // Apagar em cima de um resultado passa a editá-lo; a linha "... =" acima
    // deixaria de corresponder ao que se vê.
    val base = if (justEvaluated) copy(lastExpression = "", justEvaluated = false) else this
    if (expression == ERROR_TEXT) return base.copy(expression = "")
    // Funções entram inteiras ("sin("), então saem inteiras.
    val function = WHOLE_TOKENS.firstOrNull { expression.endsWith(it) }
    val trimmed = expression.dropLast(function?.length ?: 1)
    return base.copy(expression = trimmed.replace(DANGLING_EXPONENT, ""))
}

private fun CalcState.evaluate(graphMode: Boolean, degrees: Boolean): CalcState {
    if (expression.isEmpty() || expression == ERROR_TEXT) return this
    val closed = autoCloseParentheses(expression)
    if (graphMode) return copy(expression = closed, plotExpression = closed)
    val result = try {
        formatNumber(ExpressionEvaluator.evaluate(closed, degrees = degrees))
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
        in FUNCTION_KEYS -> "$key("
        "x²" -> "^2"
        "x⁻¹" -> "^(-1)"
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
    // Pós-fixos logo depois de um operador ("5+!") não têm sobre o que agir;
    // já um operador binário ali substitui o anterior (tratado mais abaixo).
    if (last != null && last in BINARY_OPERATORS && key in POSTFIX_KEYS) return state
    if (expr.endsWith("mod") && key in NEEDS_OPERAND) return state
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

/**
 * Troca a notação interna pela do visor: × e ÷, vírgula decimal (pt-BR) e
 * os nomes das funções inversas.
 */
fun prettyExpression(expression: String): String =
    expression
        .replace("asin(", "sin⁻¹(")
        .replace("acos(", "cos⁻¹(")
        .replace("atan(", "tan⁻¹(")
        .replace("abs(", "|x|(")
        .replace("^(-1)", "⁻¹")
        .replace("mod", " mod ")
        .replace('*', '×')
        .replace('/', '÷')
        .replace('.', ',')
