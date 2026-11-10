package com.pantri.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PantriDao {
    // Lists
    @Query(
        "SELECT l.*, COUNT(i.id) AS itemCount, COALESCE(SUM(CASE WHEN i.checked = 1 THEN 1 ELSE 0 END), 0) AS doneCount, " +
            "COALESCE(SUM(i.priceCentavos), 0) AS totalCentavos FROM lists l LEFT JOIN items i ON i.listId = l.id " +
            "WHERE l.isTemplate = :templates GROUP BY l.id ORDER BY l.createdAt DESC",
    )
    fun observeLists(templates: Boolean): Flow<List<ListSummary>>

    @Query("SELECT * FROM lists WHERE id = :id")
    fun observeList(id: Long): Flow<GroceryList?>

    @Query("SELECT * FROM lists WHERE id = :id")
    suspend fun getList(id: Long): GroceryList?

    @Insert suspend fun insertList(list: GroceryList): Long

    @Update suspend fun updateList(list: GroceryList)

    @Query("DELETE FROM lists WHERE id = :id") suspend fun deleteList(id: Long)

    // Items
    @Query("SELECT * FROM items WHERE listId = :listId ORDER BY sortOrder, id")
    fun observeItems(listId: Long): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE listId = :listId ORDER BY sortOrder, id")
    suspend fun getItems(listId: Long): List<Item>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getItem(id: Long): Item?

    @Insert suspend fun insertItem(item: Item): Long

    @Insert suspend fun insertItems(items: List<Item>)

    @Update suspend fun updateItem(item: Item)

    @Update suspend fun updateItems(items: List<Item>)

    @Delete suspend fun deleteItem(item: Item)

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM items WHERE listId = :listId")
    suspend fun maxSortOrder(listId: Long): Int

    // Budget numbers
    @Query("SELECT COALESCE(SUM(i.priceCentavos), 0) FROM items i JOIN lists l ON l.id = i.listId WHERE i.checked = 0 AND l.isTemplate = 0")
    fun observePlanned(): Flow<Long>

    @Query("SELECT checkedAt, priceCentavos, category FROM items WHERE checked = 1 AND checkedAt >= :from")
    fun observeCheckedSince(from: Long): Flow<List<CheckedRow>>

    @Query("SELECT * FROM items WHERE checked = 1 AND checkedAt BETWEEN :from AND :to ORDER BY checkedAt DESC")
    fun observePurchases(from: Long, to: Long): Flow<List<Item>>

    // Products and prices
    @Query("SELECT * FROM products ORDER BY favorite DESC, timesBought DESC, lastUsedAt DESC")
    fun observeProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE `key` = :key")
    suspend fun getProduct(key: String): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertProduct(product: Product)

    @Insert suspend fun insertPrice(entry: PriceEntry)

    @Query("SELECT * FROM prices WHERE productKey = :key ORDER BY at DESC LIMIT 8")
    fun observePrices(key: String): Flow<List<PriceEntry>>

    // Recipes
    @Query("SELECT * FROM recipes ORDER BY title COLLATE NOCASE")
    fun observeRecipes(): Flow<List<Recipe>>

    @Query("SELECT * FROM recipes WHERE id = :id")
    fun observeRecipe(id: Long): Flow<Recipe?>

    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getRecipe(id: Long): Recipe?

    @Query("SELECT * FROM recipe_ingredients WHERE recipeId = :recipeId ORDER BY sortOrder, id")
    fun observeIngredients(recipeId: Long): Flow<List<RecipeIngredient>>

    @Query("SELECT * FROM recipe_ingredients WHERE recipeId = :recipeId ORDER BY sortOrder, id")
    suspend fun getIngredients(recipeId: Long): List<RecipeIngredient>

    @Query("SELECT recipeId, COUNT(*) AS n FROM recipe_ingredients GROUP BY recipeId")
    fun observeIngredientCounts(): Flow<List<IngredientCount>>

    @Insert suspend fun insertRecipe(recipe: Recipe): Long

    @Update suspend fun updateRecipe(recipe: Recipe)

    @Query("DELETE FROM recipes WHERE id = :id") suspend fun deleteRecipe(id: Long)

    @Insert suspend fun insertIngredients(items: List<RecipeIngredient>)

    @Query("DELETE FROM recipe_ingredients WHERE recipeId = :recipeId") suspend fun clearIngredients(recipeId: Long)

    // Meal plan
    @Query("SELECT * FROM meals WHERE epochDay BETWEEN :from AND :to ORDER BY epochDay, slot, id")
    fun observeMeals(from: Long, to: Long): Flow<List<Meal>>

    @Query("SELECT * FROM meals WHERE epochDay BETWEEN :from AND :to ORDER BY epochDay, slot, id")
    suspend fun getMeals(from: Long, to: Long): List<Meal>

    @Insert suspend fun insertMeal(meal: Meal): Long

    @Query("DELETE FROM meals WHERE id = :id") suspend fun deleteMeal(id: Long)

    // Budgets
    @Query("SELECT * FROM cutoff_budgets WHERE `key` = :key")
    fun observeBudget(key: String): Flow<CutoffBudget?>

    @Query("SELECT * FROM cutoff_budgets WHERE `key` = :key")
    suspend fun getBudget(key: String): CutoffBudget?

    @Query("SELECT * FROM cutoff_budgets")
    fun observeBudgets(): Flow<List<CutoffBudget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertBudget(budget: CutoffBudget)
}

data class IngredientCount(val recipeId: Long, val n: Int)
