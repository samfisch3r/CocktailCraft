package com.cocktailcraft.android.data.repository

import com.cocktailcraft.android.data.local.dao.CocktailDao
import com.cocktailcraft.android.data.local.entity.*
import com.cocktailcraft.android.domain.repository.CocktailRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CocktailRepositoryImpl @Inject constructor(
    private val cocktailDao: CocktailDao
) : CocktailRepository {

    override fun getAllIngredients(): Flow<List<IngredientEntity>> {
        return cocktailDao.getAllIngredients()
    }

    override suspend fun addIngredient(ingredient: IngredientEntity): Long {
        return cocktailDao.insertIngredient(ingredient)
    }

    override suspend fun getIngredientById(id: Long): IngredientEntity? {
        return cocktailDao.getIngredientById(id)
    }

    override suspend fun renameOrMergeIngredient(ingredient: IngredientEntity, newName: String) {
        val existing = cocktailDao.getIngredientByName(newName)
        if (existing != null && existing.id != ingredient.id) {
            // MERGE: Move all relationships to the existing one and delete the typo/duplicate
            cocktailDao.updateBottleIngredientId(ingredient.id, existing.id)
            cocktailDao.updateRecipeIngredientId(ingredient.id, existing.id)
            cocktailDao.deleteIngredient(ingredient)
        } else {
            // RENAME: Just update the name
            cocktailDao.updateIngredient(ingredient.copy(name = newName))
        }
    }

    override suspend fun deleteIngredient(ingredient: IngredientEntity) {
        cocktailDao.deleteIngredient(ingredient)
    }

    override suspend fun getIngredientUsageCount(ingredientId: Long): Int {
        return cocktailDao.getIngredientUsageCount(ingredientId)
    }

    override suspend fun pruneUnusedIngredients() {
        cocktailDao.deleteUnusedIngredients()
    }

    override fun getAllBottles(): Flow<List<BottleItem>> {
        return cocktailDao.getAllBottles()
    }

    override suspend fun addBottle(bottle: BottleStockEntity): Long {
        return cocktailDao.insertBottle(bottle)
    }

    override suspend fun updateBottle(bottle: BottleStockEntity) {
        cocktailDao.updateBottle(bottle)
    }

    override suspend fun getBottleById(id: Long): BottleStockEntity? {
        return cocktailDao.getBottleById(id)
    }

    override suspend fun deleteBottle(bottleId: Long) {
        cocktailDao.deleteBottle(bottleId)
    }

    override suspend fun getBottlesForIngredient(ingredientId: Long): List<BottleStockEntity> {
        return cocktailDao.getBottlesForIngredient(ingredientId)
    }

    override fun getRecipesMatchingBottle(bottleId: Long, currentTime: Long): Flow<List<RecipeWithMissingCount>> {
        return cocktailDao.getRecipesMatchingBottleWithMissingCount(bottleId, currentTime)
    }

    override fun getAvailableRecipes(currentTime: Long): Flow<List<RecipeWithRating>> {
        return cocktailDao.getAvailableRecipesWithRating(currentTime)
    }

    override fun getUnratedRecipes(): Flow<List<RecipeWithRating>> {
        return cocktailDao.getUnratedRecipesWithRating()
    }

    override fun getAllRecipesWithMissingCount(currentTime: Long): Flow<List<RecipeWithMissingCount>> {
        return cocktailDao.getAllRecipesWithMissingCount(currentTime)
    }

    override suspend fun getRecipeById(recipeId: Long): CocktailRecipeEntity? {
        return cocktailDao.getRecipeById(recipeId)
    }

    override fun getRecipeByIdFlow(recipeId: Long): Flow<CocktailRecipeEntity?> {
        return cocktailDao.getRecipeByIdFlow(recipeId)
    }

    override suspend fun deleteRecipe(recipe: CocktailRecipeEntity) {
        cocktailDao.deleteRecipe(recipe)
    }

    override suspend fun getIngredientsForRecipe(recipeId: Long, currentTime: Long): List<RecipeIngredient> {
        return cocktailDao.getDetailedIngredientsForRecipe(recipeId, currentTime)
    }

    override fun getIngredientsForRecipeFlow(recipeId: Long, currentTime: Long): Flow<List<RecipeIngredient>> {
        return cocktailDao.getDetailedIngredientsForRecipeFlow(recipeId, currentTime)
    }

    override suspend fun saveRecipe(
        recipe: CocktailRecipeEntity,
        ingredients: List<RecipeIngredientCrossRefEntity>
    ) {
        val recipeId = cocktailDao.insertRecipe(recipe)
        // Clear old ingredients if editing
        cocktailDao.deleteIngredientsForRecipe(recipeId)
        ingredients.forEach { 
            cocktailDao.insertRecipeIngredient(it.copy(recipeId = recipeId))
        }
    }

    override fun getVersionHistory(recipeId: Long): Flow<List<RecipeVersionHistoryEntity>> {
        return cocktailDao.getVersionHistory(recipeId)
    }

    override suspend fun addVersion(version: RecipeVersionHistoryEntity) {
        cocktailDao.insertVersionHistory(version)
    }

    override suspend fun getFullBackup(): com.cocktailcraft.android.domain.model.BarBackup {
        return com.cocktailcraft.android.domain.model.BarBackup(
            ingredients = cocktailDao.getAllIngredientsSync(),
            bottles = cocktailDao.getAllBottlesSync(),
            recipes = cocktailDao.getAllRecipesSync(),
            ingredientRefs = cocktailDao.getAllIngredientRefsSync(),
            ratings = cocktailDao.getAllRatingsSync()
        )
    }

    override suspend fun restoreBackup(backup: com.cocktailcraft.android.domain.model.BarBackup) {
        cocktailDao.clearAllData()
        cocktailDao.insertIngredients(backup.ingredients)
        cocktailDao.insertBottles(backup.bottles)
        cocktailDao.insertRecipes(backup.recipes)
        cocktailDao.insertRecipeIngredients(backup.ingredientRefs)
        cocktailDao.insertRatings(backup.ratings)
    }
}
