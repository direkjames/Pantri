package com.pantri.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pantri.app.data.Aisle
import com.pantri.app.data.Item
import com.pantri.app.data.ItemDraft
import com.pantri.app.data.PriceEntry
import com.pantri.app.data.Product
import com.pantri.app.domain.parsePesos
import com.pantri.app.domain.parseQty
import com.pantri.app.domain.pesos
import com.pantri.app.domain.qty
import com.pantri.app.ui.components.Choice
import com.pantri.app.ui.components.GradientButton
import com.pantri.app.ui.components.PantriField
import com.pantri.app.ui.components.PantriSheet
import com.pantri.app.ui.components.SectionLabel
import com.pantri.app.ui.components.icon
import com.pantri.app.ui.theme.pantri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

private val units = listOf("pc", "kg", "g", "L", "ml", "pack", "bunch", "can", "bottle", "dozen")

/** Add an item with details, or edit one ([existing] is non-null). Money is typed in pesos for the whole line. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItemSheet(
    existing: Item?,
    prefillName: String,
    products: List<Product>,
    history: Flow<List<PriceEntry>>?,
    favorite: Boolean?,
    onFavorite: () -> Unit,
    onDismiss: () -> Unit,
    onSaveNew: (ItemDraft) -> Unit,
    onSaveEdit: (Item) -> Unit,
    onDelete: (Item) -> Unit,
) {
    val p = MaterialTheme.pantri
    var name by remember { mutableStateOf(existing?.name ?: prefillName) }
    var quantity by remember { mutableStateOf(existing?.let { qty(it.quantity) } ?: "1") }
    var unit by remember { mutableStateOf(existing?.unit ?: "") }
    var price by remember { mutableStateOf(existing?.priceCentavos?.takeIf { it > 0 }?.let { (it / 100.0).toString().removeSuffix(".0") } ?: "") }
    var aisle by remember { mutableStateOf(existing?.let { Aisle.from(it.category) } ?: Aisle.guess(prefillName)) }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var touched by remember { mutableStateOf(existing != null) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var qtyError by remember { mutableStateOf<String?>(null) }
    var priceError by remember { mutableStateOf<String?>(null) }
    val recent by (history ?: flowOf(emptyList())).collectAsStateWithLifecycle(emptyList())

    // New items: fill the aisle, unit and price from what you bought before, until you touch those fields yourself.
    LaunchedEffect(name) {
        if (existing != null || touched) return@LaunchedEffect
        val prod = products.firstOrNull { it.key == name.trim().lowercase() }
        aisle = prod?.let { Aisle.from(it.category) } ?: Aisle.guess(name)
        if (prod != null) {
            unit = prod.unit
            if (prod.unitPriceCentavos > 0) price = (prod.unitPriceCentavos / 100.0).toString().removeSuffix(".0")
        }
    }

    fun save() {
        val n = name.trim()
        val q = parseQty(quantity)
        val c = if (price.isBlank()) 0L else parsePesos(price)
        nameError = if (n.isEmpty()) "Enter an item name" else null
        qtyError = if (q == null) "Enter a quantity above zero" else null
        priceError = if (c == null) "Enter a price like 65 or 65.50" else null
        if (nameError != null || qtyError != null || priceError != null) return
        if (existing == null) onSaveNew(ItemDraft(n, q!!, unit, aisle.name, note, c!!))
        else onSaveEdit(existing.copy(name = n, quantity = q!!, unit = unit, category = aisle.name, note = note, priceCentavos = c!!))
    }

    PantriSheet(onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (existing == null) "Add item" else "Edit item", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            if (favorite != null) IconButton(onFavorite) {
                Icon(if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, if (favorite) "Remove from favourites" else "Add to favourites", tint = p.tomato)
            }
            if (existing != null) IconButton({ onDelete(existing) }) { Icon(Icons.Rounded.DeleteOutline, "Delete item", tint = p.sub) }
        }
        Spacer(Modifier.height(12.dp))
        PantriField(name, { name = it; nameError = null }, "Item", Modifier.fillMaxWidth(), error = nameError)
        Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PantriField(quantity, { quantity = it; qtyError = null; touched = true }, "Quantity", Modifier.weight(1f), KeyboardType.Decimal, qtyError)
            PantriField(price, { price = it; priceError = null; touched = true }, "Price", Modifier.weight(1f), KeyboardType.Decimal, priceError, prefix = "₱")
        }
        Text("Price is for the whole line, not each.", style = MaterialTheme.typography.bodySmall, color = p.muted)
        if (recent.isNotEmpty()) {
            Text(
                "Recent prices: " + recent.take(5).joinToString(" · ") { pesos(it.unitPriceCentavos) },
                style = MaterialTheme.typography.bodySmall, color = p.sub, modifier = Modifier.padding(top = 4.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        SectionLabel("Unit")
        FlowRow(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            units.forEach { u -> Choice(u, unit == u) { unit = if (unit == u) "" else u; touched = true } }
        }
        Spacer(Modifier.height(12.dp))
        SectionLabel("Aisle")
        FlowRow(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Aisle.entries.forEach { a -> Choice(a.label, aisle == a) { aisle = a; touched = true } }
        }
        Spacer(Modifier.height(12.dp))
        PantriField(note, { note = it }, "Note (optional)", Modifier.fillMaxWidth(), ime = androidx.compose.ui.text.input.ImeAction.Done, onDone = ::save)
        Spacer(Modifier.height(16.dp))
        GradientButton(if (existing == null) "Add to list" else "Save", ::save, Modifier.fillMaxWidth())
    }
}
