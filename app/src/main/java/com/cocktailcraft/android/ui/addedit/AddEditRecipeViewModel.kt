package com.cocktailcraft.android.ui.addedit

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.cocktailcraft.android.data.local.entity.*
import com.cocktailcraft.android.domain.repository.CocktailRepository
import com.cocktailcraft.android.domain.repository.NetworkRecipeRepository
import com.cocktailcraft.android.domain.repository.RemoteRecipe
import com.cocktailcraft.android.ui.navigation.Destination
import com.cocktailcraft.android.util.FilePersistenceHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class IngredientInputState(
    val ingredient: IngredientEntity? = null,
    val pendingName: String? = null,
    val amount: String = "",
    val unit: IngredientUnit = IngredientUnit.CL,
    val assignedBottleId: Long? = null,
    val preferredBrand: String? = null
)

data class AddEditRecipeUiState(
    val recipeId: Long? = null,
    val name: String = "",
    val instructions: String = "",
    val glassType: String = "",
    val sourceType: SourceType = SourceType.ORIGINAL,
    val imageUri: Uri? = null,
    val ingredients: List<IngredientInputState> = listOf(IngredientInputState()),
    val availableIngredients: List<IngredientEntity> = emptyList(),
    val ingredientUsages: Map<Long, Int> = emptyMap(),
    val inventory: List<BottleItem> = emptyList(),
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val isFetching: Boolean = false,
    val isInitialLoadDone: Boolean = false,
    val searchResults: List<RemoteRecipe> = emptyList()
)

@HiltViewModel
class AddEditRecipeViewModel @Inject constructor(
    private val repository: CocktailRepository,
    private val networkRepository: NetworkRecipeRepository,
    private val filePersistenceHelper: FilePersistenceHelper,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val recipeId: Long? = savedStateHandle.toRoute<Destination.AddEditRecipe>().recipeId

    private val _uiState = MutableStateFlow(AddEditRecipeUiState(recipeId = recipeId))
    val uiState: StateFlow<AddEditRecipeUiState> = _uiState.asStateFlow()

    init {
        combine(
            repository.getAllIngredients(),
            repository.getAllBottles(System.currentTimeMillis())
        ) { ingredients, bottles ->
            val usages = ingredients.associate { it.id to repository.getIngredientUsageCount(it.id) }
            _uiState.update { it.copy(
                availableIngredients = ingredients,
                inventory = bottles,
                ingredientUsages = usages,
                isInitialLoadDone = true
            ) }
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            _uiState.first { it.isInitialLoadDone }.let { 
                if (recipeId != null) {
                    loadExistingRecipe(recipeId)
                }
            }
        }
    }

    private suspend fun loadExistingRecipe(id: Long) {
        val recipe = repository.getRecipeById(id)
        if (recipe != null) {
            val ingredients = repository.getIngredientsForRecipe(id, System.currentTimeMillis())
            _uiState.update { state ->
                state.copy(
                    name = recipe.name,
                    instructions = recipe.instructions,
                    glassType = recipe.glassType,
                    sourceType = recipe.sourceType,
                    imageUri = recipe.imageUri?.let { Uri.parse(it) },
                    ingredients = ingredients.map { ing ->
                        IngredientInputState(
                            ingredient = state.availableIngredients.find { it.id == ing.ingredientId },
                            amount = ing.amount.formatAmount(),
                            unit = ing.unit,
                            assignedBottleId = ing.assignedBottleId,
                            preferredBrand = ing.preferredBrand
                        )
                    }.ifEmpty { listOf(IngredientInputState()) }
                )
            }
        }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun onInstructionsChange(instructions: String) {
        _uiState.update { it.copy(instructions = instructions) }
    }

    fun onGlassTypeChange(glassType: String) {
        _uiState.update { it.copy(glassType = glassType) }
    }

    fun onImageSelected(uri: Uri?) {
        viewModelScope.launch {
            val localUri = uri?.let { filePersistenceHelper.saveImageToInternalStorage(it) }
            _uiState.update { it.copy(imageUri = localUri) }
        }
    }

    fun fetchClassicSpecs() {
        val name = _uiState.value.name
        if (name.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isFetching = true) }
            try {
                val results = networkRepository.searchRecipes(name)
                val exactMatch = results.find { it.name.equals(name, ignoreCase = true) }
                
                if (results.size == 1 && exactMatch != null) {
                    onSearchResultSelected(results.first())
                } else if (results.isNotEmpty()) {
                    _uiState.update { it.copy(searchResults = results, isFetching = false) }
                } else {
                    _uiState.update { it.copy(isFetching = false, searchResults = emptyList()) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isFetching = false, searchResults = emptyList()) }
            }
        }
    }

    fun onSearchResultSelected(remote: RemoteRecipe) {
        viewModelScope.launch {
            val available = _uiState.value.availableIngredients
            val mappedIngredients = remote.ingredients.map { remoteIng ->
                val exactMatch = available.find { it.name.equals(remoteIng.name, ignoreCase = true) }
                val fallbackMatch = if (exactMatch == null) {
                    available.find { remoteIng.name.contains(it.name, ignoreCase = true) }
                } else null
                
                val existingMatch = exactMatch ?: fallbackMatch
                
                if (existingMatch != null) {
                    IngredientInputState(
                        ingredient = existingMatch,
                        amount = if (remoteIng.amount > 0) remoteIng.amount.formatAmount() else "",
                        unit = remoteIng.unit
                    )
                } else {
                    IngredientInputState(
                        pendingName = remoteIng.name,
                        amount = if (remoteIng.amount > 0) remoteIng.amount.formatAmount() else "",
                        unit = remoteIng.unit
                    )
                }
            }

            _uiState.update {
                it.copy(
                    name = remote.name,
                    instructions = remote.instructions,
                    glassType = remote.glassType,
                    sourceType = SourceType.CLASSIC,
                    ingredients = mappedIngredients.ifEmpty { listOf(IngredientInputState()) },
                    imageUri = remote.imageUri?.let { url -> Uri.parse(url) },
                    isFetching = false,
                    searchResults = emptyList()
                )
            }
        }
    }

    fun onSearchDialogDismiss() {
        _uiState.update { it.copy(searchResults = emptyList()) }
    }

    fun addIngredient() {
        _uiState.update { it.copy(ingredients = it.ingredients + IngredientInputState()) }
    }

    fun removeIngredient(index: Int) {
        _uiState.update { 
            val newList = it.ingredients.toMutableList()
            if (newList.size > 1) {
                newList.removeAt(index)
            }
            it.copy(ingredients = newList)
        }
    }

    fun updateIngredient(index: Int, update: (IngredientInputState) -> IngredientInputState) {
        _uiState.update { 
            val newList = it.ingredients.toMutableList()
            newList[index] = update(newList[index])
            it.copy(ingredients = newList)
        }
    }

    fun onAddNewIngredient(name: String) {
        viewModelScope.launch {
            val ingredient = IngredientEntity(name = name, isPerishable = false)
            repository.addIngredient(ingredient)
        }
    }

    fun deleteCatalogIngredient(ingredient: IngredientEntity) {
        viewModelScope.launch {
            repository.deleteIngredient(ingredient)
        }
    }

    fun renameIngredient(ingredient: IngredientEntity, newName: String) {
        viewModelScope.launch {
            repository.renameOrMergeIngredient(ingredient, newName)
        }
    }

    fun pruneUnused() {
        viewModelScope.launch {
            repository.pruneUnusedIngredients()
        }
    }

    fun saveRecipe() {
        val currentState = _uiState.value
        if (currentState.name.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            
            val finalIngredientRefs = currentState.ingredients.mapNotNull { input ->
                val ingredientId = when {
                    input.ingredient != null -> input.ingredient.id
                    input.pendingName != null -> {
                        repository.addIngredient(IngredientEntity(name = input.pendingName, isPerishable = false))
                    }
                    else -> null
                }

                ingredientId?.let { id ->
                    RecipeIngredientCrossRefEntity(
                        recipeId = currentState.recipeId ?: 0,
                        ingredientId = id,
                        amount = input.amount.toDoubleOrNull() ?: 0.0,
                        unit = input.unit,
                        assignedBottleId = input.assignedBottleId,
                        preferredBrand = input.preferredBrand
                    )
                }
            }

            val recipe = CocktailRecipeEntity(
                id = currentState.recipeId ?: 0,
                name = currentState.name,
                instructions = currentState.instructions,
                imageUri = currentState.imageUri?.toString(),
                glassType = currentState.glassType,
                sourceType = currentState.sourceType
            )
            
            repository.saveRecipe(recipe, finalIngredientRefs)
            repository.pruneUnusedIngredients()
            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }
}
