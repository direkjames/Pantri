package com.pantri.app.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A shopping list, or a template when [isTemplate] is true. Money is always stored in centavos. */
@Entity(tableName = "lists")
data class GroceryList(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isTemplate: Boolean = false,
    val createdAt: Long,
    /** The store you plan to shop at for this list; null for none. Cleared in code when the store is deleted. */
    val storeId: Long? = null,
)

@Entity(tableName = "stores")
data class Store(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

/** The latest price of one product at one store: [priceCentavos] pays for [quantity] [unit] (unit may be empty). */
@Entity(
    tableName = "store_prices",
    foreignKeys = [ForeignKey(Store::class, ["id"], ["storeId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("productKey", "storeId", unique = true), Index("storeId")],
)
data class StorePrice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productKey: String,
    val storeId: Long,
    val priceCentavos: Long,
    val quantity: Double = 1.0,
    val unit: String = "",
    val at: Long,
)

@Entity(
    tableName = "items",
    foreignKeys = [ForeignKey(GroceryList::class, ["id"], ["listId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("listId")],
)
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,
    val name: String,
    val quantity: Double = 1.0,
    val unit: String = "",
    val category: String = Aisle.Other.name,
    val note: String = "",
    /** Price of the whole line (what you pay for this quantity). */
    val priceCentavos: Long = 0,
    val checked: Boolean = false,
    val checkedAt: Long? = null,
    val sortOrder: Int = 0,
    val createdAt: Long,
)

/** One row per product ever added: powers autocomplete, favourites and buy again. [key] is the lower-case name. */
@Entity(tableName = "products")
data class Product(
    @PrimaryKey val key: String,
    val name: String,
    val category: String,
    val unit: String,
    val unitPriceCentavos: Long,
    val timesBought: Int,
    val favorite: Boolean,
    val lastUsedAt: Long,
)

@Entity(tableName = "prices", indices = [Index("productKey")])
data class PriceEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productKey: String,
    val unitPriceCentavos: Long,
    val at: Long,
)

@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val servings: Int = 4,
    val minutes: Int = 0,
    val steps: String = "",
    val createdAt: Long,
)

@Entity(
    tableName = "recipe_ingredients",
    foreignKeys = [ForeignKey(Recipe::class, ["id"], ["recipeId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("recipeId")],
)
data class RecipeIngredient(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: Long,
    val name: String,
    val quantity: Double = 1.0,
    val unit: String = "",
    val category: String = Aisle.Other.name,
    val sortOrder: Int = 0,
)

/** One meal on one day. [title] is kept so the plan survives a deleted recipe. */
@Entity(
    tableName = "meals",
    foreignKeys = [ForeignKey(Recipe::class, ["id"], ["recipeId"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index("recipeId"), Index("epochDay")],
)
data class Meal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val slot: Int,
    val recipeId: Long?,
    val title: String,
)

/** Budget for one cutoff, e.g. key "2026-11-A" (1st to 15th) or "2026-11-B" (16th to month end). */
@Entity(tableName = "cutoff_budgets")
data class CutoffBudget(
    @PrimaryKey val key: String,
    val amountCentavos: Long,
)

data class ListSummary(
    @Embedded val list: GroceryList,
    val itemCount: Int,
    val doneCount: Int,
    val totalCentavos: Long,
)

data class CheckedRow(
    @ColumnInfo(name = "checkedAt") val checkedAt: Long,
    @ColumnInfo(name = "priceCentavos") val priceCentavos: Long,
    @ColumnInfo(name = "category") val category: String,
)
