package com.cocktailcraft.android.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Destination {
    @Serializable
    data object Dashboard : Destination
    
    @Serializable
    data class RecipeDetail(val recipeId: Long) : Destination
    
    @Serializable
    data class AddEditRecipe(val recipeId: Long? = null) : Destination
    
    @Serializable
    data object BottleInventory : Destination

    @Serializable
    data object RecipeLibrary : Destination

    @Serializable
    data class AddBottle(val bottleId: Long? = null) : Destination
}
