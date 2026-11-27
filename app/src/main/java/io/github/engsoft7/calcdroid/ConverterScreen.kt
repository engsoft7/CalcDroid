package io.github.engsoft7.calcdroid

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.engsoft7.calcdroid.ui.theme.CalcColors

private const val BASES = "bases"
private val BASE_NAMES = mapOf(2 to R.string.base_bin, 8 to R.string.base_oct, 10 to R.string.base_dec, 16 to R.string.base_hex)

/** Entrada do conversor: dígitos, um ponto decimal e sinal na frente. */
fun converterInput(current: String, key: String, base: Int?): String {
    val text = if (current == "0") "" else current
    return when (key) {
        "AC" -> "0"
        "⌫" -> text.dropLast(1).ifEmpty { "0" }.let { if (it == "-") "0" else it }
        "±" -> when {
            current == "0" -> "0"
            current.startsWith("-") -> current.drop(1)
            else -> "-$current"
        }
        "." -> when {
            base != null -> current // bases só aceitam inteiros
            current.contains('.') -> current
            else -> "$current."
        }
        else -> {
            val c = key.single()
            val valid = if (base != null) UnitConverter.isValidDigit(c, base) else c.isDigit()
            if (!valid || text.length >= 30) current else text + c
        }
    }
}

@Composable
fun ConverterScreen(colors: CalcColors, decimalSep: Char, modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    var categoryId by rememberSaveable { mutableStateOf("length") }
    var fromId by rememberSaveable { mutableStateOf("km") }
    var toId by rememberSaveable { mutableStateOf("mi") }
    var fromBase by rememberSaveable { mutableStateOf(10) }
    var toBase by rememberSaveable { mutableStateOf(2) }
    var input by rememberSaveable { mutableStateOf("1") }

    val isBases = categoryId == BASES
    val category = if (isBases) null else UnitConverter.category(categoryId)
    val from = category?.units?.firstOrNull { it.id == fromId } ?: category?.units?.get(0)
    val to = category?.units?.firstOrNull { it.id == toId } ?: category?.units?.getOrNull(1)

    val invalid = stringResource(R.string.converter_invalid)
    val output: String = if (isBases) {
        UnitConverter.convertBase(input, fromBase, toBase) ?: invalid
    } else {
        val value = input.toDoubleOrNull()
        if (value == null || from == null || to == null) invalid
        else prettyNumber(formatNumber(UnitConverter.convert(value, from, to)), decimalSep)
    }
    val shownInput = if (isBases) input else input.replace('.', decimalSep)

    fun selectCategory(id: String) {
        categoryId = id
        input = "1"
        if (id != BASES) {
            val units = UnitConverter.category(id).units
            fromId = units[0].id
            toId = units[1].id
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Categorias
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            UnitConverter.categories.forEach { cat ->
                SmallChip(stringResource(cat.name), categoryId == cat.id, colors) { selectCategory(cat.id) }
            }
            SmallChip(stringResource(R.string.converter_bases), isBases, colors) { selectCategory(BASES) }
        }

        // Valores: origem em cima, resultado embaixo
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.card)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isBases) {
                BaseSelector(stringResource(R.string.converter_from), fromBase, colors) {
                    fromBase = it
                    input = "0"
                }
            } else {
                UnitSelector(stringResource(R.string.converter_from), category!!, from!!, colors) { fromId = it }
            }
            ValueText(shownInput, colors, big = true)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text(
                    text = "⇅",
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(role = Role.Button, onClickLabel = stringResource(R.string.converter_swap)) {
                            if (isBases) {
                                val converted = UnitConverter.convertBase(input, fromBase, toBase)
                                val old = fromBase
                                fromBase = toBase
                                toBase = old
                                if (converted != null) input = converted
                            } else {
                                val old = fromId
                                fromId = toId
                                toId = old
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    fontSize = 24.sp,
                    color = colors.accent
                )
            }
            if (isBases) {
                BaseSelector(stringResource(R.string.converter_to), toBase, colors) { toBase = it }
            } else {
                UnitSelector(stringResource(R.string.converter_to), category!!, to!!, colors) { toId = it }
            }
            ValueText(output, colors, big = false)
        }

        // Teclado
        val hexKeys = isBases && fromBase == 16
        val rows = buildList {
            if (hexKeys) {
                add(listOf("A", "B", "C", "D"))
                add(listOf("E", "F"))
            }
            add(listOf("7", "8", "9", "⌫"))
            add(listOf("4", "5", "6", "AC"))
            add(listOf("1", "2", "3", "±"))
            add(listOf("0", "."))
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            Keypad(
                rows = rows,
                onKey = { key ->
                    haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                    input = converterInput(input, key, if (isBases) fromBase else null)
                },
                colors = colors,
                decimalSep = decimalSep,
                modifier = Modifier.fillMaxSize(),
                spacing = 10.dp,
                compact = maxHeight < 320.dp
            )
        }
    }
}

@Composable
private fun ValueText(text: String, colors: CalcColors, big: Boolean) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.End,
        maxLines = 2,
        fontSize = when {
            !big -> if (text.length > 16) 20.sp else 28.sp
            text.length > 16 -> 26.sp
            else -> 36.sp
        },
        fontWeight = if (big) FontWeight.Light else FontWeight.Medium,
        color = if (big) colors.text else colors.accent
    )
}

@Composable
private fun SmallChip(label: String, selected: Boolean, colors: CalcColors, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) colors.functionKey else colors.chip)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(label, fontSize = 13.sp, color = if (selected) colors.keyText else colors.chipText)
    }
}

@Composable
private fun SelectorRow(caption: String, value: String, colors: CalcColors, onOpen: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.DropdownList, onClick = onOpen)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$caption: ", fontSize = 14.sp, color = colors.secondaryText)
        Text(value, modifier = Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.text)
        Text("▾", fontSize = 15.sp, color = colors.secondaryText)
    }
}

private fun unitLabel(name: String, symbol: String) = if (symbol.isEmpty()) name else "$name ($symbol)"

@Composable
private fun UnitSelector(
    caption: String,
    category: UnitCategory,
    selected: UnitDef,
    colors: CalcColors,
    onSelect: (String) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Box {
        SelectorRow(caption, unitLabel(stringResource(selected.name), selected.symbol), colors) { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            category.units.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(unitLabel(stringResource(unit.name), unit.symbol)) },
                    onClick = {
                        onSelect(unit.id)
                        open = false
                    }
                )
            }
        }
    }
}

@Composable
private fun BaseSelector(caption: String, selected: Int, colors: CalcColors, onSelect: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        SelectorRow(caption, stringResource(BASE_NAMES.getValue(selected)) + " ($selected)", colors) { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            UnitConverter.bases.forEach { base ->
                DropdownMenuItem(
                    text = { Text(stringResource(BASE_NAMES.getValue(base)) + " ($base)") },
                    onClick = {
                        onSelect(base)
                        open = false
                    }
                )
            }
        }
    }
}
