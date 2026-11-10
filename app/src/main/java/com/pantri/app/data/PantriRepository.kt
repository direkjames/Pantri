package com.pantri.app.data

import androidx.room.withTransaction
import com.pantri.app.domain.Cutoff
import com.pantri.app.domain.Incoming
import com.pantri.app.domain.Limits
import com.pantri.app.domain.mergeIngredients
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId

data class ItemDraft(
    val name: String,
    val quantity: Double = 1.0,
    val unit: String = "",
    val category: String = Aisle.Other.name,
    val note: String = "",
    val priceCentavos: Long = 0,
)

data class CutoffSummary(
    val cutoff: Cutoff,
    val budget: Long,
    val spent: Long,
    val planned: Long,
    val byAisle: List<Pair<Aisle, Long>>,
) {
    val left: Long get() = budget - spent
    val fraction: Float get() = if (budget <= 0) 0f else (spent.toFloat() / budget).coerceIn(0f, 1f)
}

data class HistoryRow(val cutoff: Cutoff, val budget: Long, val spent: Long)

class PantriRepository(
    private val db: PantriDatabase,
    private val settings: SettingsStore,
    private val now: () -> Long = { System.currentTimeMillis() },
) {
    private val dao = db.dao()
    val settingsFlow: Flow<Settings> = settings.settings

    // Lists and templates
    fun lists() = dao.observeLists(false)
    fun templates() = dao.observeLists(true)
    fun list(id: Long) = dao.observeList(id)
    fun items(listId: Long) = dao.observeItems(listId)

    suspend fun createList(name: String): Long = dao.insertList(GroceryList(name = name.clean(Limits.NAME).ifBlank { "New list" }, createdAt = now()))

    suspend fun renameList(list: GroceryList, name: String) {
        if (name.isNotBlank()) dao.updateList(list.copy(name = name.clean(Limits.NAME)))
    }

    suspend fun deleteList(id: Long) = dao.deleteList(id)

    /** Copies [sourceId] into a new list or template; items come over unchecked and unpriced-history free. */
    private suspend fun copyList(sourceId: Long, name: String, template: Boolean): Long = db.withTransaction {
        val id = dao.insertList(GroceryList(name = name.clean(Limits.NAME).ifBlank { "New list" }, isTemplate = template, createdAt = now()))
        dao.insertItems(dao.getItems(sourceId).map { it.copy(id = 0, listId = id, checked = false, checkedAt = null, createdAt = now()) })
        id
    }

    suspend fun saveAsTemplate(listId: Long, name: String) = copyList(listId, name, true)
    suspend fun createFromTemplate(templateId: Long, name: String) = copyList(templateId, name, false)

    suspend fun removeChecked(listId: Long) = db.withTransaction {
        dao.getItems(listId).filter { it.checked }.forEach { dao.deleteItem(it) }
    }

    // Items
    suspend fun addItem(listId: Long, draft: ItemDraft): Long = db.withTransaction {
        val name = draft.name.clean(Limits.NAME)
        val unit = draft.unit.clean(Limits.UNIT)
        val qty = draft.quantity.coerceIn(0.0001, Limits.MAX_QUANTITY)
        val price = draft.priceCentavos.coerceIn(0, Limits.MAX_PRICE_CENTAVOS)
        val id = dao.insertItem(
            Item(
                listId = listId, name = name, quantity = qty, unit = unit, category = draft.category,
                note = draft.note.clean(Limits.NOTE), priceCentavos = price, sortOrder = dao.maxSortOrder(listId) + 1, createdAt = now(),
            ),
        )
        rememberProduct(name, draft.category, unit, qty, price)
        id
    }

    suspend fun updateItem(item: Item) = db.withTransaction {
        val clean = item.copy(
            name = item.name.clean(Limits.NAME), unit = item.unit.clean(Limits.UNIT), note = item.note.clean(Limits.NOTE),
            quantity = item.quantity.coerceIn(0.0001, Limits.MAX_QUANTITY), priceCentavos = item.priceCentavos.coerceIn(0, Limits.MAX_PRICE_CENTAVOS),
        )
        dao.updateItem(clean)
        rememberProduct(clean.name, clean.category, clean.unit, clean.quantity, clean.priceCentavos)
    }

    suspend fun deleteItem(item: Item) = dao.deleteItem(item)

    /** Puts a deleted item back (undo). Does nothing if its list was deleted in the meantime. */
    suspend fun restoreItem(item: Item) {
        if (dao.getList(item.listId) != null) dao.insertItem(item)
    }

    suspend fun setChecked(item: Item, checked: Boolean) = db.withTransaction {
        val t = now()
        dao.updateItem(item.copy(checked = checked, checkedAt = if (checked) t else null))
        if (checked) {
            val key = item.name.trim().lowercase()
            val unitPrice = if (item.priceCentavos > 0 && item.quantity > 0) Math.round(item.priceCentavos / item.quantity) else 0L
            val p = dao.getProduct(key)
            dao.upsertProduct(
                (p ?: Product(key, item.name.trim(), item.category, item.unit, unitPrice, 0, false, t))
                    .copy(timesBought = (p?.timesBought ?: 0) + 1, lastUsedAt = t, unitPriceCentavos = if (unitPrice > 0) unitPrice else (p?.unitPriceCentavos ?: 0)),
            )
            // Checking, unchecking and checking again must not fill the price history with the same price.
            val last = dao.lastPrice(key)
            if (unitPrice > 0 && !(last != null && last.unitPriceCentavos == unitPrice && t - last.at < DAY_MS)) {
                dao.insertPrice(PriceEntry(productKey = key, unitPriceCentavos = unitPrice, at = t))
            }
            // On a list tied to a store, what you paid becomes that store's price for the item.
            val storeId = dao.getList(item.listId)?.storeId
            if (storeId != null && item.priceCentavos > 0 && dao.storeExists(storeId) > 0) {
                dao.upsertStorePrice(StorePrice(productKey = key, storeId = storeId, priceCentavos = item.priceCentavos, quantity = item.quantity, unit = item.unit, at = t))
            }
        }
    }

    // Stores and per-store prices
    fun stores() = dao.observeStores()
    fun storePrices() = dao.observeStorePrices()

    /** Adds a store, or returns the one you already have with that name. Null when the name is blank or you hit the limit. */
    suspend fun addStore(name: String): Long? = db.withTransaction {
        val n = name.clean(Limits.NAME)
        if (n.isEmpty()) return@withTransaction null
        val all = dao.allStores()
        all.firstOrNull { it.name.equals(n, ignoreCase = true) }?.let { return@withTransaction it.id }
        if (all.size >= Limits.MAX_STORES) null else dao.insertStore(Store(name = n, createdAt = now()))
    }

    suspend fun renameStore(store: Store, name: String) {
        if (name.isNotBlank()) dao.updateStore(store.copy(name = name.clean(Limits.NAME)))
    }

    suspend fun deleteStore(id: Long) = db.withTransaction {
        dao.clearListStore(id)
        dao.deleteStore(id)
    }

    /** Saves what [productKey] costs at [storeId]: [priceCentavos] pays for [quantity] [unit]. Ignored if the store was deleted meanwhile. */
    suspend fun setStorePrice(productKey: String, storeId: Long, priceCentavos: Long, quantity: Double, unit: String) {
        if (productKey.isBlank() || dao.storeExists(storeId) == 0) return
        dao.upsertStorePrice(
            StorePrice(
                productKey = productKey.trim().lowercase().take(Limits.NAME), storeId = storeId,
                priceCentavos = priceCentavos.coerceIn(1, Limits.MAX_PRICE_CENTAVOS), quantity = quantity.coerceIn(0.0001, Limits.MAX_QUANTITY),
                unit = unit.clean(Limits.UNIT), at = now(),
            ),
        )
    }

    suspend fun clearStorePrice(productKey: String, storeId: Long) = dao.deleteStorePrice(productKey, storeId)

    suspend fun setListStore(listId: Long, storeId: Long?) =
        dao.setListStore(listId, storeId?.takeIf { dao.storeExists(it) > 0 })

    // Backup and restore
    suspend fun exportBackup(): BackupData = db.withTransaction {
        val s = settings.settings.first()
        BackupData(
            now(), s.darkMode, s.haptics, s.defaultBudgetCentavos, dao.allLists(), dao.allItems(), dao.allProducts(), dao.allPrices(),
            dao.allRecipes(), dao.allIngredients(), dao.allMeals(), dao.allBudgets(), dao.allStores(), dao.allStorePrices(),
        )
    }

    /** Replaces everything with [d] in one transaction: if anything fails, nothing changes. Rows pointing at missing parents are dropped. */
    suspend fun restoreBackup(d: BackupData) {
        db.withTransaction {
            val storeIds = d.stores.map { it.id }.toSet()
            val lists = d.lists.distinctBy { it.id }.map { if (it.storeId != null && it.storeId !in storeIds) it.copy(storeId = null) else it }
            val listIds = lists.map { it.id }.toSet()
            val recipes = d.recipes.distinctBy { it.id }
            val recipeIds = recipes.map { it.id }.toSet()
            dao.wipeLists(); dao.wipeRecipes(); dao.wipeMeals(); dao.wipeProducts(); dao.wipePrices(); dao.wipeBudgets(); dao.wipeStores()
            dao.putStores(d.stores.distinctBy { it.id })
            dao.putLists(lists)
            dao.insertItems(d.items.distinctBy { it.id }.filter { it.listId in listIds })
            dao.putRecipes(recipes)
            dao.insertIngredients(d.ingredients.distinctBy { it.id }.filter { it.recipeId in recipeIds })
            dao.putMeals(d.meals.distinctBy { it.id }.map { if (it.recipeId != null && it.recipeId !in recipeIds) it.copy(recipeId = null) else it })
            dao.putProducts(d.products.distinctBy { it.key })
            dao.putPrices(d.prices.distinctBy { it.id })
            dao.putBudgets(d.budgets.distinctBy { it.key })
            dao.putStorePrices(d.storePrices.distinctBy { it.productKey to it.storeId }.filter { it.storeId in storeIds })
        }
        d.darkMode?.let { settings.setDarkMode(it) }
        d.haptics?.let { settings.setHaptics(it) }
        d.defaultBudget?.takeIf { it > 0 }?.let { settings.setDefaultBudget(it) }
    }

    suspend fun setBackupFolder(uri: String) = settings.setBackupFolder(uri)
    suspend fun setBackupSchedule(days: Int, weekday: Int) = settings.setBackupSchedule(days, weekday)
    suspend fun setBackupResult(at: Long, error: String?) = settings.setBackupResult(at, error)

    /** Saves the new order. [ordered] is the whole list in its new order. */
    suspend fun reorder(ordered: List<Item>) = db.withTransaction {
        dao.updateItems(ordered.mapIndexed { i, it -> it.copy(sortOrder = i) })
    }

    // Products and prices
    fun products() = dao.observeProducts()
    fun priceHistory(key: String) = dao.observePrices(key)

    suspend fun toggleFavorite(key: String) {
        dao.getProduct(key)?.let { dao.upsertProduct(it.copy(favorite = !it.favorite)) }
    }

    private suspend fun rememberProduct(name: String, category: String, unit: String, quantity: Double, priceCentavos: Long) {
        if (name.isBlank()) return
        val key = name.lowercase()
        val old = dao.getProduct(key)
        val unitPrice = if (priceCentavos > 0 && quantity > 0) Math.round(priceCentavos / quantity) else old?.unitPriceCentavos ?: 0
        dao.upsertProduct(
            Product(key, name, category, unit, unitPrice, old?.timesBought ?: 0, old?.favorite ?: false, now()),
        )
    }

    // Recipes
    fun recipes() = dao.observeRecipes()
    fun recipe(id: Long) = dao.observeRecipe(id)
    fun ingredients(recipeId: Long) = dao.observeIngredients(recipeId)
    fun ingredientCounts() = dao.observeIngredientCounts().map { rows -> rows.associate { it.recipeId to it.n } }

    suspend fun saveRecipe(recipe: Recipe, ingredients: List<RecipeIngredient>): Long = db.withTransaction {
        val safe = recipe.copy(title = recipe.title.clean(Limits.NAME), steps = recipe.steps.trim().take(Limits.STEPS))
        val id = if (safe.id == 0L) dao.insertRecipe(safe.copy(createdAt = now())) else safe.also { dao.updateRecipe(it) }.id
        dao.clearIngredients(id)
        dao.insertIngredients(
            ingredients.filter { it.name.isNotBlank() }.take(200).mapIndexed { i, ing ->
                ing.copy(id = 0, recipeId = id, name = ing.name.clean(Limits.NAME), unit = ing.unit.clean(Limits.UNIT), quantity = ing.quantity.coerceIn(0.0001, Limits.MAX_QUANTITY), sortOrder = i)
            },
        )
        id
    }

    suspend fun deleteRecipe(id: Long) = dao.deleteRecipe(id)

    suspend fun ingredientsOf(recipeId: Long): List<Incoming> =
        dao.getIngredients(recipeId).map { Incoming(it.name, it.quantity, it.unit, it.category) }

    /** Adds [incoming] to [listId], adding up what is already there. Returns how many lines were added or changed. */
    suspend fun addToList(listId: Long, incoming: List<Incoming>): Int = db.withTransaction {
        val plan = mergeIngredients(dao.getItems(listId), incoming)
        dao.updateItems(plan.updates)
        var order = dao.maxSortOrder(listId)
        for (i in plan.inserts) {
            val key = i.name.trim().lowercase()
            val unitPrice = dao.getProduct(key)?.unitPriceCentavos ?: 0
            val price = Math.round(unitPrice * i.quantity)
            dao.insertItem(
                Item(listId = listId, name = i.name, quantity = i.quantity, unit = i.unit, category = i.category, priceCentavos = price, sortOrder = ++order, createdAt = now()),
            )
            rememberProduct(i.name, i.category, i.unit, i.quantity, price)
        }
        plan.updates.size + plan.inserts.size
    }

    // Meal plan
    fun meals(from: LocalDate, to: LocalDate) = dao.observeMeals(from.toEpochDay(), to.toEpochDay())

    suspend fun addMeal(day: LocalDate, slot: Int, recipeId: Long?, title: String) {
        // Plan the recipe only if it still exists; a deleted one would break the link.
        val safeRecipe = recipeId?.takeIf { dao.getRecipe(it) != null }
        if (title.isNotBlank() && slot in 0..3) dao.insertMeal(Meal(epochDay = day.toEpochDay(), slot = slot, recipeId = safeRecipe, title = title.clean(Limits.NAME)))
    }

    suspend fun deleteMeal(id: Long) = dao.deleteMeal(id)

    /** Every ingredient of every recipe planned between [from] and [to] (a recipe planned twice counts twice). */
    suspend fun ingredientsForPlan(from: LocalDate, to: LocalDate): List<Incoming> =
        dao.getMeals(from.toEpochDay(), to.toEpochDay()).mapNotNull { it.recipeId }.flatMap { ingredientsOf(it) }

    suspend fun setDarkMode(on: Boolean) = settings.setDarkMode(on)
    suspend fun setHaptics(on: Boolean) = settings.setHaptics(on)

    // Budget
    suspend fun setDefaultBudget(centavos: Long, current: Cutoff) {
        settings.setDefaultBudget(centavos)
        dao.upsertBudget(CutoffBudget(current.key, centavos))
    }

    suspend fun setCutoffBudget(cutoff: Cutoff, centavos: Long) = dao.upsertBudget(CutoffBudget(cutoff.key, centavos))

    /** Freezes today's budget for [cutoff] the first time it is seen, so later edits never rewrite history. */
    suspend fun ensureBudget(cutoff: Cutoff) {
        if (dao.getBudget(cutoff.key) == null) dao.upsertBudget(CutoffBudget(cutoff.key, settings.settings.first().defaultBudgetCentavos))
    }

    fun observeCutoff(cutoff: Cutoff): Flow<CutoffSummary> = combine(
        dao.observeBudget(cutoff.key),
        settings.settings,
        dao.observePurchases(cutoff.startMillis(), cutoff.endMillis()),
        dao.observePlanned(),
    ) { b, s, purchases, planned ->
        val byAisle = purchases.groupBy { Aisle.from(it.category) }.map { (a, l) -> a to l.sumOf { it.priceCentavos } }.sortedByDescending { it.second }
        CutoffSummary(cutoff, b?.amountCentavos ?: s.defaultBudgetCentavos, purchases.sumOf { it.priceCentavos }, planned, byAisle)
    }

    fun observePurchases(cutoff: Cutoff) = dao.observePurchases(cutoff.startMillis(), cutoff.endMillis())

    /** The last [count] finished cutoffs before [current], newest first. Only cutoffs that had a saved budget appear. */
    fun observeHistory(current: Cutoff, count: Int = 6): Flow<List<HistoryRow>> {
        val cutoffs = generateSequence(current.previous()) { it.previous() }.take(count).toList()
        return combine(dao.observeBudgets(), dao.observeCheckedSince(cutoffs.last().startMillis())) { budgets, checked ->
            val byKey = budgets.associateBy { it.key }
            cutoffs.mapNotNull { c ->
                val b = byKey[c.key] ?: return@mapNotNull null
                val spent = checked.filter { it.checkedAt in c.startMillis()..c.endMillis() }.sumOf { it.priceCentavos }
                HistoryRow(c, b.amountCentavos, spent)
            }
        }
    }

    companion object {
        private const val DAY_MS = 24L * 60 * 60 * 1000
        fun today(): LocalDate = LocalDate.now(ZoneId.systemDefault())
    }
}

private fun String.clean(max: Int) = trim().take(max)

/** Finished cutoffs in a row, newest first, where you spent something and stayed within budget. */
fun streakOf(history: List<HistoryRow>, expectedNewest: Cutoff): Int {
    var expect = expectedNewest
    var n = 0
    for (h in history) {
        if (h.cutoff != expect || h.spent <= 0 || h.spent > h.budget) break
        n++
        expect = expect.previous()
    }
    return n
}
