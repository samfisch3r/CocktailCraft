package com.cocktailcraft.android.ui.inventory

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.cocktailcraft.android.data.local.entity.BottleStockEntity
import com.cocktailcraft.android.data.local.entity.IngredientEntity
import com.cocktailcraft.android.domain.repository.CocktailRepository
import com.cocktailcraft.android.ui.navigation.Destination
import com.cocktailcraft.android.util.FilePersistenceHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddBottleUiState(
    val bottleId: Long? = null,
    val name: String = "",
    val selectedIngredient: IngredientEntity? = null,
    val availableIngredients: List<IngredientEntity> = emptyList(),
    val ingredientUsages: Map<Long, Int> = emptyMap(),
    val isTemporary: Boolean = false,
    val notes: String = "",
    val imageUri: Uri? = null,
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false,
    val isInitialLoadDone: Boolean = false
)

@HiltViewModel
class AddBottleViewModel @Inject constructor(
    private val repository: CocktailRepository,
    private val filePersistenceHelper: FilePersistenceHelper,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val bottleId: Long? = savedStateHandle.toRoute<Destination.AddBottle>().bottleId

    private val _uiState = MutableStateFlow(AddBottleUiState(bottleId = bottleId))
    val uiState: StateFlow<AddBottleUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Wait for ingredients to load first
            repository.getAllIngredients()
                .onEach { ingredients ->
                    _uiState.update { it.copy(availableIngredients = ingredients) }
                    refreshUsageCounts()
                }
                .take(1)
                .collect()

            if (bottleId != null) {
                loadExistingBottle(bottleId)
            } else {
                _uiState.update { it.copy(isInitialLoadDone = true) }
            }
        }

        // Continue tracking ingredient updates
        repository.getAllIngredients()
            .onEach { ingredients -> 
                _uiState.update { it.copy(availableIngredients = ingredients) }
                refreshUsageCounts()
            }
            .launchIn(viewModelScope)
    }

    private suspend fun loadExistingBottle(id: Long) {
        val bottle = repository.getBottleById(id)
        if (bottle != null) {
            val ingredient = repository.getIngredientById(bottle.ingredientId)
            _uiState.update { state ->
                state.copy(
                    name = bottle.name,
                    selectedIngredient = ingredient,
                    notes = bottle.notes ?: "",
                    imageUri = bottle.imageUri?.let { Uri.parse(it) },
                    isTemporary = bottle.expiresAt != null,
                    isInitialLoadDone = true
                )
            }
        } else {
            _uiState.update { it.copy(isInitialLoadDone = true) }
        }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun onIngredientChange(ingredient: IngredientEntity) {
        _uiState.update { it.copy(selectedIngredient = ingredient) }
    }

    fun onTemporaryChange(isTemporary: Boolean) {
        _uiState.update { it.copy(isTemporary = isTemporary) }
    }

    fun onNotesChange(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    fun onImageSelected(uri: Uri?) {
        viewModelScope.launch {
            val localUri = uri?.let { filePersistenceHelper.saveImageToInternalStorage(it) }
            _uiState.update { it.copy(imageUri = localUri) }
        }
    }

    fun saveBottle() {
        val state = _uiState.value
        if (state.name.isBlank() || state.selectedIngredient == null) return

        viewModelScope.launch {
            val expiresAt = if (state.isTemporary) {
                // If editing and was already temporary, we could keep old expiry or refresh.
                // Bartender preference: Refresh it to 1 week from now if they are "saving" it.
                System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L)
            } else null

            val bottle = BottleStockEntity(
                id = state.bottleId ?: 0,
                ingredientId = state.selectedIngredient.id,
                name = state.name,
                notes = state.notes.takeIf { it.isNotBlank() },
                imageUri = state.imageUri?.toString(),
                expiresAt = expiresAt
            )
            
            if (state.bottleId == null) {
                repository.addBottle(bottle)
            } else {
                repository.updateBottle(bottle)
            }
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    fun deleteBottle() {
        val id = _uiState.value.bottleId ?: return
        viewModelScope.launch {
            repository.deleteBottle(id)
            repository.pruneUnusedIngredients()
            _uiState.update { it.copy(isDeleted = true) }
        }
    }

    fun onAddNewIngredient(name: String) {
        viewModelScope.launch {
            val newIngredient = IngredientEntity(name = name, isPerishable = false)
            val newId = repository.addIngredient(newIngredient)
            _uiState.update { it.copy(selectedIngredient = newIngredient.copy(id = newId)) }
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

    private fun refreshUsageCounts() {
        viewModelScope.launch {
            val counts = _uiState.value.availableIngredients.associate { 
                it.id to repository.getIngredientUsageCount(it.id)
            }
            _uiState.update { it.copy(ingredientUsages = counts) }
        }
    }
}
