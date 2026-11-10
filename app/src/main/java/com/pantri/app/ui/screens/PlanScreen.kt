package com.pantri.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pantri.app.data.PantriRepository
import com.pantri.app.data.Recipe
import com.pantri.app.ui.LocalSnack
import com.pantri.app.ui.PlanVm
import com.pantri.app.ui.components.EmptyState
import com.pantri.app.ui.components.GradientButton
import com.pantri.app.ui.components.IconTile
import com.pantri.app.ui.components.ListPickerDialog
import com.pantri.app.ui.components.PantriCard
import com.pantri.app.ui.components.PantriField
import com.pantri.app.ui.components.PantriSheet
import com.pantri.app.ui.components.SectionLabel
import com.pantri.app.ui.components.SoftButton
import com.pantri.app.ui.pantriVm
import com.pantri.app.ui.theme.Tone
import com.pantri.app.ui.theme.pantri
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val slotNames = listOf("Breakfast", "Lunch", "Dinner", "Snack")
private val slotIcons = listOf(Icons.Rounded.WbSunny, Icons.Rounded.Restaurant, Icons.Rounded.NightsStay, Icons.Rounded.LocalCafe)
private val slotTones = listOf(Tone.Yellow, Tone.Green, Tone.Red, Tone.Orange)

@Composable
fun PlanScreen(onNewRecipe: () -> Unit, onOpenList: (Long) -> Unit) {
    val vm = pantriVm { PlanVm(it) }
    val weekStart by vm.weekStart.collectAsStateWithLifecycle()
    val meals by vm.meals.collectAsStateWithLifecycle()
    val recipes by vm.recipes.collectAsStateWithLifecycle()
    val lists by vm.lists.collectAsStateWithLifecycle()
    val counts by vm.counts.collectAsStateWithLifecycle()
    val snack = LocalSnack.current
    val scope = rememberCoroutineScope()
    val p = MaterialTheme.pantri
    val today = PantriRepository.today()
    var selected by rememberSaveable { mutableStateOf(today) }
    var addSlot by remember { mutableIntStateOf(-1) }
    var pickList by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(weekStart) { if (selected < weekStart || selected > weekStart.plusDays(6)) selected = if (today in weekStart..weekStart.plusDays(6)) today else weekStart }

    val plannedRecipes = meals.count { it.recipeId != null }
    val ingredientCount = meals.mapNotNull { it.recipeId }.sumOf { counts[it] ?: 0 }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(Modifier.statusBarsPadding().padding(top = 8.dp)) {
                Text("Meal plan", style = MaterialTheme.typography.displayMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton({ vm.shiftWeek(-1) }) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Previous week") }
                    Text(weekLabel(weekStart), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    IconButton({ vm.shiftWeek(1) }) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Next week") }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (0..6).forEach { i ->
                    val d = weekStart.plusDays(i.toLong())
                    val sel = d == selected
                    val has = meals.any { it.epochDay == d.toEpochDay() }
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                            .background(if (sel) p.green else p.card).selectable(selected = sel, role = Role.Tab) { selected = d }.padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.ENGLISH), style = MaterialTheme.typography.labelSmall, color = if (sel) Color.White.copy(alpha = 0.85f) else p.sub)
                        Text("${d.dayOfMonth}", style = MaterialTheme.typography.titleMedium, color = if (sel) Color.White else if (d == today) p.green else p.ink)
                        Box(Modifier.padding(top = 4.dp).size(5.dp).clip(CircleShape).background(if (has) (if (sel) Color.White else p.orange) else Color.Transparent))
                    }
                }
            }
        }
        if (recipes.isEmpty() && meals.isEmpty()) {
            item {
                EmptyState(Icons.Rounded.Restaurant, "Plan your week", "Add recipes to your recipe book first, then drop them onto days here.") {
                    GradientButton("Add a recipe", onNewRecipe, icon = Icons.Rounded.Add)
                }
            }
        }
        slotNames.forEachIndexed { slot, name ->
            val here = meals.filter { it.epochDay == selected.toEpochDay() && it.slot == slot }
            item(key = "slot$slot") {
                PantriCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconTile(slotIcons[slot], slotTones[slot])
                        Spacer(Modifier.size(12.dp))
                        Text(name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        IconButton({ addSlot = slot }) { Icon(Icons.Rounded.Add, "Add $name", tint = p.green) }
                    }
                    here.forEach { m ->
                        Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(m.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                            IconButton({ vm.deleteMeal(m.id) }, Modifier.size(32.dp)) { Icon(Icons.Rounded.Close, "Remove ${m.title}", Modifier.size(18.dp), tint = p.muted) }
                        }
                    }
                    if (here.isEmpty()) Text("Nothing planned", Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall, color = p.muted)
                }
            }
        }
        item {
            PantriCard(Modifier.fillMaxWidth()) {
                SectionLabel("This week")
                Text(
                    if (plannedRecipes == 0) "No recipes planned yet." else "$plannedRecipes ${if (plannedRecipes == 1) "recipe" else "recipes"} · $ingredientCount ${if (ingredientCount == 1) "ingredient" else "ingredients"}",
                    style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 6.dp),
                )
                GradientButton(
                    "Add week's ingredients to a list",
                    { if (plannedRecipes > 0) pickList = true else scope.launch { snack.showSnackbar("Plan a recipe first, then add its ingredients.") } },
                    Modifier.fillMaxWidth(), icon = Icons.Rounded.ShoppingCart,
                )
            }
        }
    }

    if (addSlot >= 0) MealSheet(selected, slotNames[addSlot], recipes, { addSlot = -1 }) { recipeId, title ->
        vm.addMeal(selected, addSlot, recipeId, title); addSlot = -1
    }
    if (pickList) ListPickerDialog("Add ingredients to", lists, "Week of ${weekLabel(weekStart)}", "Add", { id, name ->
        pickList = false
        vm.addWeekToList(id, name) { target, n ->
            scope.launch {
                if (snack.showSnackbar("Added $n ${if (n == 1) "item" else "items"} to your list", "Open") == SnackbarResult.ActionPerformed) onOpenList(target)
            }
        }
    }, { pickList = false })
}

@Composable
private fun MealSheet(day: LocalDate, slot: String, recipes: List<Recipe>, onDismiss: () -> Unit, onAdd: (Long?, String) -> Unit) {
    val p = MaterialTheme.pantri
    var custom by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    fun addCustom() { if (custom.isBlank()) error = "Type what you're having" else onAdd(null, custom) }
    PantriSheet(onDismiss) {
        Text("$slot on ${day.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)}", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        if (recipes.isNotEmpty()) {
            SectionLabel("From your recipe book")
            Spacer(Modifier.height(6.dp))
            recipes.forEach { r ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(16.dp)).background(p.card).clickable { onAdd(r.id, r.title) }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconTile(Icons.Rounded.Restaurant, Tone.Orange, size = 36.dp)
                    Spacer(Modifier.size(12.dp))
                    Text(r.title, style = MaterialTheme.typography.titleSmall)
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        SectionLabel("Or type something")
        Spacer(Modifier.height(6.dp))
        PantriField(custom, { custom = it; error = null }, "Meal", Modifier.fillMaxWidth(), error = error, ime = ImeAction.Done, onDone = ::addCustom)
        Spacer(Modifier.height(8.dp))
        SoftButton("Add", ::addCustom, Modifier.fillMaxWidth())
    }
}

private fun weekLabel(start: LocalDate): String {
    val end = start.plusDays(6)
    val m1 = start.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
    val m2 = end.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
    return if (start.month == end.month) "$m1 ${start.dayOfMonth} to ${end.dayOfMonth}" else "$m1 ${start.dayOfMonth} to $m2 ${end.dayOfMonth}"
}
