package com.pantri.app.data

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** The newest backup format this app can read. Bump it only when old apps could not read the new file. */
const val BACKUP_FORMAT = 1

class BackupException(message: String) : Exception(message)

/** Everything in the app, as plain rows. Appearance settings travel with it; the backup folder and schedule do not. */
data class BackupData(
    val createdAt: Long,
    val darkMode: Boolean? = null,
    val haptics: Boolean? = null,
    val defaultBudget: Long? = null,
    val lists: List<GroceryList> = emptyList(),
    val items: List<Item> = emptyList(),
    val products: List<Product> = emptyList(),
    val prices: List<PriceEntry> = emptyList(),
    val recipes: List<Recipe> = emptyList(),
    val ingredients: List<RecipeIngredient> = emptyList(),
    val meals: List<Meal> = emptyList(),
    val budgets: List<CutoffBudget> = emptyList(),
    val stores: List<Store> = emptyList(),
    val storePrices: List<StorePrice> = emptyList(),
)

private fun JSONObject.long(k: String): Long? = if (isNull(k)) null else getLong(k)

private fun <T> JSONObject.rows(k: String, f: (JSONObject) -> T): List<T> {
    val a = optJSONArray(k) ?: return emptyList() // an older backup may not have this table yet
    return List(a.length()) { f(a.getJSONObject(it)) }
}

private fun <T> JSONObject.put(k: String, rows: List<T>, f: (T) -> JSONObject) = put(k, JSONArray(rows.map(f)))

private fun obj(vararg pairs: Pair<String, Any?>) = JSONObject().also { o -> pairs.forEach { (k, v) -> o.put(k, v ?: JSONObject.NULL) } }

fun BackupData.toJson(): String = JSONObject().apply {
    put("app", "Pantri")
    put("format", BACKUP_FORMAT)
    put("createdAt", createdAt)
    put("settings", obj("darkMode" to darkMode, "haptics" to haptics, "defaultBudget" to defaultBudget))
    put("lists", lists) { obj("id" to it.id, "name" to it.name, "isTemplate" to it.isTemplate, "createdAt" to it.createdAt, "storeId" to it.storeId) }
    put("items", items) {
        obj(
            "id" to it.id, "listId" to it.listId, "name" to it.name, "quantity" to it.quantity, "unit" to it.unit, "category" to it.category,
            "note" to it.note, "priceCentavos" to it.priceCentavos, "checked" to it.checked, "checkedAt" to it.checkedAt,
            "sortOrder" to it.sortOrder, "createdAt" to it.createdAt,
        )
    }
    put("products", products) {
        obj(
            "key" to it.key, "name" to it.name, "category" to it.category, "unit" to it.unit, "unitPriceCentavos" to it.unitPriceCentavos,
            "timesBought" to it.timesBought, "favorite" to it.favorite, "lastUsedAt" to it.lastUsedAt,
        )
    }
    put("prices", prices) { obj("id" to it.id, "productKey" to it.productKey, "unitPriceCentavos" to it.unitPriceCentavos, "at" to it.at) }
    put("recipes", recipes) {
        obj("id" to it.id, "title" to it.title, "servings" to it.servings, "minutes" to it.minutes, "steps" to it.steps, "createdAt" to it.createdAt)
    }
    put("ingredients", ingredients) {
        obj(
            "id" to it.id, "recipeId" to it.recipeId, "name" to it.name, "quantity" to it.quantity, "unit" to it.unit,
            "category" to it.category, "sortOrder" to it.sortOrder,
        )
    }
    put("meals", meals) { obj("id" to it.id, "epochDay" to it.epochDay, "slot" to it.slot, "recipeId" to it.recipeId, "title" to it.title) }
    put("budgets", budgets) { obj("key" to it.key, "amountCentavos" to it.amountCentavos) }
    put("stores", stores) { obj("id" to it.id, "name" to it.name, "createdAt" to it.createdAt) }
    put("storePrices", storePrices) {
        obj(
            "id" to it.id, "productKey" to it.productKey, "storeId" to it.storeId, "priceCentavos" to it.priceCentavos,
            "quantity" to it.quantity, "unit" to it.unit, "at" to it.at,
        )
    }
}.toString()

/** Reads a backup file. Throws [BackupException] with a message fit to show the user when it is not a usable Pantri backup. */
fun parseBackup(text: String): BackupData {
    try {
        val o = JSONObject(text)
        if (o.optString("app") != "Pantri") throw BackupException("This file is not a Pantri backup.")
        if (o.getInt("format") > BACKUP_FORMAT) throw BackupException("This backup is from a newer version of Pantri. Update the app, then try again.")
        val s = o.optJSONObject("settings")
        return BackupData(
            createdAt = o.getLong("createdAt"),
            darkMode = s?.takeUnless { it.isNull("darkMode") }?.getBoolean("darkMode"),
            haptics = s?.takeUnless { it.isNull("haptics") }?.getBoolean("haptics"),
            defaultBudget = s?.long("defaultBudget"),
            lists = o.rows("lists") { GroceryList(it.getLong("id"), it.getString("name"), it.getBoolean("isTemplate"), it.getLong("createdAt"), it.long("storeId")) },
            items = o.rows("items") {
                Item(
                    it.getLong("id"), it.getLong("listId"), it.getString("name"), it.getDouble("quantity"), it.getString("unit"), it.getString("category"),
                    it.getString("note"), it.getLong("priceCentavos"), it.getBoolean("checked"), it.long("checkedAt"), it.getInt("sortOrder"), it.getLong("createdAt"),
                )
            },
            products = o.rows("products") {
                Product(
                    it.getString("key"), it.getString("name"), it.getString("category"), it.getString("unit"), it.getLong("unitPriceCentavos"),
                    it.getInt("timesBought"), it.getBoolean("favorite"), it.getLong("lastUsedAt"),
                )
            },
            prices = o.rows("prices") { PriceEntry(it.getLong("id"), it.getString("productKey"), it.getLong("unitPriceCentavos"), it.getLong("at")) },
            recipes = o.rows("recipes") { Recipe(it.getLong("id"), it.getString("title"), it.getInt("servings"), it.getInt("minutes"), it.getString("steps"), it.getLong("createdAt")) },
            ingredients = o.rows("ingredients") {
                RecipeIngredient(it.getLong("id"), it.getLong("recipeId"), it.getString("name"), it.getDouble("quantity"), it.getString("unit"), it.getString("category"), it.getInt("sortOrder"))
            },
            meals = o.rows("meals") { Meal(it.getLong("id"), it.getLong("epochDay"), it.getInt("slot"), it.long("recipeId"), it.getString("title")) },
            budgets = o.rows("budgets") { CutoffBudget(it.getString("key"), it.getLong("amountCentavos")) },
            stores = o.rows("stores") { Store(it.getLong("id"), it.getString("name"), it.getLong("createdAt")) },
            storePrices = o.rows("storePrices") {
                StorePrice(it.getLong("id"), it.getString("productKey"), it.getLong("storeId"), it.getLong("priceCentavos"), it.getDouble("quantity"), it.getString("unit"), it.getLong("at"))
            },
        )
    } catch (e: JSONException) {
        throw BackupException("This file is damaged or is not a Pantri backup.")
    }
}
