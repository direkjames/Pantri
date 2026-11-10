package com.pantri.app.ui

import kotlinx.coroutines.flow.catch
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pantri.app.PantriApp
import com.pantri.app.data.BackupData
import com.pantri.app.data.BackupException
import com.pantri.app.data.BackupService
import com.pantri.app.data.Store
import com.pantri.app.data.StorePrice
import com.pantri.app.data.CutoffSummary
import com.pantri.app.data.GroceryList
import com.pantri.app.data.HistoryRow
import com.pantri.app.data.Item
import com.pantri.app.data.ItemDraft
import com.pantri.app.data.ListSummary
import com.pantri.app.data.Meal
import com.pantri.app.data.PantriRepository
import com.pantri.app.data.Product
import com.pantri.app.data.Recipe
import com.pantri.app.data.RecipeIngredient
import com.pantri.app.data.Settings
import com.pantri.app.data.streakOf
import com.pantri.app.domain.Cutoff
import com.pantri.app.domain.Incoming
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Composable
inline fun <reified T : ViewModel> pantriVm(key: String? = null, crossinline make: (PantriRepository) -> T): T {
    val repo = (LocalContext.current.applicationContext as PantriApp).repo
    return viewModel(key = key, factory = viewModelFactory { initializer { make(repo) } })
}

/** Today's cutoff, re-checked every minute so the app rolls over at midnight without a restart. */
fun currentCutoff(): Flow<Cutoff> = flow {
    while (true) {
        emit(Cutoff.of(PantriRepository.today()))
        delay(60_000)
    }
}.distinctUntilChanged()

private fun <T> Flow<T>.hold(scope: kotlinx.coroutines.CoroutineScope, initial: T): StateFlow<T> =
    catch { UiErrors.report(it) }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), initial)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeVm(private val repo: PantriRepository) : ViewModel() {
    val lists = repo.lists().hold(viewModelScope, emptyList<ListSummary>())
    val templates = repo.templates().hold(viewModelScope, emptyList<ListSummary>())
    val cutoff = currentCutoff().hold(viewModelScope, Cutoff.of(PantriRepository.today()))
    val summary: StateFlow<CutoffSummary?> = currentCutoff().flatMapLatest { repo.observeCutoff(it) }.hold(viewModelScope, null)
    val streak = currentCutoff().flatMapLatest { c -> repo.observeHistory(c).map { streakOf(it, c.previous()) } }.hold(viewModelScope, 0)

    init {
        viewModelScope.launch(safeLaunch) { currentCutoff().collect { repo.ensureBudget(it) } }
    }

    fun createList(name: String, templateId: Long?, onCreated: (Long) -> Unit) = viewModelScope.launch(safeLaunch) {
        onCreated(if (templateId == null) repo.createList(name) else repo.createFromTemplate(templateId, name))
    }

    fun setCutoffBudget(centavos: Long) = viewModelScope.launch(safeLaunch) { repo.setCutoffBudget(cutoff.value, centavos) }
    fun deleteTemplate(id: Long) = viewModelScope.launch(safeLaunch) { repo.deleteList(id) }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ListVm(private val repo: PantriRepository, val listId: Long) : ViewModel() {
    val list: StateFlow<GroceryList?> = repo.list(listId).hold(viewModelScope, null)
    val items = repo.items(listId).hold(viewModelScope, emptyList<Item>())
    val products = repo.products().hold(viewModelScope, emptyList<Product>())
    val summary: StateFlow<CutoffSummary?> = currentCutoff().flatMapLatest { repo.observeCutoff(it) }.hold(viewModelScope, null)
    val stores = repo.stores().hold(viewModelScope, emptyList<Store>())

    fun priceHistory(key: String) = repo.priceHistory(key)
    /** Ties the list to [storeId], or to a brand new store when [newName] is typed (that wins). */
    fun pickStore(storeId: Long?, newName: String) = viewModelScope.launch(safeLaunch) {
        repo.setListStore(listId, if (newName.isNotBlank()) repo.addStore(newName) else storeId)
    }

    fun addQuick(name: String, product: Product?) = viewModelScope.launch(safeLaunch) {
        val n = name.trim()
        if (n.isEmpty()) return@launch
        val unit = product?.unit ?: ""
        val price = product?.unitPriceCentavos ?: 0L
        repo.addItem(listId, ItemDraft(n, 1.0, unit, product?.category ?: com.pantri.app.data.Aisle.guess(n).name, "", price))
    }

    fun addItem(draft: ItemDraft) = viewModelScope.launch(safeLaunch) { repo.addItem(listId, draft) }
    fun updateItem(item: Item) = viewModelScope.launch(safeLaunch) { repo.updateItem(item) }
    fun toggle(item: Item) = viewModelScope.launch(safeLaunch) { repo.setChecked(item, !item.checked) }
    fun setChecked(item: Item, on: Boolean) = viewModelScope.launch(safeLaunch) { repo.setChecked(item, on) }
    fun delete(item: Item) = viewModelScope.launch(safeLaunch) { repo.deleteItem(item) }
    fun restore(item: Item) = viewModelScope.launch(safeLaunch) { repo.restoreItem(item) }
    fun reorder(ordered: List<Item>) = viewModelScope.launch(safeLaunch) { repo.reorder(ordered) }
    fun toggleFavorite(key: String) = viewModelScope.launch(safeLaunch) { repo.toggleFavorite(key) }
    fun rename(name: String) = viewModelScope.launch(safeLaunch) { list.value?.let { repo.renameList(it, name) } }
    fun saveAsTemplate(name: String) = viewModelScope.launch(safeLaunch) { repo.saveAsTemplate(listId, name) }
    fun removeChecked() = viewModelScope.launch(safeLaunch) { repo.removeChecked(listId) }
    fun deleteList(onDone: () -> Unit) = viewModelScope.launch(safeLaunch) { repo.deleteList(listId); onDone() }
}

class RecipesVm(private val repo: PantriRepository) : ViewModel() {
    val recipes = repo.recipes().hold(viewModelScope, emptyList<Recipe>())
    val counts = repo.ingredientCounts().hold(viewModelScope, emptyMap<Long, Int>())
    val lists = repo.lists().hold(viewModelScope, emptyList<ListSummary>())

    fun recipe(id: Long) = repo.recipe(id)
    fun ingredients(id: Long) = repo.ingredients(id)

    fun save(recipe: Recipe, ingredients: List<RecipeIngredient>, onDone: (Long) -> Unit) = viewModelScope.launch(safeLaunch) {
        onDone(repo.saveRecipe(recipe, ingredients))
    }

    fun delete(id: Long, onDone: () -> Unit) = viewModelScope.launch(safeLaunch) { repo.deleteRecipe(id); onDone() }

    /** Adds to an existing list, or to a new list when [listId] is null. Reports the list id and how many lines changed. */
    fun addToList(listId: Long?, newListName: String, incoming: List<Incoming>, onDone: (Long, Int) -> Unit) = viewModelScope.launch(safeLaunch) {
        val target = listId ?: repo.createList(newListName)
        onDone(target, repo.addToList(target, incoming))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PlanVm(private val repo: PantriRepository) : ViewModel() {
    private val _weekStart = MutableStateFlow(PantriRepository.today().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))
    val weekStart: StateFlow<LocalDate> = _weekStart
    val meals: StateFlow<List<Meal>> = _weekStart.flatMapLatest { repo.meals(it, it.plusDays(6)) }.hold(viewModelScope, emptyList())
    val recipes = repo.recipes().hold(viewModelScope, emptyList<Recipe>())
    val lists = repo.lists().hold(viewModelScope, emptyList<ListSummary>())
    val counts = repo.ingredientCounts().hold(viewModelScope, emptyMap<Long, Int>())

    fun shiftWeek(weeks: Long) = _weekStart.update { it.plusWeeks(weeks) }
    fun thisWeek() { _weekStart.value = PantriRepository.today().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }
    fun addMeal(day: LocalDate, slot: Int, recipeId: Long?, title: String) = viewModelScope.launch(safeLaunch) { repo.addMeal(day, slot, recipeId, title) }
    fun deleteMeal(id: Long) = viewModelScope.launch(safeLaunch) { repo.deleteMeal(id) }

    fun addWeekToList(listId: Long?, newListName: String, onDone: (Long, Int) -> Unit) = viewModelScope.launch(safeLaunch) {
        val start = _weekStart.value
        val incoming = repo.ingredientsForPlan(start, start.plusDays(6))
        val target = listId ?: repo.createList(newListName)
        onDone(target, repo.addToList(target, incoming))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetVm(private val repo: PantriRepository) : ViewModel() {
    val cutoff = currentCutoff().hold(viewModelScope, Cutoff.of(PantriRepository.today()))
    val summary: StateFlow<CutoffSummary?> = currentCutoff().flatMapLatest { repo.observeCutoff(it) }.hold(viewModelScope, null)
    val history: StateFlow<List<HistoryRow>> = currentCutoff().flatMapLatest { repo.observeHistory(it) }.hold(viewModelScope, emptyList())
    val purchases = currentCutoff().flatMapLatest { repo.observePurchases(it) }.hold(viewModelScope, emptyList<Item>())
    fun setCutoffBudget(centavos: Long) = viewModelScope.launch(safeLaunch) { repo.setCutoffBudget(cutoff.value, centavos) }
}

/** Stores, the price you saw at each one, and the product list to pick from. */
class StoresVm(private val repo: PantriRepository) : ViewModel() {
    val stores = repo.stores().hold(viewModelScope, emptyList<Store>())
    val prices = repo.storePrices().hold(viewModelScope, emptyList<StorePrice>())
    val products = repo.products().hold(viewModelScope, emptyList<Product>())

    fun addStore(name: String, onDone: (Long?) -> Unit = {}) = viewModelScope.launch(safeLaunch) { onDone(repo.addStore(name)) }
    fun renameStore(store: Store, name: String) = viewModelScope.launch(safeLaunch) { repo.renameStore(store, name) }
    fun deleteStore(id: Long) = viewModelScope.launch(safeLaunch) { repo.deleteStore(id) }
    fun setPrice(key: String, storeId: Long, centavos: Long, quantity: Double, unit: String) =
        viewModelScope.launch(safeLaunch) { repo.setStorePrice(key, storeId, centavos, quantity, unit) }
    fun clearPrice(key: String, storeId: Long) = viewModelScope.launch(safeLaunch) { repo.clearStorePrice(key, storeId) }
}

class SettingsVm(private val repo: PantriRepository, private val backup: BackupService) : ViewModel() {
    val settings = repo.settingsFlow.hold(viewModelScope, Settings())

    /** A backup file the user picked, read and checked, waiting for them to confirm the restore. */
    val pending = MutableStateFlow<BackupData?>(null)
    val busy = MutableStateFlow(false)

    private fun run(block: suspend () -> Unit) = viewModelScope.launch(safeLaunch) {
        busy.value = true
        try { block() } catch (e: BackupException) { UiErrors.events.tryEmit(e.message ?: "Something went wrong.") } finally { busy.value = false }
    }

    fun setFolder(uri: String) = viewModelScope.launch(safeLaunch) { repo.setBackupFolder(uri) }
    fun setSchedule(days: Int, weekday: Int) = viewModelScope.launch(safeLaunch) { repo.setBackupSchedule(days, weekday) }
    fun backupNow() = run { backup.backupNow().fold({ UiErrors.events.tryEmit("Backup saved.") }, { throw it }) }
    fun pick(uri: android.net.Uri) = run { pending.value = backup.read(uri) }
    fun cancelRestore() { pending.value = null }
    fun confirmRestore() = run {
        val d = pending.value ?: return@run
        backup.restore(d)
        pending.value = null
        UiErrors.events.tryEmit("Restored from your backup.")
    }

    fun setDark(on: Boolean) = viewModelScope.launch(safeLaunch) { repo.setDarkMode(on) }
    fun setHaptics(on: Boolean) = viewModelScope.launch(safeLaunch) { repo.setHaptics(on) }
    fun setDefaultBudget(centavos: Long) = viewModelScope.launch(safeLaunch) { repo.setDefaultBudget(centavos, Cutoff.of(PantriRepository.today())) }
}
