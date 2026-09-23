package com.cocktailcraft.android.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.cocktailcraft.android.data.local.entity.*
import com.cocktailcraft.android.domain.repository.CocktailRepository
import com.cocktailcraft.android.ui.navigation.Destination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class RecipeDetailUiState(
    val recipe: CocktailRecipeEntity? = null,
    val ingredients: List<RecipeIngredient> = emptyList(),
    val ratings: List<RecipeVersionHistoryEntity> = emptyList(),
    val isLoading: Boolean = true,
    val selectedRating: RecipeVersionHistoryEntity? = null,
    val selectedRatingIngredients: List<IngredientSnapshot> = emptyList(),
    val matchingBottles: List<BottleStockEntity> = emptyList(),
    val selectedIngredientName: String? = null
)

@HiltViewModel
class RecipeDetailViewModel @Inject constructor(
    private val repository: CocktailRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val recipeId: Long = savedStateHandle.toRoute<Destination.RecipeDetail>().recipeId

    private val _uiState = MutableStateFlow(RecipeDetailUiState())
    val uiState: StateFlow<RecipeDetailUiState> = _uiState.asStateFlow()

    init {
        loadRecipeData()
    }

    private fun loadRecipeData() {
        val currentTime = System.currentTimeMillis()

        combine(
            repository.getRecipeByIdFlow(recipeId),
            repository.getIngredientsForRecipeFlow(recipeId, currentTime),
            repository.getVersionHistory(recipeId)
        ) { recipe, ingredients, ratings ->
            Triple(recipe, ingredients, ratings)
        }.onEach { (recipe, ingredients, ratings) ->
            _uiState.update { 
                it.copy(
                    recipe = recipe,
                    ingredients = ingredients,
                    ratings = ratings,
                    isLoading = false
                )
            }
        }.launchIn(viewModelScope)
    }

    fun loadMatchingBottles(ingredientId: Long, ingredientName: String) {
        viewModelScope.launch {
            val bottles = repository.getBottlesForIngredient(ingredientId)
            _uiState.update { 
                it.copy(
                    matchingBottles = bottles,
                    selectedIngredientName = ingredientName
                )
            }
        }
    }

    fun clearMatchingBottles() {
        _uiState.update { 
            it.copy(
                matchingBottles = emptyList(),
                selectedIngredientName = null
            )
        }
    }

    fun addRating(rating: Float, notes: String?) {
        val currentState = _uiState.value
        val currentRecipe = currentState.recipe ?: return
        
        viewModelScope.launch {
            val snapshots = currentState.ingredients.map { 
                IngredientSnapshot(
                    ingredientId = it.ingredientId,
                    ingredientName = it.ingredientName,
                    amount = it.amount,
                    unit = it.unit,
                    assignedBottleId = it.assignedBottleId,
                    assignedBottleName = it.assignedBottleName
                )
            }
            
            val nextRatingNumber = (currentState.ratings.maxOfOrNull { it.versionNumber } ?: 0) + 1
            val ratingEntry = RecipeVersionHistoryEntity(
                recipeId = recipeId,
                versionNumber = nextRatingNumber,
                timestamp = System.currentTimeMillis(),
                rating = rating,
                tweakNotes = notes,
                instructions = currentRecipe.instructions,
                glassType = currentRecipe.glassType,
                ingredientsJson = Json.encodeToString(snapshots)
            )
            repository.addVersion(ratingEntry)
        }
    }

    fun selectRating(rating: RecipeVersionHistoryEntity?) {
        val ingredients = if (rating != null) {
            try {
                Json.decodeFromString<List<IngredientSnapshot>>(rating.ingredientsJson)
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
        
        _uiState.update { it.copy(selectedRating = rating, selectedRatingIngredients = ingredients) }
    }

    fun restoreRating(rating: RecipeVersionHistoryEntity) {
        val currentRecipe = _uiState.value.recipe ?: return
        viewModelScope.launch {
            val restoredRecipe = currentRecipe.copy(
                instructions = rating.instructions,
                glassType = rating.glassType
            )
            
            val snapshots: List<IngredientSnapshot> = try {
                Json.decodeFromString(rating.ingredientsJson)
            } catch (_: Exception) {
                emptyList()
            }
            
            val ingredientRefs = snapshots.map { 
                RecipeIngredientCrossRefEntity(
                    recipeId = recipeId,
                    ingredientId = it.ingredientId,
                    amount = it.amount,
                    unit = it.unit,
                    assignedBottleId = it.assignedBottleId
                )
            }
            
            repository.saveRecipe(restoredRecipe, ingredientRefs)
            selectRating(null)
            loadRecipeData() // Refresh UI
        }
    }

    fun deleteRecipe(onSuccess: () -> Unit) {
        val recipe = _uiState.value.recipe ?: return
        viewModelScope.launch {
            repository.deleteRecipe(recipe)
            onSuccess()
        }
    }
}
