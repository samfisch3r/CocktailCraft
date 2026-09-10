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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class RecipeDetailUiState(
    val recipe: CocktailRecipeEntity? = null,
    val ingredients: List<RecipeIngredient> = emptyList(),
    val versions: List<RecipeVersionHistoryEntity> = emptyList(),
    val isLoading: Boolean = true,
    val selectedVersion: RecipeVersionHistoryEntity? = null,
    val selectedVersionIngredients: List<IngredientSnapshot> = emptyList(),
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
        viewModelScope.launch {
            val currentTime = System.currentTimeMillis()
            val recipe = repository.getRecipeById(recipeId)
            val ingredients = repository.getIngredientsForRecipe(recipeId, currentTime)
            
            _uiState.update { 
                it.copy(
                    recipe = recipe,
                    ingredients = ingredients,
                    isLoading = false
                )
            }
        }
        
        repository.getVersionHistory(recipeId)
            .onEach { versions ->
                _uiState.update { it.copy(versions = versions) }
            }
            .launchIn(viewModelScope)
    }

    fun loadMatchingBottles(ingredientId: Long, ingredientName: String) {
        viewModelScope.launch {
            val bottles = repository.getBottlesForIngredient(ingredientId, System.currentTimeMillis())
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

    fun addTweak(rating: Float, notes: String?) {
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
            
            val nextVersion = (currentState.versions.maxOfOrNull { it.versionNumber } ?: 0) + 1
            val version = RecipeVersionHistoryEntity(
                recipeId = recipeId,
                versionNumber = nextVersion,
                timestamp = System.currentTimeMillis(),
                rating = rating,
                tweakNotes = notes,
                instructions = currentRecipe.instructions,
                glassType = currentRecipe.glassType,
                ingredientsJson = Json.encodeToString(snapshots)
            )
            repository.addVersion(version)
        }
    }

    fun selectVersion(version: RecipeVersionHistoryEntity?) {
        val ingredients = if (version != null) {
            try {
                Json.decodeFromString<List<IngredientSnapshot>>(version.ingredientsJson)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
        
        _uiState.update { it.copy(selectedVersion = version, selectedVersionIngredients = ingredients) }
    }

    fun restoreVersion(version: RecipeVersionHistoryEntity) {
        val currentRecipe = _uiState.value.recipe ?: return
        viewModelScope.launch {
            val restoredRecipe = currentRecipe.copy(
                instructions = version.instructions,
                glassType = version.glassType
            )
            
            val snapshots: List<IngredientSnapshot> = try {
                Json.decodeFromString(version.ingredientsJson)
            } catch (e: Exception) {
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
            selectVersion(null)
            loadRecipeData() // Refresh UI
        }
    }

    fun deleteRecipe(onSuccess: () -> Unit) {
        val recipe = _uiState.value.recipe ?: return
        viewModelScope.launch {
            repository.deleteRecipe(recipe)
            repository.pruneUnusedIngredients()
            onSuccess()
        }
    }
}
