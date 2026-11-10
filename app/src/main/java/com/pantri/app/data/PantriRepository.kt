package com.pantri.app.data

import androidx.room.withTransaction
import com.pantri.app.domain.Cutoff
import com.pantri.app.domain.Incoming
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

    suspend fun createList(name: String): Long = dao.insertList(GroceryList(name = name.trim().ifBlank { "New list" }, createdAt = now()))

    suspend fun renameList(list: GroceryList, name: String) {
        if (name.isNotBlank()) dao.updateList(list.copy(name = name.trim()))
    }

    suspend fun deleteList(id: Long) = dao.deleteList(id)

    /** Copies [sourceId] into a new list or template; items come over unchecked and unpriced-history free. */
    private suspend fun copyList(sourceId: Long, name: String, template: Boolean): Long = db.withTransaction {
        val id = dao.insertList(GroceryList(name = name.trim().ifBlank { "New list" }, isTemplate = template, createdAt = now()))
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
        val name = draft.name.trim()
        val id = dao.insertItem(
            Item(
                listId = listId, name = name, quantity = draft.quantity, unit = draft.unit.trim(), category = draft.category,
                note = draft.note.trim(), priceCentavos = draft.priceCentavos, sortOrder = dao.maxSortOrder(listId) + 1, createdAt = now(),
            ),
        )
        rememberProduct(name, draft.category, draft.unit.trim(), draft.quantity, draft.priceCentavos)
        id
    }

    suspend fun updateItem(item: Item) = db.withTransaction {
        dao.updateItem(item.copy(name = item.name.trim(), unit = item.unit.trim(), note = item.note.trim()))
        rememberProduct(item.name.trim(), item.category, item.unit.trim(), item.quantity, item.priceCentavos)
    }

    suspend fun deleteItem(item: Item) = dao.deleteItem(item)

    /** Puts a deleted item back (undo). */
    suspend fun restoreItem(item: Item) {
        dao.insertItem(item)
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
            if (unitPrice > 0) dao.insertPrice(PriceEntry(productKey = key, unitPriceCentavos = unitPrice, at = t))
        }
    }

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
        val id = if (recipe.id == 0L) dao.insertRecipe(recipe.copy(createdAt = now())) else recipe.also { dao.updateRecipe(it) }.id
        dao.clearIngredients(id)
        dao.insertIngredients(
            ingredients.filter { it.name.isNotBlank() }.mapIndexed { i, ing -> ing.copy(id = 0, recipeId = id, name = ing.name.trim(), sortOrder = i) },
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
        if (title.isNotBlank()) dao.insertMeal(Meal(epochDay = day.toEpochDay(), slot = slot, recipeId = recipeId, title = title.trim()))
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
        fun today(): LocalDate = LocalDate.now(ZoneId.systemDefault())
    }
}

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
