package com.cocktailcraft.android.data.local.dao

import androidx.room.*
import com.cocktailcraft.android.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CocktailDao {
    // --- Ingredients ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredient(ingredient: IngredientEntity): Long

    @Query("SELECT * FROM ingredient ORDER BY name ASC")
    fun getAllIngredients(): Flow<List<IngredientEntity>>

    @Query("SELECT * FROM ingredient WHERE id = :id")
    suspend fun getIngredientById(id: Long): IngredientEntity?

    @Query("SELECT * FROM ingredient WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getIngredientByName(name: String): IngredientEntity?

    @Update
    suspend fun updateIngredient(ingredient: IngredientEntity)

    @Delete
    suspend fun deleteIngredient(ingredient: IngredientEntity)

    @Query("UPDATE bottle_stock SET ingredientId = :targetId WHERE ingredientId = :oldId")
    suspend fun updateBottleIngredientId(oldId: Long, targetId: Long)

    @Query("UPDATE recipe_ingredient_xref SET ingredientId = :targetId WHERE ingredientId = :oldId")
    suspend fun updateRecipeIngredientId(oldId: Long, targetId: Long)

    @Query("""
        SELECT (
            SELECT COUNT(*) FROM bottle_stock WHERE ingredientId = :ingredientId
        ) + (
            SELECT COUNT(*) FROM recipe_ingredient_xref WHERE ingredientId = :ingredientId
        )
    """)
    suspend fun getIngredientUsageCount(ingredientId: Long): Int

    @Query("""
        DELETE FROM ingredient 
        WHERE id NOT IN (SELECT ingredientId FROM bottle_stock)
        AND id NOT IN (SELECT ingredientId FROM recipe_ingredient_xref)
    """)
    suspend fun deleteUnusedIngredients()

    // --- Bottles ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBottle(bottle: BottleStockEntity): Long

    @Update
    suspend fun updateBottle(bottle: BottleStockEntity)

    @Query("SELECT * FROM bottle_stock WHERE id = :id")
    suspend fun getBottleById(id: Long): BottleStockEntity?

    @Query("DELETE FROM bottle_stock WHERE id = :bottleId")
    suspend fun deleteBottle(bottleId: Long)

    @Query("""
        SELECT 
            b.id, 
            b.name as brandName, 
            i.name as ingredientName, 
            b.notes,
            b.expiresAt,
            b.imageUri,
            b.inStock
        FROM bottle_stock b
        JOIN ingredient i ON b.ingredientId = i.id
        ORDER BY i.name ASC, b.name ASC
    """)
    fun getAllBottles(): Flow<List<BottleItem>>

    @Query("""
        SELECT b.* FROM bottle_stock b
        JOIN ingredient i ON b.ingredientId = i.id
        WHERE i.id = :ingredientId
    """)
    suspend fun getBottlesForIngredient(ingredientId: Long): List<BottleStockEntity>

    // --- Recipes ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: CocktailRecipeEntity): Long

    @Query("SELECT * FROM cocktail_recipe WHERE id = :recipeId")
    suspend fun getRecipeById(recipeId: Long): CocktailRecipeEntity?

    @Delete
    suspend fun deleteRecipe(recipe: CocktailRecipeEntity)

    @Query("DELETE FROM recipe_ingredient_xref WHERE recipeId = :recipeId")
    suspend fun deleteIngredientsForRecipe(recipeId: Long)

    @Query("""
        SELECT 
            i.id as ingredientId, 
            i.name as ingredientName, 
            xref.amount, 
            xref.unit,
            xref.assignedBottleId,
            xref.preferredBrand,
            b_assigned.name as assignedBottleName,
            CASE 
                WHEN xref.assignedBottleId IS NOT NULL THEN 
                    (SELECT COUNT(*) FROM bottle_stock WHERE id = xref.assignedBottleId AND inStock = 1 AND (expiresAt IS NULL OR expiresAt > :currentTime)) > 0
                WHEN xref.preferredBrand IS NOT NULL THEN 
                    (SELECT COUNT(*) FROM bottle_stock WHERE ingredientId = i.id AND LOWER(name) = LOWER(xref.preferredBrand) AND inStock = 1 AND (expiresAt IS NULL OR expiresAt > :currentTime)) > 0
                ELSE 
                    (SELECT COUNT(*) FROM bottle_stock WHERE ingredientId = i.id AND inStock = 1 AND (expiresAt IS NULL OR expiresAt > :currentTime)) > 0
            END as isAvailable,
            (SELECT GROUP_CONCAT(name, ', ') FROM bottle_stock WHERE ingredientId = i.id AND inStock = 1 AND (expiresAt IS NULL OR expiresAt > :currentTime)) as bottleNames
        FROM recipe_ingredient_xref AS xref
        JOIN ingredient AS i ON xref.ingredientId = i.id
        LEFT JOIN bottle_stock AS b_assigned ON xref.assignedBottleId = b_assigned.id
        WHERE xref.recipeId = :recipeId
    """)
    suspend fun getDetailedIngredientsForRecipe(recipeId: Long, currentTime: Long): List<RecipeIngredient>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipeIngredient(crossRef: RecipeIngredientCrossRefEntity)

    @Transaction
    @Query("""
        SELECT 
            r.*,
            (
                SELECT COUNT(*) FROM recipe_ingredient_xref xref
                JOIN ingredient i ON xref.ingredientId = i.id
                WHERE xref.recipeId = r.id 
                AND (
                    (assignedBottleId IS NULL AND preferredBrand IS NULL AND xref.ingredientId NOT IN (
                        SELECT ingredientId FROM bottle_stock 
                        WHERE inStock = 1 AND (expiresAt IS NULL OR expiresAt > :currentTime)
                    ))
                    OR
                    (assignedBottleId IS NOT NULL AND assignedBottleId NOT IN (
                        SELECT id FROM bottle_stock 
                        WHERE inStock = 1 AND (expiresAt IS NULL OR expiresAt > :currentTime)
                    ))
                    OR
                    (preferredBrand IS NOT NULL AND assignedBottleId IS NULL AND xref.ingredientId NOT IN (
                        SELECT ingredientId FROM bottle_stock 
                        WHERE LOWER(name) = LOWER(xref.preferredBrand) AND inStock = 1 AND (expiresAt IS NULL OR expiresAt > :currentTime)
                    ))
                )
            ) as missingCount,
            (SELECT AVG(rating) FROM recipe_version_history WHERE recipeId = r.id) as averageRating
        FROM cocktail_recipe r
        JOIN recipe_ingredient_xref xref ON r.id = xref.recipeId
        JOIN bottle_stock b ON xref.ingredientId = b.ingredientId
        WHERE b.id = :bottleId AND b.inStock = 1 AND (b.expiresAt IS NULL OR b.expiresAt > :currentTime)
        ORDER BY missingCount ASC, name ASC
    """)
    fun getRecipesMatchingBottleWithMissingCount(bottleId: Long, currentTime: Long): Flow<List<RecipeWithMissingCount>>

    @Query("""
        SELECT 
            r.*,
            (SELECT AVG(rating) FROM recipe_version_history WHERE recipeId = r.id) as averageRating
        FROM cocktail_recipe r
        WHERE id NOT IN (
            SELECT recipeId FROM recipe_ingredient_xref xref
            JOIN ingredient i ON xref.ingredientId = i.id
            WHERE 
                (assignedBottleId IS NULL AND preferredBrand IS NULL AND xref.ingredientId NOT IN (SELECT ingredientId FROM bottle_stock WHERE inStock = 1 AND (expiresAt IS NULL OR expiresAt > :currentTime)))
                OR
                (assignedBottleId IS NOT NULL AND assignedBottleId NOT IN (SELECT id FROM bottle_stock WHERE inStock = 1 AND (expiresAt IS NULL OR expiresAt > :currentTime)))
                OR
                (preferredBrand IS NOT NULL AND assignedBottleId IS NULL AND xref.ingredientId NOT IN (SELECT ingredientId FROM bottle_stock WHERE LOWER(name) = LOWER(xref.preferredBrand) AND inStock = 1 AND (expiresAt IS NULL OR expiresAt > :currentTime)))
        )
    """)
    fun getAvailableRecipesWithRating(currentTime: Long): Flow<List<RecipeWithRating>>

    @Query("""
        SELECT 
            r.*,
            (SELECT AVG(rating) FROM recipe_version_history WHERE recipeId = r.id) as averageRating
        FROM cocktail_recipe r
        WHERE id NOT IN (SELECT DISTINCT recipeId FROM recipe_version_history)
        ORDER BY id DESC
    """)
    fun getUnratedRecipesWithRating(): Flow<List<RecipeWithRating>>

    @Query("""
        SELECT 
            r.*,
            (
                SELECT COUNT(*) FROM recipe_ingredient_xref xref
                JOIN ingredient i ON xref.ingredientId = i.id
                WHERE xref.recipeId = r.id 
                AND (
                    (assignedBottleId IS NULL AND preferredBrand IS NULL AND xref.ingredientId NOT IN (
                        SELECT ingredientId FROM bottle_stock 
                        WHERE inStock = 1 AND (expiresAt IS NULL OR expiresAt > :currentTime)
                    ))
                    OR
                    (assignedBottleId IS NOT NULL AND assignedBottleId NOT IN (
                        SELECT id FROM bottle_stock 
                        WHERE inStock = 1 AND (expiresAt IS NULL OR expiresAt > :currentTime)
                    ))
                    OR
                    (preferredBrand IS NOT NULL AND assignedBottleId IS NULL AND xref.ingredientId NOT IN (
                        SELECT ingredientId FROM bottle_stock 
                        WHERE LOWER(name) = LOWER(xref.preferredBrand) AND inStock = 1 AND (expiresAt IS NULL OR expiresAt > :currentTime)
                    ))
                )
            ) as missingCount,
            (SELECT AVG(rating) FROM recipe_version_history WHERE recipeId = r.id) as averageRating
        FROM cocktail_recipe r
        ORDER BY missingCount ASC, name ASC
    """)
    fun getAllRecipesWithMissingCount(currentTime: Long): Flow<List<RecipeWithMissingCount>>

    @Query("SELECT * FROM cocktail_recipe")
    suspend fun getAllRecipesSync(): List<CocktailRecipeEntity>

    @Query("SELECT * FROM recipe_ingredient_xref")
    suspend fun getAllIngredientRefsSync(): List<RecipeIngredientCrossRefEntity>

    @Query("SELECT * FROM recipe_version_history")
    suspend fun getAllRatingsSync(): List<RecipeVersionHistoryEntity>

    @Query("SELECT * FROM bottle_stock")
    suspend fun getAllBottlesSync(): List<BottleStockEntity>

    @Query("SELECT * FROM ingredient")
    suspend fun getAllIngredientsSync(): List<IngredientEntity>

    @Transaction
    suspend fun clearAllData() {
        deleteAllBottles()
        deleteAllRecipeIngredients()
        deleteAllRecipes()
        deleteAllIngredients()
        deleteAllRatings()
    }

    @Query("DELETE FROM bottle_stock")
    suspend fun deleteAllBottles()

    @Query("DELETE FROM recipe_ingredient_xref")
    suspend fun deleteAllRecipeIngredients()

    @Query("DELETE FROM cocktail_recipe")
    suspend fun deleteAllRecipes()

    @Query("DELETE FROM ingredient")
    suspend fun deleteAllIngredients()

    @Query("DELETE FROM recipe_version_history")
    suspend fun deleteAllRatings()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredients(ingredients: List<IngredientEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBottles(bottles: List<BottleStockEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipes(recipes: List<CocktailRecipeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipeIngredients(refs: List<RecipeIngredientCrossRefEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRatings(ratings: List<RecipeVersionHistoryEntity>)

    // --- Version History ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersionHistory(version: RecipeVersionHistoryEntity)

    @Query("SELECT * FROM recipe_version_history WHERE recipeId = :recipeId ORDER BY versionNumber DESC")
    fun getVersionHistory(recipeId: Long): Flow<List<RecipeVersionHistoryEntity>>
}
