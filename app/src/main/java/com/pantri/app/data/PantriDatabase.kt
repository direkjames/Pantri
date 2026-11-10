package com.pantri.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        GroceryList::class, Item::class, Product::class, PriceEntry::class, Recipe::class, RecipeIngredient::class,
        Meal::class, CutoffBudget::class, Store::class, StorePrice::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class PantriDatabase : RoomDatabase() {
    abstract fun dao(): PantriDao

    companion object {
        /** v2: stores, per-store prices and a store on each list. Adds only; nothing existing is touched. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `lists` ADD COLUMN `storeId` INTEGER")
                db.execSQL("CREATE TABLE IF NOT EXISTS `stores` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `store_prices` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `productKey` TEXT NOT NULL, " +
                        "`storeId` INTEGER NOT NULL, `priceCentavos` INTEGER NOT NULL, `quantity` REAL NOT NULL, `unit` TEXT NOT NULL, `at` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`storeId`) REFERENCES `stores`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_store_prices_productKey_storeId` ON `store_prices` (`productKey`, `storeId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_store_prices_storeId` ON `store_prices` (`storeId`)")
            }
        }

        fun create(context: Context): PantriDatabase =
            Room.databaseBuilder(context.applicationContext, PantriDatabase::class.java, "pantri.db").addMigrations(MIGRATION_1_2).build()
    }
}
