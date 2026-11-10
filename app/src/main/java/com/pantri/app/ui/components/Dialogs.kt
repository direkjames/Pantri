package com.pantri.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pantri.app.data.ListSummary
import com.pantri.app.domain.parsePesos
import com.pantri.app.ui.theme.pantri

/** Edit a peso amount. Empty, zero or invalid input shows an error instead of saving. */
@Composable
fun BudgetDialog(title: String, note: String, initialCentavos: Long, onSave: (Long) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(if (initialCentavos % 100 == 0L) (initialCentavos / 100).toString() else "%.2f".format(initialCentavos / 100.0)) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    fun save() {
        val v = parsePesos(text)
        if (v == null || v <= 0) error = "Enter an amount above zero" else onSave(v)
    }
    PantriDialog(title, onDismiss, "Save", ::save) {
        Text(note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.pantri.sub)
        PantriField(text, { text = it; error = null }, "Budget", Modifier.padding(top = 4.dp), KeyboardType.Decimal, error, prefix = "₱", ime = androidx.compose.ui.text.input.ImeAction.Done, onDone = ::save)
    }
}

/** Pick an existing list, or start a new one. [onPick] gets a null id when "New list" is chosen. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ListPickerDialog(title: String, lists: List<ListSummary>, defaultNewName: String, confirm: String, onPick: (Long?, String) -> Unit, onDismiss: () -> Unit) {
    var picked by rememberSaveable { mutableStateOf(lists.firstOrNull()?.list?.id) }
    var name by rememberSaveable { mutableStateOf(defaultNewName) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    fun go() {
        if (picked == null && name.isBlank()) error = "Give the new list a name" else onPick(picked, name)
    }
    PantriDialog(title, onDismiss, confirm, ::go) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            lists.forEach { Choice(it.list.name, picked == it.list.id) { picked = it.list.id } }
            Choice("New list", picked == null) { picked = null }
        }
        if (picked == null) PantriField(name, { name = it; error = null }, "List name", error = error, ime = androidx.compose.ui.text.input.ImeAction.Done, onDone = ::go)
    }
}

@Composable
fun Choice(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val p = MaterialTheme.pantri
    val bg = if (selected) p.green else p.tint(com.pantri.app.ui.theme.Tone.Green).first
    val fg = if (selected) Color.White else p.tint(com.pantri.app.ui.theme.Tone.Green).second
    Row(modifier.minimumInteractiveComponentSize().clip(RoundedCornerShape(14.dp)).background(bg).selectable(selected = selected, role = Role.RadioButton, onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = fg, maxLines = 1)
    }
}
