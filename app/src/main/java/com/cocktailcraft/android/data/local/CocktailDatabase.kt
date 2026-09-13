package com.cocktailcraft.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.cocktailcraft.android.data.local.dao.CocktailDao
import com.cocktailcraft.android.data.local.entity.*

@Database(
    entities = [
        IngredientEntity::class,
        BottleStockEntity::class,
        CocktailRecipeEntity::class,
        RecipeIngredientCrossRefEntity::class,
        RecipeVersionHistoryEntity::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class CocktailDatabase : RoomDatabase() {
    abstract fun cocktailDao(): CocktailDao
}
