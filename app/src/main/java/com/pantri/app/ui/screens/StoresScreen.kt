package com.pantri.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Storefront
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pantri.app.data.Store
import com.pantri.app.data.StorePrice
import com.pantri.app.domain.Offer
import com.pantri.app.domain.parsePesos
import com.pantri.app.domain.parseQty
import com.pantri.app.domain.perLabel
import com.pantri.app.domain.pesos
import com.pantri.app.domain.qty
import com.pantri.app.domain.rankOffers
import com.pantri.app.ui.StoresVm
import com.pantri.app.ui.components.Choice
import com.pantri.app.ui.components.EmptyState
import com.pantri.app.ui.components.GradientButton
import com.pantri.app.ui.components.IconTile
import com.pantri.app.ui.components.PantriCard
import com.pantri.app.ui.components.PantriDialog
import com.pantri.app.ui.components.PantriField
import com.pantri.app.ui.components.PantriSheet
import com.pantri.app.ui.components.Pill
import com.pantri.app.ui.components.SectionLabel
import com.pantri.app.ui.components.SoftButton
import com.pantri.app.ui.components.TopBar
import com.pantri.app.ui.pantriVm
import com.pantri.app.ui.theme.Tone
import com.pantri.app.ui.theme.pantri

private val unitChoices = listOf("pc", "kg", "g", "L", "ml", "pack", "bunch", "can", "bottle", "dozen")
private val storeTones = listOf(Tone.Green, Tone.Orange, Tone.Red, Tone.Yellow)

/** Your stores, and every item you have a store price for, cheapest store first. */
@Composable
fun StoresScreen(onBack: () -> Unit) {
    val vm = pantriVm { StoresVm(it) }
    val stores by vm.stores.collectAsStateWithLifecycle()
    val prices by vm.prices.collectAsStateWithLifecycle()
    val products by vm.products.collectAsStateWithLifecycle()
    val p = MaterialTheme.pantri
    var newName by rememberSaveable { mutableStateOf("") }
    var renameId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var compareKey by rememberSaveable { mutableStateOf<String?>(null) }
    var picking by rememberSaveable { mutableStateOf(false) }
    var addError by rememberSaveable { mutableStateOf<String?>(null) }

    val nameOf = remember(stores) { stores.associate { it.id to it.name } }
    val productName = remember(products) { products.associate { it.key to it.name } }
    val compared = remember(prices, nameOf, productName) {
        prices.groupBy { it.productKey }
            .map { (k, ps) -> Triple(k, productName[k] ?: k.replaceFirstChar { c -> c.uppercase() }, rankOffers(ps) { id -> nameOf[id] ?: "?" }) }
            .sortedBy { it.second.lowercase() }
    }

    fun addStore() {
        if (newName.isBlank()) { addError = "Type the store's name"; return }
        vm.addStore(newName)
        newName = ""
        addError = null
    }

    Column(Modifier.fillMaxSize()) {
        TopBar("Stores & prices", onBack = onBack)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { SectionLabel("Your stores") }
            if (stores.isEmpty()) {
                item {
                    EmptyState(Icons.Rounded.Storefront, "Add your first store", "Add the stores you shop at, then note what each one charges. Pantri shows which is cheapest.")
                }
            }
            items(stores, key = { "s${it.id}" }) { s ->
                val count = prices.count { it.storeId == s.id }
                PantriCard(Modifier.fillMaxWidth(), padding = 10.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconTile(Icons.Rounded.Storefront, storeTones[(s.id % storeTones.size).toInt()])
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.name, style = MaterialTheme.typography.titleMedium)
                            Text(if (count == 1) "1 price" else "$count prices", style = MaterialTheme.typography.bodySmall, color = p.sub)
                        }
                        IconButton({ renameId = s.id }) { Icon(Icons.Rounded.Edit, "Rename ${s.name}", tint = p.sub) }
                        IconButton({ deleteId = s.id }) { Icon(Icons.Rounded.DeleteOutline, "Delete ${s.name}", tint = p.sub) }
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PantriField(newName, { newName = it; addError = null }, "New store, like OSave", Modifier.weight(1f), error = addError, ime = ImeAction.Done, onDone = ::addStore)
                    GradientButton("Add", ::addStore, Modifier.padding(top = 4.dp), icon = Icons.Rounded.Add)
                }
            }

            item { SectionLabel("Compare prices", Modifier.padding(top = 8.dp)) }
            item {
                if (stores.isEmpty()) Text("Add a store first, then you can add prices.", style = MaterialTheme.typography.bodySmall, color = p.muted)
                else GradientButton("Add a price", { picking = true }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Add)
            }
            items(compared, key = { "c${it.first}" }) { (key, name, offers) ->
                PantriCard(Modifier.fillMaxWidth().clickable(onClickLabel = "Compare $name", role = Role.Button) { compareKey = key }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(name, style = MaterialTheme.typography.titleMedium)
                            val best = offers.firstOrNull { it.best }
                            Text(
                                if (best != null) "Cheapest at ${best.storeName}: ${perUnitText(best)}" else "${offers.size} stores, different units",
                                style = MaterialTheme.typography.bodySmall, color = p.sub,
                            )
                        }
                        Pill(if (offers.size == 1) "1 store" else "${offers.size} stores", Tone.Green)
                    }
                }
            }
        }
    }

    if (picking) PickProductDialog(productNames = products.map { it.name }, onDismiss = { picking = false }) { name ->
        picking = false
        compareKey = name.trim().lowercase()
    }
    compareKey?.let { k ->
        CompareSheet(vm, k, productName[k] ?: k.replaceFirstChar { it.uppercase() }, 1.0, "") { compareKey = null }
    }
    stores.firstOrNull { it.id == renameId }?.let { s ->
        NameDialog("Rename store", s.name, "Save", { renameId = null }) { vm.renameStore(s, it); renameId = null }
    }
    stores.firstOrNull { it.id == deleteId }?.let { s ->
        PantriDialog("Delete ${s.name}?", { deleteId = null }, "Delete", { vm.deleteStore(s.id); deleteId = null }) {
            Text("Its prices are removed too. Your lists and items stay.", style = MaterialTheme.typography.bodyMedium, color = p.sub)
        }
    }
}

private fun perUnitText(o: Offer) = o.perUnit?.let { "${pesos(Math.round(it))} ${perLabel(o.group)}" } ?: pesos(o.price.priceCentavos)

private fun agoText(at: Long): String {
    val d = ((System.currentTimeMillis() - at) / 86_400_000L).toInt().coerceAtLeast(0)
    return when (d) { 0 -> "today"; 1 -> "yesterday"; else -> "$d days ago" }
}

/** Every store's price for one item, ranked, with a way to add or change each one. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompareSheet(vm: StoresVm, productKey: String, productName: String, defaultQty: Double, defaultUnit: String, onDismiss: () -> Unit) {
    val stores by vm.stores.collectAsStateWithLifecycle()
    val all by vm.prices.collectAsStateWithLifecycle()
    val p = MaterialTheme.pantri
    var editId by rememberSaveable { mutableStateOf<Long?>(null) }
    var newStore by rememberSaveable { mutableStateOf("") }

    val mine = all.filter { it.productKey == productKey }
    val offers = rankOffers(mine) { id -> stores.firstOrNull { it.id == id }?.name ?: "?" }
    val missing = stores.filter { s -> mine.none { it.storeId == s.id } }
    val comparable = offers.filter { it.perUnit != null }

    PantriSheet(onDismiss) {
        Text(productName, style = MaterialTheme.typography.headlineSmall)
        Text("Compare what each store charges.", style = MaterialTheme.typography.bodySmall, color = p.sub)
        if (comparable.size >= 2) {
            val best = comparable.first()
            val worst = comparable.last()
            Spacer(Modifier.height(10.dp))
            PantriCard(Modifier.fillMaxWidth()) {
                Text("${best.storeName} is cheapest", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Save ${pesos(Math.round(worst.extraPerUnit))} ${perLabel(best.group)} compared with ${worst.storeName}.",
                    style = MaterialTheme.typography.bodySmall, color = p.sub,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        offers.forEach { o ->
            PantriCard(Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable(onClickLabel = "Change price at ${o.storeName}", role = Role.Button) { editId = o.price.storeId }, padding = 12.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(o.storeName, style = MaterialTheme.typography.titleSmall)
                        Text(packText(o.price), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            (if (o.perUnit != null) "${perUnitText(o)} · " else "Different unit, can't compare · ") + agoText(o.price.at),
                            style = MaterialTheme.typography.bodySmall, color = p.sub,
                        )
                    }
                    when {
                        o.best && comparable.size >= 2 -> Pill("Cheapest", Tone.Green)
                        o.perUnit != null && comparable.size >= 2 -> Pill("+${pesos(Math.round(o.extraPerUnit))}", Tone.Orange)
                    }
                    IconButton({ vm.clearPrice(productKey, o.price.storeId) }) { Icon(Icons.Rounded.Close, "Remove price at ${o.storeName}", tint = p.muted) }
                }
            }
        }
        if (offers.any { it.perUnit == null }) {
            Text("Tap a price and use the same kind of unit as the others to compare it. Kg and g work together, and so do L and ml.", style = MaterialTheme.typography.bodySmall, color = p.sub)
        }
        if (missing.isNotEmpty()) {
            SectionLabel(if (offers.isEmpty()) "Add a price at" else "No price yet at", Modifier.padding(top = 4.dp))
            FlowRow(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                missing.forEach { s -> Choice(s.name, false) { editId = s.id } }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PantriField(newStore, { newStore = it }, "New store", Modifier.weight(1f), ime = ImeAction.Done, onDone = { vm.addStore(newStore) { id -> if (id != null) editId = id }; newStore = "" })
            SoftButton("Add", { vm.addStore(newStore) { id -> if (id != null) editId = id }; newStore = "" }, icon = Icons.Rounded.Add)
        }
    }

    stores.firstOrNull { it.id == editId }?.let { s ->
        val old = mine.firstOrNull { it.storeId == s.id }
        PriceDialog(s.name, productName, old, defaultQty, defaultUnit, { editId = null }) { c, q, u ->
            vm.setPrice(productKey, s.id, c, q, u)
            editId = null
        }
    }
}

private fun packText(sp: StorePrice): String {
    val unit = sp.unit.trim()
    return if (unit.isEmpty() && sp.quantity == 1.0) pesos(sp.priceCentavos) else "${pesos(sp.priceCentavos)} for ${qty(sp.quantity)}${if (unit.isEmpty()) "" else " $unit"}"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PriceDialog(
    storeName: String, productName: String, old: StorePrice?, defaultQty: Double, defaultUnit: String,
    onDismiss: () -> Unit, onSave: (Long, Double, String) -> Unit,
) {
    val p = MaterialTheme.pantri
    var price by rememberSaveable { mutableStateOf(old?.let { (it.priceCentavos / 100.0).toString().removeSuffix(".0") } ?: "") }
    var quantity by rememberSaveable { mutableStateOf(qty(old?.quantity ?: defaultQty)) }
    var unit by rememberSaveable { mutableStateOf(old?.unit ?: defaultUnit) }
    var priceError by rememberSaveable { mutableStateOf<String?>(null) }
    var qtyError by rememberSaveable { mutableStateOf<String?>(null) }
    fun save() {
        val c = parsePesos(price)
        val q = parseQty(quantity)
        priceError = if (c == null || c <= 0) "Enter a price above zero" else null
        qtyError = if (q == null) "Enter an amount above zero" else null
        if (priceError == null && qtyError == null) onSave(c!!, q!!, unit)
    }
    PantriDialog("$productName at $storeName", onDismiss, "Save", ::save) {
        Text("What you pay for the amount below. The unit is optional, but it lets Pantri compare different pack sizes.", style = MaterialTheme.typography.bodySmall, color = p.sub)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PantriField(price, { price = it; priceError = null }, "Price", Modifier.weight(1f), KeyboardType.Decimal, priceError, prefix = "₱")
            PantriField(quantity, { quantity = it; qtyError = null }, "Amount", Modifier.weight(1f), KeyboardType.Decimal, qtyError, ime = ImeAction.Done, onDone = ::save)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            unitChoices.forEach { u -> Choice(u, unit == u) { unit = if (unit == u) "" else u } }
        }
    }
}

/** Type an item name; matching items you already have show up as one-tap choices. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PickProductDialog(productNames: List<String>, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    fun go() { if (text.isBlank()) error = "Type an item name" else onPick(text) }
    val matches = if (text.isBlank()) emptyList() else productNames.filter { it.contains(text.trim(), ignoreCase = true) && !it.equals(text.trim(), ignoreCase = true) }.take(6)
    PantriDialog("Which item?", onDismiss, "Next", ::go) {
        PantriField(text, { text = it; error = null }, "Item, like Rice", error = error, ime = ImeAction.Done, onDone = ::go)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            matches.forEach { m -> Choice(m, false) { onPick(m) } }
        }
    }
}

@Composable
private fun NameDialog(title: String, initial: String, confirm: String, onDismiss: () -> Unit, onDone: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf(initial) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    fun go() { if (text.isBlank()) error = "Type a name" else onDone(text) }
    PantriDialog(title, onDismiss, confirm, ::go) {
        PantriField(text, { text = it; error = null }, "Name", error = error, ime = ImeAction.Done, onDone = ::go)
    }
}
