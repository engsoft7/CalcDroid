package com.example.myapplication.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.AccentText
import com.example.myapplication.ui.theme.DisplayPrimary
import com.example.myapplication.ui.theme.DisplaySecondary
import com.example.myapplication.ui.theme.GlassStroke

data class HistoryEntry(val expression: String, val result: String)

@Composable
fun HistoryPanel(
    entries: List<HistoryEntry>,
    visible: Boolean,
    onSelect: (HistoryEntry) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, GlassStroke, RoundedCornerShape(24.dp))
                .padding(12.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Histórico",
                    style = MaterialTheme.typography.labelSmall,
                    color = DisplaySecondary,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                if (entries.isNotEmpty()) {
                    Text(
                        text = "Limpar",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentText,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .clickable(onClick = onClear)
                    )
                }
            }

            if (entries.isEmpty()) {
                Text(
                    text = "Seus cálculos aparecerão aqui",
                    style = MaterialTheme.typography.bodyLarge,
                    color = DisplaySecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 220.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    contentPadding = PaddingValues(top = 8.dp)
                ) {
                    items(entries.asReversed()) { entry ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelect(entry) }
                                .padding(horizontal = 8.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = entry.expression,
                                style = MaterialTheme.typography.bodyLarge,
                                color = DisplaySecondary,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "= ${entry.result}",
                                style = MaterialTheme.typography.titleLarge,
                                color = DisplayPrimary,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}
