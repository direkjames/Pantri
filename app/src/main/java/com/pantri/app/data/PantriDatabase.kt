package com.pantri.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [GroceryList::class, Item::class, Product::class, PriceEntry::class, Recipe::class, RecipeIngredient::class, Meal::class, CutoffBudget::class],
    version = 1,
    exportSchema = true,
)
abstract class PantriDatabase : RoomDatabase() {
    abstract fun dao(): PantriDao

    companion object {
        fun create(context: Context): PantriDatabase =
            Room.databaseBuilder(context.applicationContext, PantriDatabase::class.java, "pantri.db").build()
    }
}
