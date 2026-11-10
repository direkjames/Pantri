package com.pantri.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pantri.app.data.Aisle
import com.pantri.app.data.Item
import com.pantri.app.data.Product
import com.pantri.app.domain.pesos
import com.pantri.app.domain.qty
import com.pantri.app.ui.ListVm
import com.pantri.app.ui.LocalSnack
import com.pantri.app.ui.components.EmptyState
import com.pantri.app.ui.components.GradientButton
import com.pantri.app.ui.components.IconTile
import com.pantri.app.ui.components.LinearBar
import com.pantri.app.ui.components.PantriCard
import com.pantri.app.ui.components.PantriCheck
import com.pantri.app.ui.components.PantriDialog
import com.pantri.app.ui.components.PantriField
import com.pantri.app.ui.components.Pill
import com.pantri.app.ui.components.TopBar
import com.pantri.app.ui.components.icon
import com.pantri.app.ui.components.rememberHaptic
import com.pantri.app.ui.components.tone
import com.pantri.app.ui.pantriVm
import com.pantri.app.ui.theme.Tone
import com.pantri.app.ui.theme.pantri
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import androidx.compose.runtime.withFrameNanos

private sealed interface Row2 {
    data class Header(val aisle: Aisle, val count: Int) : Row2
    data class Entry(val item: Item) : Row2
}

@Composable
fun ListScreen(listId: Long, onBack: () -> Unit) {
    val vm = pantriVm(key = "list$listId") { ListVm(it, listId) }
    val list by vm.list.collectAsStateWithLifecycle()
    val dbItems by vm.items.collectAsStateWithLifecycle()
    val products by vm.products.collectAsStateWithLifecycle()
    val summary by vm.summary.collectAsStateWithLifecycle()
    val snack = LocalSnack.current
    val scope = rememberCoroutineScope()
    val haptic = rememberHaptic()
    val p = MaterialTheme.pantri

    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var sheetName by rememberSaveable { mutableStateOf<String?>(null) }
    var menu by rememberSaveable { mutableStateOf(false) }
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }
    var draft by rememberSaveable { mutableStateOf("") }

    // Drag to reorder: we edit a local copy while dragging and save it once on release.
    var working by remember { mutableStateOf<List<Item>?>(null) }
    var dragId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(dbItems) { if (dragId == null) working = null }
    val shown = working ?: dbItems

    val rows = remember(shown) {
        buildList {
            shown.groupBy { Aisle.from(it.category) }.toSortedMap(compareBy { it.ordinal }).forEach { (a, its) ->
                add(Row2.Header(a, its.size))
                (its.filter { !it.checked } + its.filter { it.checked }).forEach { add(Row2.Entry(it)) }
            }
        }
    }
    val listState = rememberLazyListState()
    val shownNow by rememberUpdatedState(shown)
    val signal = remember { Channel<Unit>(Channel.CONFLATED) }
    LaunchedEffect(Unit) {
        for (u in signal) {
            val id = dragId ?: continue
            val info = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "i$id" } ?: continue
            val center = info.offset + info.size / 2f + dragOffset
            val hit = listState.layoutInfo.visibleItemsInfo.firstOrNull {
                it.key != info.key && (it.key as? String)?.startsWith("i") == true && center >= it.offset && center <= it.offset + it.size
            } ?: continue
            val other = (hit.key as String).drop(1).toLong()
            val cur = shownNow
            val a = cur.firstOrNull { it.id == id } ?: continue
            val b = cur.firstOrNull { it.id == other } ?: continue
            if (a.category != b.category || b.checked || a.checked) continue
            val ia = cur.indexOf(a)
            val ib = cur.indexOf(b)
            working = cur.toMutableList().also { it[ia] = b; it[ib] = a }
            dragOffset += (info.offset - hit.offset)
            withFrameNanos { }
            withFrameNanos { }
        }
    }

    val open = dbItems.filter { !it.checked }
    val remaining = open.sumOf { it.priceCentavos }
    val doneCount = dbItems.count { it.checked }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopBar(list?.name ?: "", onBack = onBack, actions = {
                Box {
                    IconButton({ menu = true }) { Icon(Icons.Rounded.MoreVert, "List options", tint = p.sub) }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem({ Text("Rename") }, { menu = false; dialog = "rename" })
                        DropdownMenuItem({ Text("Save as template") }, { menu = false; dialog = "template" })
                        DropdownMenuItem({ Text("Remove done items") }, { menu = false; vm.removeChecked() }, enabled = doneCount > 0)
                        DropdownMenuItem({ Text("Delete list", color = MaterialTheme.colorScheme.error) }, { menu = false; dialog = "delete" })
                    }
                }
            })
            LazyColumn(
                Modifier.weight(1f), listState,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "progress") {
                    PantriCard(Modifier.fillMaxWidth()) {
                        Text("$doneCount of ${dbItems.size} done", style = MaterialTheme.typography.titleSmall)
                        Text("Swipe right to check, left to delete", style = MaterialTheme.typography.bodySmall, color = p.muted)
                        Spacer(Modifier.height(10.dp))
                        LinearBar(if (dbItems.isEmpty()) 0f else doneCount.toFloat() / dbItems.size, Modifier.fillMaxWidth())
                    }
                }
                if (dbItems.isEmpty()) {
                    item(key = "empty") {
                        EmptyState(Icons.Rounded.ShoppingCart, "Nothing here yet", "Type an item below. Pantri remembers what you buy.")
                    }
                }
                items(rows, key = { if (it is Row2.Header) "h${it.aisle.name}" else "i${(it as Row2.Entry).item.id}" }) { r ->
                    when (r) {
                        is Row2.Header -> AisleHeader(r.aisle, r.count, Modifier.padding(top = 8.dp))
                        is Row2.Entry -> {
                            val item = r.item
                            val dragging = dragId == item.id
                            SwipeableItem(
                                item = item,
                                dragging = dragging,
                                dragOffset = if (dragging) dragOffset else 0f,
                                modifier = if (dragging) Modifier else Modifier.animateItem(),
                                onCheck = { vm.toggle(item) },
                                onTap = { editingId = item.id },
                                onDelete = {
                                    vm.delete(item)
                                    scope.launch {
                                        if (snack.showSnackbar("Removed ${item.name}", "Undo") == SnackbarResult.ActionPerformed) vm.restore(item)
                                    }
                                },
                                onDragStart = { working = dbItems; dragId = item.id; dragOffset = 0f; haptic(HapticFeedbackType.LongPress) },
                                onDrag = { dy -> dragOffset += dy; signal.trySend(Unit) },
                                onDragEnd = {
                                    working?.let { vm.reorder(it) }
                                    dragId = null
                                    dragOffset = 0f
                                },
                            )
                        }
                    }
                }
            }
            BottomBar(
                draft = draft, onDraft = { draft = it }, products = products, existing = open.map { it.name.lowercase() }.toSet(),
                remaining = remaining, left = summary?.left,
                onSubmit = { name ->
                    val prod = products.firstOrNull { it.key == name.trim().lowercase() }
                    vm.addQuick(name, prod); draft = ""
                },
                onPick = { prod -> vm.addQuick(prod.name, prod); draft = "" },
                onMore = { sheetName = draft; draft = "" },
            )
        }
    }

    val editing = dbItems.firstOrNull { it.id == editingId }
    if (sheetName != null || editing != null) {
        val item = editing
        ItemSheet(
            existing = item, prefillName = sheetName.orEmpty(), products = products,
            history = item?.let { vm.priceHistory(it.name.trim().lowercase()) },
            favorite = item?.let { i -> products.firstOrNull { it.key == i.name.trim().lowercase() }?.favorite },
            onFavorite = { item?.let { vm.toggleFavorite(it.name.trim().lowercase()) } },
            onDismiss = { editingId = null; sheetName = null },
            onSaveNew = { vm.addItem(it); sheetName = null },
            onSaveEdit = { vm.updateItem(it); editingId = null },
            onDelete = {
                editingId = null
                vm.delete(it)
                scope.launch { if (snack.showSnackbar("Removed ${it.name}", "Undo") == SnackbarResult.ActionPerformed) vm.restore(it) }
            },
        )
    }

    when (dialog) {
        "rename" -> TextPrompt("Rename list", "Name", list?.name.orEmpty(), "Save", { dialog = null }) { vm.rename(it); dialog = null }
        "template" -> TextPrompt("Save as template", "Template name", list?.name.orEmpty(), "Save", { dialog = null }) {
            vm.saveAsTemplate(it); dialog = null
            scope.launch { snack.showSnackbar("Saved. Find it under Templates on the home screen.") }
        }
        "delete" -> PantriDialog("Delete this list?", { dialog = null }, "Delete", { dialog = null; vm.deleteList(onBack) }) {
            Text("${dbItems.size} items will be removed. This can't be undone.", style = MaterialTheme.typography.bodyMedium, color = p.sub)
        }
    }
}

@Composable
private fun AisleHeader(aisle: Aisle, count: Int, modifier: Modifier = Modifier) {
    Row(modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconTile(aisle.icon(), aisle.tone(), size = 28.dp)
        Spacer(Modifier.size(8.dp))
        Text(aisle.label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Text("$count", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.pantri.muted)
    }
}

@Composable
private fun SwipeableItem(
    item: Item,
    dragging: Boolean,
    dragOffset: Float,
    modifier: Modifier,
    onCheck: () -> Unit,
    onTap: () -> Unit,
    onDelete: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    val p = MaterialTheme.pantri
    val state = rememberSwipeToDismissBoxState()
    // A row restored by Undo can come back with its old "swiped away" state, so settle it before reacting to swipes.
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (state.currentValue != SwipeToDismissBoxValue.Settled) state.snapTo(SwipeToDismissBoxValue.Settled)
        armed = true
    }
    LaunchedEffect(state.currentValue, armed) {
        if (!armed) return@LaunchedEffect
        when (state.currentValue) {
            SwipeToDismissBoxValue.StartToEnd -> { onCheck(); state.snapTo(SwipeToDismissBoxValue.Settled) }
            SwipeToDismissBoxValue.EndToStart -> onDelete()
            SwipeToDismissBoxValue.Settled -> Unit
        }
    }
    val shape = RoundedCornerShape(18.dp)
    SwipeToDismissBox(
        state,
        modifier = modifier.zIndex(if (dragging) 1f else 0f).graphicsLayer { translationY = dragOffset },
        backgroundContent = {
            val target = state.targetValue
            val bg by animateColorAsState(
                when (target) {
                    SwipeToDismissBoxValue.StartToEnd -> p.green
                    SwipeToDismissBoxValue.EndToStart -> p.tomato
                    else -> p.line
                },
                label = "swipeBg",
            )
            Row(Modifier.fillMaxSize().background(bg, shape).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Check, null, tint = Color.White)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Rounded.DeleteOutline, null, tint = Color.White)
            }
        },
    ) {
        val aisle = Aisle.from(item.category)
        Row(
            Modifier
                .fillMaxWidth()
                .shadow(if (dragging) 14.dp else 3.dp, shape, ambientColor = p.shadow.copy(alpha = 0.12f), spotColor = p.shadow.copy(alpha = 0.16f))
                .background(p.card, shape)
                .clickable(onClick = onTap)
                .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PantriCheck(item.checked, { onCheck() }, label = item.name)
            Spacer(Modifier.size(4.dp))
            IconTile(aisle.icon(), aisle.tone(), size = 34.dp)
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.name, style = MaterialTheme.typography.titleSmall,
                    color = if (item.checked) p.muted else p.ink,
                    textDecoration = if (item.checked) TextDecoration.LineThrough else null, maxLines = 2,
                )
                val sub = listOf(qty(item.quantity) + if (item.unit.isBlank()) "" else " ${item.unit}", item.note).filter { it.isNotBlank() }.joinToString(" · ")
                Text(sub, style = MaterialTheme.typography.bodySmall, color = p.sub, maxLines = 1)
            }
            if (item.priceCentavos > 0) Pill(pesos(item.priceCentavos), aisle.tone())
            if (!item.checked) {
                Box(
                    Modifier.size(48.dp).pointerInput(item.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { onDragStart() },
                            onDrag = { change, amount -> change.consume(); onDrag(amount.y) },
                            onDragEnd = onDragEnd,
                            onDragCancel = onDragEnd,
                        )
                    },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.DragIndicator, "Hold and drag to reorder", tint = p.muted) }
            } else {
                Spacer(Modifier.size(8.dp))
            }
        }
    }
}

@Composable
private fun BottomBar(
    draft: String, onDraft: (String) -> Unit, products: List<Product>, existing: Set<String>,
    remaining: Long, left: Long?, onSubmit: (String) -> Unit, onPick: (Product) -> Unit, onMore: () -> Unit,
) {
    val p = MaterialTheme.pantri
    val suggestions = remember(draft, products, existing) {
        val q = draft.trim().lowercase()
        products.filter { it.key !in existing && (if (q.isEmpty()) (it.favorite || it.timesBought > 0) else it.key.contains(q)) }.take(8)
    }
    Column(Modifier.fillMaxWidth().background(p.bg).imePadding().navigationBarsPadding().padding(bottom = 8.dp)) {
        if (suggestions.isNotEmpty()) {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(suggestions, key = { it.key }) { s ->
                    Row(
                        Modifier.minimumInteractiveComponentSize().background(p.tint(Tone.Green).first, RoundedCornerShape(14.dp)).clickable(role = Role.Button) { onPick(s) }.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (s.favorite) { Icon(Icons.Rounded.Favorite, null, Modifier.size(14.dp), tint = p.tomato); Spacer(Modifier.size(4.dp)) }
                        Text(s.name, style = MaterialTheme.typography.labelLarge, color = p.tint(Tone.Green).second, maxLines = 1)
                    }
                }
            }
        }
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            TextField(
                draft, onDraft, Modifier.weight(1f), singleLine = true,
                placeholder = { Text(if (suggestions.isEmpty() || draft.isNotEmpty()) "Add an item" else "Add an item or tap to buy again") },
                shape = RoundedCornerShape(20.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (draft.isNotBlank()) onSubmit(draft) }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = p.card, unfocusedContainerColor = p.card,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, cursorColor = p.green,
                ),
            )
            IconButton(onMore) { Icon(Icons.Rounded.Tune, "Add with details", tint = p.green) }
            Box(
                Modifier.size(46.dp).background(androidx.compose.ui.graphics.Brush.linearGradient(p.heroGreen), RoundedCornerShape(16.dp))
                    .clickable(enabled = draft.isNotBlank()) { onSubmit(draft) },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.ArrowUpward, "Add item", tint = Color.White) }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.padding(horizontal = 16.dp).fillMaxWidth()
                .background(androidx.compose.ui.graphics.Brush.linearGradient(if (left != null && remaining > left) p.heroSunset else p.heroGreen), RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Still to buy", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
                Text(pesos(remaining), style = MaterialTheme.typography.headlineSmall, color = Color.White)
            }
            if (left != null && remaining > 0) {
                val over = remaining - left
                com.pantri.app.ui.components.GlassPill(if (over > 0) "${pesos(over)} over what's left" else "Fits your budget")
            }
        }
    }
}

@Composable
fun TextPrompt(title: String, label: String, initial: String, confirm: String, onDismiss: () -> Unit, onDone: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf(initial) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    fun go() { if (text.isBlank()) error = "Enter a name" else onDone(text.trim()) }
    PantriDialog(title, onDismiss, confirm, ::go) {
        PantriField(text, { text = it; error = null }, label, error = error, ime = ImeAction.Done, onDone = ::go)
    }
}
