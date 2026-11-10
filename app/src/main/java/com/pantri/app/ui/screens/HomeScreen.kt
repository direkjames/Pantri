package com.pantri.app.ui.screens

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pantri.app.data.CutoffSummary
import com.pantri.app.data.ListSummary
import com.pantri.app.domain.pesos
import com.pantri.app.ui.components.BudgetDialog
import com.pantri.app.ui.components.Choice
import com.pantri.app.ui.components.EmptyState
import com.pantri.app.ui.components.GlassPill
import com.pantri.app.ui.components.GradientButton
import com.pantri.app.ui.components.HeroCard
import com.pantri.app.ui.components.IconTile
import com.pantri.app.ui.components.PantriCard
import com.pantri.app.ui.components.PantriDialog
import com.pantri.app.ui.components.PantriField
import com.pantri.app.ui.components.Pill
import com.pantri.app.ui.components.ProgressRing
import com.pantri.app.ui.components.SectionLabel
import com.pantri.app.ui.components.SoftButton
import com.pantri.app.ui.HomeVm
import com.pantri.app.ui.pantriVm
import com.pantri.app.ui.theme.Tone
import com.pantri.app.ui.theme.pantri
import java.time.LocalTime

private val tones = listOf(Tone.Green, Tone.Yellow, Tone.Orange, Tone.Red)

@Composable
fun HomeScreen(onOpenList: (Long) -> Unit, onNavigate: (String) -> Unit, onSettings: () -> Unit) {
    val vm = pantriVm { HomeVm(it) }
    val lists by vm.lists.collectAsStateWithLifecycle()
    val templates by vm.templates.collectAsStateWithLifecycle()
    val summary by vm.summary.collectAsStateWithLifecycle()
    val streak by vm.streak.collectAsStateWithLifecycle()
    var newList by rememberSaveable { mutableStateOf(false) }
    var editBudget by rememberSaveable { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(greeting(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.pantri.sub)
                    Text("Pantri", style = MaterialTheme.typography.displayMedium)
                }
                if (streak > 0) {
                    Pill("$streak on budget", Tone.Yellow, icon = Icons.Rounded.LocalFireDepartment)
                    Spacer(Modifier.size(4.dp))
                }
                IconButton(onSettings) { Icon(Icons.Rounded.Settings, "Settings", tint = MaterialTheme.pantri.sub) }
            }
        }
        item { summary?.let { BudgetHero(it) { editBudget = true } } ?: Spacer(Modifier.height(170.dp)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickTile("New list", Icons.Rounded.Add, Tone.Green, Modifier.weight(1f)) { newList = true }
                QuickTile("Plan meals", Icons.Rounded.CalendarMonth, Tone.Orange, Modifier.weight(1f)) { onNavigate("plan") }
                QuickTile("Recipes", Icons.AutoMirrored.Rounded.MenuBook, Tone.Red, Modifier.weight(1f)) { onNavigate("recipes") }
            }
        }
        item { SectionLabel("Your lists", Modifier.padding(top = 4.dp)) }
        if (lists.isEmpty()) {
            item {
                EmptyState(Icons.Rounded.ShoppingCart, "Start your first list", "Add what you need, set a price, and watch your cutoff budget.") {
                    GradientButton("Create list", { newList = true }, icon = Icons.Rounded.Add)
                }
            }
        }
        items(lists, key = { it.list.id }) { s -> ListRow(s, tones[(s.list.id % tones.size).toInt()]) { onOpenList(s.list.id) } }
        if (templates.isNotEmpty()) {
            item { SectionLabel("Templates", Modifier.padding(top = 4.dp)) }
            items(templates, key = { "t${it.list.id}" }) { t ->
                PantriCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(t.list.name, style = MaterialTheme.typography.titleMedium)
                            Text("${t.itemCount} items", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.pantri.sub)
                        }
                        SoftButton("Use", { vm.createList(t.list.name, t.list.id, onOpenList) })
                        IconButton({ vm.deleteTemplate(t.list.id) }) { Icon(Icons.Rounded.Close, "Delete template", tint = MaterialTheme.pantri.muted) }
                    }
                }
            }
        }
    }

    if (newList) NewListDialog(templates, { newList = false }) { name, tid ->
        newList = false
        vm.createList(name, tid, onOpenList)
    }
    if (editBudget) summary?.let {
        BudgetDialog("Budget for ${it.cutoff.label()}", "Changes only this cutoff. Change the usual amount in Settings.", it.budget, { v -> editBudget = false; vm.setCutoffBudget(v) }, { editBudget = false })
    }
}

@Composable
fun BudgetHero(s: CutoffSummary, onClick: () -> Unit) {
    val over = s.left < 0
    HeroCard(if (over) MaterialTheme.pantri.heroSunset else MaterialTheme.pantri.heroGreen, Modifier.fillMaxWidth().clickable(onClickLabel = "Edit budget", role = Role.Button, onClick = onClick)) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProgressRing(s.fraction, Modifier.size(96.dp), color = if (over) Color.White else Color(0xFFF6C62F)) {
                    Text("${(s.fraction * 100).toInt()}%", style = MaterialTheme.typography.titleLarge, color = Color.White)
                }
                Spacer(Modifier.size(16.dp))
                Column {
                    Text(if (over) "Over this cutoff" else "Left this cutoff", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
                    Text(pesos(if (over) -s.left else s.left), style = MaterialTheme.typography.displayMedium, color = Color.White)
                    val days = s.cutoff.daysLeft(com.pantri.app.data.PantriRepository.today())
                    Text("${s.cutoff.label()} · ${if (days == 1) "last day" else "$days days left"}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
                }
            }
            Spacer(Modifier.height(16.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassPill("Spent ${pesos(s.spent)}")
                GlassPill("Planned ${pesos(s.planned)}")
                GlassPill("Budget ${pesos(s.budget)}")
            }
            if (s.planned > s.left && s.left >= 0) {
                Spacer(Modifier.height(10.dp))
                Text("Your open lists are ${pesos(s.planned - s.left)} over what's left.", style = MaterialTheme.typography.bodySmall, color = Color.White)
            }
        }
    }
}

@Composable
private fun QuickTile(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tone: Tone, modifier: Modifier, onClick: () -> Unit) {
    PantriCard(modifier.clickable(role = Role.Button, onClick = onClick), padding = 12.dp) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            IconTile(icon, tone)
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

@Composable
private fun ListRow(s: ListSummary, tone: Tone, onClick: () -> Unit) {
    PantriCard(Modifier.fillMaxWidth().clickable(onClickLabel = "Edit budget", role = Role.Button, onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(Icons.Rounded.ShoppingCart, tone, size = 44.dp)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(s.list.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(
                    if (s.itemCount == 0) "Empty" else "${s.itemCount} items · ${s.doneCount} done",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.pantri.sub,
                )
            }
            if (s.totalCentavos > 0) Pill(pesos(s.totalCentavos), tone)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NewListDialog(templates: List<ListSummary>, onDismiss: () -> Unit, onCreate: (String, Long?) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var template by rememberSaveable { mutableStateOf<Long?>(null) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    fun go() {
        val n = name.ifBlank { templates.firstOrNull { it.list.id == template }?.list?.name.orEmpty() }
        if (n.isBlank()) error = "Give your list a name" else onCreate(n, template)
    }
    PantriDialog("New list", onDismiss, "Create", ::go) {
        PantriField(name, { name = it; error = null }, "Name", error = error, ime = androidx.compose.ui.text.input.ImeAction.Done, onDone = ::go)
        if (templates.isNotEmpty()) {
            SectionLabel("Start from")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Choice("Blank", template == null) { template = null }
                templates.forEach { Choice(it.list.name, template == it.list.id) { template = it.list.id } }
            }
        }
    }
}

private fun greeting(): String {
    val h = LocalTime.now().hour
    return when {
        h < 12 -> "Good morning"
        h < 18 -> "Good afternoon"
        else -> "Good evening"
    }
}
