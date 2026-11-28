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
    // Expressões desenhadas no modo gráfico: f(x) e a segunda, g(x).
    val plotExpression: String = "",
    val plotExpression2: String = "",
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

fun CalcState.press(
    key: String,
    graphMode: Boolean = false,
    degrees: Boolean = true,
    // No gráfico, "=" desenha a g(x) em vez da f(x).
    secondPlot: Boolean = false,
): CalcState =
    when (key) {
        // Limpar não apaga a memória, como nas calculadoras de verdade.
        "C" -> CalcState(memory = memory)
        "⌫" -> backspace()
        "=" -> evaluate(graphMode, degrees, secondPlot)
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

private fun CalcState.evaluate(graphMode: Boolean, degrees: Boolean, secondPlot: Boolean = false): CalcState {
    // No gráfico, "=" com o visor vazio apaga a função.
    if (graphMode && expression.isEmpty()) {
        return if (secondPlot) copy(plotExpression2 = "") else copy(plotExpression = "")
    }
    if (expression.isEmpty() || expression == ERROR_TEXT) return this
    val closed = autoCloseParentheses(expression)
    if (graphMode) {
        return if (secondPlot) copy(expression = closed, plotExpression2 = closed)
        else copy(expression = closed, plotExpression = closed)
    }
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
 * Troca a notação interna pela do visor: × e ÷, o separador decimal do idioma e
 * os nomes das funções inversas.
 */
fun prettyExpression(expression: String, decimalSep: Char = ','): String =
    expression
        .replace("asin(", "sin⁻¹(")
        .replace("acos(", "cos⁻¹(")
        .replace("atan(", "tan⁻¹(")
        .replace("abs(", "|x|(")
        .replace("^(-1)", "⁻¹")
        .replace("mod", " mod ")
        .replace('*', '×')
        .replace('/', '÷')
        .replace('.', decimalSep)

// ---- Resultado ao vivo ----

private val PLAIN_NUMBER = Regex("^-?[0-9.]+(E[+-]?[0-9]+)?$")

/**
 * Resultado da conta enquanto ela é digitada, para mostrar em cinza antes do
 * "=". Null quando não há o que antecipar: visor vazio, resultado já
 * mostrado, número sozinho ou conta ainda inválida.
 */
fun CalcState.preview(degrees: Boolean = true): String? {
    if (justEvaluated || expression.isEmpty() || expression == ERROR_TEXT) return null
    if (PLAIN_NUMBER.matches(expression)) return null
    val value = currentValue(degrees) ?: return null
    return formatNumber(value).takeIf { it != ERROR_TEXT }
}

// ---- Colar ----

private const val PASTE_ALLOWED = "0123456789.+-*/^()%!πe"

/**
 * Converte um texto copiado de outro app numa conta que o teclado entende,
 * ou null se não for uma conta válida. Aceita ×, ÷ e −, espaços, e números
 * com separador de milhar ("1.234,5" ou "1,234.5") ou decimal do idioma.
 */
fun sanitizePasted(text: String, decimalSep: Char = ','): String? {
    var t = text.trim()
        .replace('×', '*').replace('÷', '/').replace('−', '-').replace('–', '-')
        .replace(" ", "").replace(" ", "")
    if (t.isEmpty()) return null
    t = normalizeSeparators(t, decimalSep)
    if (t.any { it !in PASTE_ALLOWED } || t.length > 60) return null
    return try {
        ExpressionEvaluator.evaluate(autoCloseParentheses(t))
        t
    } catch (e: Exception) {
        null
    }
}

/** Deixa só '.' como separador decimal, tirando os separadores de milhar. */
private fun normalizeSeparators(t: String, decimalSep: Char): String {
    val hasDot = '.' in t
    val hasComma = ',' in t
    return when {
        // Os dois aparecem: o último de cada número é o decimal.
        hasDot && hasComma -> {
            val decimal = if (t.lastIndexOf(',') > t.lastIndexOf('.')) ',' else '.'
            val thousands = if (decimal == ',') '.' else ','
            t.replace(thousands.toString(), "").replace(decimal, '.')
        }
        hasComma -> commaOrDotAlone(t, ',', decimalSep)
        hasDot -> commaOrDotAlone(t, '.', decimalSep)
        else -> t
    }
}

private fun commaOrDotAlone(t: String, sep: Char, decimalSep: Char): String {
    // O separador do idioma é decimal. O outro também é, a não ser que
    // apareça em grupos de exatamente 3 dígitos ("1.234.567" no inglês
    // seria milhar com vírgula; "12.500" no português, milhar com ponto).
    if (sep == decimalSep) return t.replace(sep, '.')
    val thousandsPattern = Regex("\\d{1,3}(\\${sep}\\d{3})+(?!\\d)")
    val looksThousands = thousandsPattern.containsMatchIn(t) && !Regex("\\${sep}\\d{1,2}(?!\\d)|\\${sep}\\d{4,}").containsMatchIn(t)
    return if (looksThousands) t.replace(sep.toString(), "") else t.replace(sep, '.')
}

/** Cola no visor: depois de um resultado substitui, senão acrescenta. */
fun CalcState.paste(text: String): CalcState {
    val base = if (justEvaluated || expression == ERROR_TEXT) copy(expression = "", lastExpression = "", justEvaluated = false) else this
    val joined = base.expression + text
    return if (joined.length <= MAX_LENGTH) base.copy(expression = joined) else base
}

// ---- Frações ----

/**
 * O resultado como fração "a/b" (ex.: 0,333… → 1/3), por frações contínuas
 * com denominador até [maxDenominator]. Null para inteiros, números grandes
 * ou valores que não são uma fração simples (como π).
 */
fun toFraction(value: Double, maxDenominator: Long = 10_000): String? {
    if (!value.isFinite() || value == Math.rint(value) || kotlin.math.abs(value) >= 1e9) return null
    val sign = if (value < 0) "-" else ""
    val x = kotlin.math.abs(value)
    var h0 = 0L; var h1 = 1L
    var k0 = 1L; var k1 = 0L
    var rest = x
    repeat(40) {
        val a = kotlin.math.floor(rest).toLong()
        val h2 = a * h1 + h0
        val k2 = a * k1 + k0
        if (k2 > maxDenominator) return null
        h0 = h1; h1 = h2; k0 = k1; k1 = k2
        if (kotlin.math.abs(x - h1.toDouble() / k1) <= 1e-9 * maxOf(1.0, x)) {
            return if (k1 == 1L) null else "$sign$h1/$k1"
        }
        val frac = rest - a
        if (frac < 1e-15) return null
        rest = 1 / frac
    }
    return null
}
