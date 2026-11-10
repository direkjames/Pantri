package com.pantri.app.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pantri.app.data.Aisle
import com.pantri.app.data.Recipe
import com.pantri.app.data.RecipeIngredient
import com.pantri.app.domain.Incoming
import com.pantri.app.domain.parseQty
import com.pantri.app.domain.qty
import com.pantri.app.ui.LocalSnack
import com.pantri.app.ui.RecipesVm
import com.pantri.app.ui.components.EmptyState
import com.pantri.app.ui.components.GlassPill
import com.pantri.app.ui.components.GradientButton
import com.pantri.app.ui.components.HeroCard
import com.pantri.app.ui.components.IconTile
import com.pantri.app.ui.components.ListPickerDialog
import com.pantri.app.ui.components.PantriCard
import com.pantri.app.ui.components.PantriCheck
import com.pantri.app.ui.components.PantriDialog
import com.pantri.app.ui.components.PantriField
import com.pantri.app.ui.components.SectionLabel
import com.pantri.app.ui.components.SoftButton
import com.pantri.app.ui.components.TopBar
import com.pantri.app.ui.pantriVm
import com.pantri.app.ui.theme.Tone
import com.pantri.app.ui.theme.pantri
import kotlinx.coroutines.launch

private val recipeTones = listOf(Tone.Orange, Tone.Green, Tone.Red, Tone.Yellow)

@Composable
fun RecipesScreen(onOpen: (Long) -> Unit, onNew: () -> Unit) {
    val vm = pantriVm { RecipesVm(it) }
    val recipes by vm.recipes.collectAsStateWithLifecycle()
    val counts by vm.counts.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 190.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Column(Modifier.statusBarsPadding().padding(top = 8.dp, bottom = 4.dp)) {
                    Text("Recipe book", style = MaterialTheme.typography.displayMedium)
                    Text("Type in the ones you love, then send the ingredients to a list.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.pantri.sub)
                }
            }
            if (recipes.isEmpty()) {
                item {
                    EmptyState(Icons.AutoMirrored.Rounded.MenuBook, "Your recipe book is empty", "Add a recipe from the internet or your family. Each one can fill a grocery list in one tap.") {
                        GradientButton("Add a recipe", onNew, icon = Icons.Rounded.Add)
                    }
                }
            }
            items(recipes, key = { it.id }) { r ->
                PantriCard(Modifier.fillMaxWidth().clickable { onOpen(r.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconTile(Icons.Rounded.Restaurant, recipeTones[(r.id % recipeTones.size).toInt()], size = 48.dp)
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(r.title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                            val meta = listOfNotNull(
                                "${counts[r.id] ?: 0} ingredients",
                                r.minutes.takeIf { it > 0 }?.let { "$it min" },
                                "serves ${r.servings}",
                            ).joinToString(" · ")
                            Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.pantri.sub)
                        }
                    }
                }
            }
        }
        if (recipes.isNotEmpty()) {
            GradientButton("New recipe", onNew, Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 112.dp), icon = Icons.Rounded.Add)
        }
    }
}

@Composable
fun RecipeDetailScreen(id: Long, onBack: () -> Unit, onEdit: () -> Unit, onOpenList: (Long) -> Unit) {
    val vm = pantriVm { RecipesVm(it) }
    val recipe by vm.recipe(id).collectAsStateWithLifecycle(null)
    val ingredients by vm.ingredients(id).collectAsStateWithLifecycle(emptyList())
    val lists by vm.lists.collectAsStateWithLifecycle()
    val snack = LocalSnack.current
    val scope = rememberCoroutineScope()
    val p = MaterialTheme.pantri
    var skipped by rememberSaveable { mutableStateOf(setOf<Long>()) }
    var pick by rememberSaveable { mutableStateOf(false) }
    var menu by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val r = recipe ?: return
    val chosen = ingredients.filter { it.id !in skipped }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.statusBarsPadding().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
                Spacer(Modifier.weight(1f))
                Box {
                    IconButton({ menu = true }) { Icon(Icons.Rounded.MoreVert, "Recipe options", tint = p.sub) }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem({ Text("Edit") }, { menu = false; onEdit() })
                        DropdownMenuItem({ Text("Delete", color = MaterialTheme.colorScheme.error) }, { menu = false; confirmDelete = true })
                    }
                }
            }
            HeroCard(p.heroSunset, Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Icon(Icons.Rounded.Restaurant, null, Modifier.size(32.dp), tint = Color.White.copy(alpha = 0.9f))
                    Spacer(Modifier.height(18.dp))
                    Text(r.title, style = MaterialTheme.typography.headlineLarge, color = Color.White)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassPill("Serves ${r.servings}")
                        if (r.minutes > 0) GlassPill("${r.minutes} min")
                        GlassPill("${ingredients.size} ingredients")
                    }
                }
            }
            SectionLabel("Ingredients")
            PantriCard(Modifier.fillMaxWidth(), padding = 8.dp) {
                if (ingredients.isEmpty()) Text("No ingredients yet. Tap Edit to add some.", Modifier.padding(8.dp), color = p.sub)
                ingredients.forEach { ing ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        PantriCheck(ing.id !in skipped, { on -> skipped = if (on) skipped - ing.id else skipped + ing.id }, label = ing.name)
                        Spacer(Modifier.size(12.dp))
                        Text(ing.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, maxLines = 2)
                        Text(qty(ing.quantity) + if (ing.unit.isBlank()) "" else " ${ing.unit}", style = MaterialTheme.typography.bodyMedium, color = p.sub)
                    }
                }
            }
            if (r.steps.isNotBlank()) {
                SectionLabel("Steps")
                PantriCard(Modifier.fillMaxWidth()) { Text(r.steps, style = MaterialTheme.typography.bodyLarge) }
            }
        }
        GradientButton(
            if (chosen.isEmpty()) "Pick ingredients to add" else "Add ${chosen.size} to grocery list",
            { if (chosen.isNotEmpty()) pick = true },
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(20.dp).fillMaxWidth(),
            icon = Icons.Rounded.ShoppingCart,
        )
    }

    if (pick) ListPickerDialog("Add to which list?", lists, r.title, "Add", { listId, name ->
        pick = false
        vm.addToList(listId, name, chosen.map { Incoming(it.name, it.quantity, it.unit, it.category) }) { target, n ->
            scope.launch {
                if (snack.showSnackbar("Added $n ${if (n == 1) "item" else "items"} to your list", "Open") == SnackbarResult.ActionPerformed) onOpenList(target)
            }
        }
    }, { pick = false })
    if (confirmDelete) PantriDialog("Delete this recipe?", { confirmDelete = false }, "Delete", { confirmDelete = false; vm.delete(id, onBack) }) {
        Text("Meals in your plan that use it keep their name.", style = MaterialTheme.typography.bodyMedium, color = p.sub)
    }
}

private class IngRow(var name: String, var qty: String, var unit: String, val key: Long)

/** Keeps the half-typed ingredient rows through rotation and the app being killed. */
private val IngRowsSaver = listSaver<SnapshotStateList<IngRow>, List<String>>(
    save = { l -> l.map { listOf(it.key.toString(), it.name, it.qty, it.unit) } },
    restore = { saved -> mutableStateListOf<IngRow>().apply { saved.forEach { add(IngRow(it[1], it[2], it[3], it[0].toLong())) } } },
)

@Composable
fun RecipeEditorScreen(id: Long, onBack: () -> Unit, onSaved: (Long) -> Unit) {
    val vm = pantriVm { RecipesVm(it) }
    val p = MaterialTheme.pantri
    val loaded by vm.recipe(id).collectAsStateWithLifecycle(null)
    val loadedIngredients by vm.ingredients(id).collectAsStateWithLifecycle(null)
    var title by rememberSaveable { mutableStateOf("") }
    var servings by rememberSaveable { mutableStateOf("4") }
    var minutes by rememberSaveable { mutableStateOf("") }
    var steps by rememberSaveable { mutableStateOf("") }
    val rows = rememberSaveable(saver = IngRowsSaver) { mutableStateListOf<IngRow>().apply { if (id == 0L) add(IngRow("", "1", "", 0)) } }
    var counter by rememberSaveable { mutableLongStateOf(1L) }
    var filled by rememberSaveable { mutableStateOf(id == 0L) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var titleError by rememberSaveable { mutableStateOf<String?>(null) }

    // Existing recipes: copy the saved values into the form once.
    LaunchedEffect(loaded, loadedIngredients) {
        val r = loaded
        val ings = loadedIngredients
        if (!filled && r != null && ings != null) {
            title = r.title; servings = r.servings.toString(); minutes = if (r.minutes > 0) r.minutes.toString() else ""; steps = r.steps
            rows.clear(); ings.forEach { rows.add(IngRow(it.name, qty(it.quantity), it.unit, counter++)) }
            filled = true
        }
    }
    fun save() {
        titleError = if (title.isBlank()) "Give the recipe a name" else null
        val parsed = mutableListOf<RecipeIngredient>()
        var bad = false
        rows.filter { it.name.isNotBlank() }.forEach {
            val q = if (it.qty.isBlank()) 1.0 else parseQty(it.qty)
            if (q == null) bad = true else parsed += RecipeIngredient(recipeId = 0, name = it.name, quantity = q, unit = it.unit.trim(), category = Aisle.guess(it.name).name)
        }
        error = if (bad) "Check the quantities: use numbers above zero." else null
        if (titleError != null || bad) return
        val base = loaded ?: Recipe(title = "", createdAt = 0)
        vm.save(base.copy(title = title.trim(), servings = servings.toIntOrNull()?.coerceIn(1, 99) ?: 4, minutes = minutes.toIntOrNull()?.coerceIn(0, 999) ?: 0, steps = steps.trim()), parsed, onSaved)
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        TopBar(if (id == 0L) "New recipe" else "Edit recipe", onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PantriField(title, { title = it; titleError = null }, "Recipe name", Modifier.fillMaxWidth(), error = titleError)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PantriField(servings, { servings = it }, "Servings", Modifier.weight(1f), KeyboardType.Number)
                PantriField(minutes, { minutes = it }, "Minutes", Modifier.weight(1f), KeyboardType.Number)
            }
            SectionLabel("Ingredients", Modifier.padding(top = 6.dp))
            rows.forEachIndexed { i, row ->
                androidx.compose.runtime.key(row.key) {
                    var n by remember { mutableStateOf(row.name) }
                    var q by remember { mutableStateOf(row.qty) }
                    var u by remember { mutableStateOf(row.unit) }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Top) {
                        PantriField(n, { n = it; row.name = it }, "Ingredient", Modifier.weight(1f))
                        PantriField(q, { q = it; row.qty = it }, "Qty", Modifier.width(92.dp), KeyboardType.Decimal)
                        PantriField(u, { u = it; row.unit = it }, "Unit", Modifier.width(92.dp), maxLength = com.pantri.app.domain.Limits.UNIT)
                        IconButton({ rows.removeAt(i) }, Modifier.padding(top = 4.dp)) { Icon(Icons.Rounded.Close, "Remove ingredient", tint = p.muted) }
                    }
                }
            }
            SoftButton("Add ingredient", { rows.add(IngRow("", "1", "", counter++)) }, icon = Icons.Rounded.Add)
            PantriField(steps, { steps = it }, "Steps (optional)", Modifier.fillMaxWidth().padding(top = 6.dp), singleLine = false, minLines = 4, ime = ImeAction.Default, maxLength = com.pantri.app.domain.Limits.STEPS)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Spacer(Modifier.height(8.dp))
        }
        GradientButton("Save recipe", ::save, Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp))
    }
}
