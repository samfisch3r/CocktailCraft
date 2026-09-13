package com.cocktailcraft.android.domain.repository

import com.cocktailcraft.android.data.local.entity.*
import kotlinx.coroutines.flow.Flow

interface CocktailRepository {
    // Ingredients
    fun getAllIngredients(): Flow<List<IngredientEntity>>
    suspend fun addIngredient(ingredient: IngredientEntity): Long
    suspend fun getIngredientById(id: Long): IngredientEntity?
    suspend fun renameOrMergeIngredient(ingredient: IngredientEntity, newName: String)
    suspend fun deleteIngredient(ingredient: IngredientEntity)
    suspend fun getIngredientUsageCount(ingredientId: Long): Int
    suspend fun pruneUnusedIngredients()

    // Bottles
    fun getAllBottles(): Flow<List<BottleItem>>
    suspend fun addBottle(bottle: BottleStockEntity): Long
    suspend fun updateBottle(bottle: BottleStockEntity)
    suspend fun getBottleById(id: Long): BottleStockEntity?
    suspend fun deleteBottle(bottleId: Long)
    suspend fun getBottlesForIngredient(ingredientId: Long): List<BottleStockEntity>
    
    // Recipes
    fun getRecipesMatchingBottle(bottleId: Long, currentTime: Long): Flow<List<RecipeWithMissingCount>>
    fun getAvailableRecipes(currentTime: Long): Flow<List<RecipeWithRating>>
    fun getUnratedRecipes(): Flow<List<RecipeWithRating>>
    fun getAllRecipesWithMissingCount(currentTime: Long): Flow<List<RecipeWithMissingCount>>
    suspend fun getRecipeById(recipeId: Long): CocktailRecipeEntity?
    suspend fun deleteRecipe(recipe: CocktailRecipeEntity)
    suspend fun getIngredientsForRecipe(recipeId: Long, currentTime: Long): List<RecipeIngredient>
    suspend fun saveRecipe(recipe: CocktailRecipeEntity, ingredients: List<RecipeIngredientCrossRefEntity>)
    
    // Backup
    suspend fun getFullBackup(): com.cocktailcraft.android.domain.model.BarBackup
    suspend fun restoreBackup(backup: com.cocktailcraft.android.domain.model.BarBackup)

    // History
    fun getVersionHistory(recipeId: Long): Flow<List<RecipeVersionHistoryEntity>>
    suspend fun addVersion(version: RecipeVersionHistoryEntity)
}

interface NetworkRecipeRepository {
    suspend fun searchRecipes(name: String): List<RemoteRecipe>
}

data class RemoteRecipe(
    val name: String,
    val instructions: String,
    val imageUri: String?,
    val glassType: String,
    val ingredients: List<RemoteIngredient>
)

data class RemoteIngredient(
    val name: String,
    val amount: Double,
    val unit: IngredientUnit
)
