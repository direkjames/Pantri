package com.pantri.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pantri.app.data.PantriRepository
import com.pantri.app.domain.pesos
import com.pantri.app.ui.BudgetVm
import com.pantri.app.ui.components.BudgetDialog
import com.pantri.app.ui.components.EmptyState
import com.pantri.app.ui.components.IconTile
import com.pantri.app.ui.components.LinearBar
import com.pantri.app.ui.components.PantriCard
import com.pantri.app.ui.components.Pill
import com.pantri.app.ui.components.SectionLabel
import com.pantri.app.ui.components.icon
import com.pantri.app.ui.components.tone
import com.pantri.app.ui.pantriVm
import com.pantri.app.ui.theme.pantri
import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun BudgetScreen() {
    val vm = pantriVm { BudgetVm(it) }
    val summary by vm.summary.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val purchases by vm.purchases.collectAsStateWithLifecycle()
    val p = MaterialTheme.pantri
    var edit by rememberSaveable { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Column(Modifier.statusBarsPadding().padding(top = 8.dp)) {
                Text("Budget", style = MaterialTheme.typography.displayMedium)
                Text("Money counts as spent when you check an item off.", style = MaterialTheme.typography.bodyMedium, color = p.sub)
            }
        }
        item { summary?.let { BudgetHero(it) { edit = true } } ?: Spacer(Modifier.height(170.dp)) }
        summary?.let { s ->
            if (s.byAisle.isNotEmpty()) {
                item { SectionLabel("Where it went") }
                item {
                    PantriCard(Modifier.fillMaxWidth()) {
                        val max = s.byAisle.maxOf { it.second }.coerceAtLeast(1)
                        s.byAisle.forEachIndexed { i, (aisle, amount) ->
                            Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconTile(aisle.icon(), aisle.tone(), size = 34.dp)
                                Spacer(Modifier.size(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(aisle.label, style = MaterialTheme.typography.titleSmall)
                                    Spacer(Modifier.height(4.dp))
                                    LinearBar(amount.toFloat() / max, Modifier.fillMaxWidth())
                                }
                                Spacer(Modifier.size(12.dp))
                                Text(pesos(amount), style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    }
                }
            }
        }
        item { SectionLabel("Past cutoffs") }
        item {
            if (history.isEmpty()) {
                EmptyState(Icons.Rounded.Savings, "Your history starts here", "After your first full cutoff you'll see how you did against budget.")
            } else {
                PantriCard(Modifier.fillMaxWidth()) { HistoryChart(history.reversed()) }
            }
        }
        if (purchases.isNotEmpty()) {
            item { SectionLabel("Bought this cutoff") }
            item {
                PantriCard(Modifier.fillMaxWidth(), padding = 8.dp) {
                    purchases.take(20).forEach { it ->
                        val tone = com.pantri.app.data.Aisle.from(it.category).tone()
                        val day = Instant.ofEpochMilli(it.checkedAt ?: 0).atZone(ZoneId.systemDefault()).toLocalDate()
                        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(it.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                                Text("${day.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)} ${day.dayOfMonth}", style = MaterialTheme.typography.bodySmall, color = p.sub)
                            }
                            Pill(pesos(it.priceCentavos), tone)
                        }
                    }
                }
            }
        }
    }

    if (edit) summary?.let {
        BudgetDialog("Budget for ${it.cutoff.label()}", "Changes only this cutoff. Change the usual amount in Settings.", it.budget, { v -> edit = false; vm.setCutoffBudget(v) }, { edit = false })
    }
}

@Composable
private fun HistoryChart(rows: List<com.pantri.app.data.HistoryRow>) {
    val p = MaterialTheme.pantri
    val max = rows.maxOf { maxOf(it.budget, it.spent) }.coerceAtLeast(1)
    val labelColor = p.sub
    Column {
        Canvas(Modifier.fillMaxWidth().height(140.dp)) {
            val slot = size.width / rows.size
            val barW = slot * 0.5f
            rows.forEachIndexed { i, r ->
                val x = slot * i + (slot - barW) / 2
                val bh = size.height * (r.spent.toFloat() / max)
                drawRoundRect(
                    if (r.spent > r.budget) p.tomato else p.green, Offset(x, size.height - bh), Size(barW, bh.coerceAtLeast(4f)), CornerRadius(10f, 10f),
                )
                val by = size.height - size.height * (r.budget.toFloat() / max)
                drawLine(p.orange, Offset(x - 6f, by), Offset(x + barW + 6f, by), 4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            rows.forEach { r ->
                Text(
                    "${r.cutoff.start.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)} ${r.cutoff.start.dayOfMonth}",
                    Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = labelColor, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        Text("Bars are what you spent. The dashed line is your budget.", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall, color = p.muted)
    }
}
