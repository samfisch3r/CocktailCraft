package com.cocktailcraft.android.domain.model

import com.cocktailcraft.android.data.local.entity.*
import kotlinx.serialization.Serializable

@Serializable
data class BarBackup(
    val ingredients: List<IngredientEntity>,
    val bottles: List<BottleStockEntity>,
    val recipes: List<CocktailRecipeEntity>,
    val ingredientRefs: List<RecipeIngredientCrossRefEntity>,
    val ratings: List<RecipeVersionHistoryEntity>
)
