package com.cocktailcraft.android.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cocktailcraft.android.data.local.entity.BottleItem
import com.cocktailcraft.android.data.local.entity.RecipeWithMissingCount
import com.cocktailcraft.android.domain.repository.CocktailRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InventoryUiState(
    val bottles: List<BottleItem> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val selectedImageUri: String? = null,
    val selectedBottle: BottleItem? = null,
    val matchingRecipes: List<RecipeWithMissingCount> = emptyList()
)

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val repository: CocktailRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedImageUri = MutableStateFlow<String?>(null)
    private val _selectedBottle = MutableStateFlow<BottleItem?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<InventoryUiState> = combine(
        repository.getAllBottles(System.currentTimeMillis()),
        _searchQuery,
        _selectedImageUri,
        _selectedBottle
    ) { bottles, query, selectedImage, selectedBottle ->
        val filteredBottles = if (query.isBlank()) {
            bottles
        } else {
            bottles.filter { 
                it.brandName.contains(query, ignoreCase = true) || 
                it.ingredientName.contains(query, ignoreCase = true) 
            }
        }
        InventoryUiState(
            bottles = filteredBottles, 
            searchQuery = query,
            selectedImageUri = selectedImage,
            selectedBottle = selectedBottle
        )
    }.flatMapLatest { state ->
        if (state.selectedBottle != null) {
            repository.getRecipesMatchingBottle(state.selectedBottle.id, System.currentTimeMillis()).map { recipes ->
                state.copy(matchingRecipes = recipes)
            }
        } else {
            flowOf(state.copy(matchingRecipes = emptyList()))
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InventoryUiState(isLoading = true)
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun showImage(uri: String?) {
        _selectedImageUri.value = uri
    }

    fun hideImage() {
        _selectedImageUri.value = null
    }

    fun selectBottle(bottle: BottleItem?) {
        _selectedBottle.value = bottle
    }

    fun refreshBottle(bottleId: Long) {
        viewModelScope.launch {
            val bottle = repository.getBottleById(bottleId) ?: return@launch
            val updated = bottle.copy(
                expiresAt = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L)
            )
            repository.updateBottle(updated)
        }
    }

    fun deleteBottle(bottleId: Long) {
        viewModelScope.launch {
            repository.deleteBottle(bottleId)
            repository.pruneUnusedIngredients()
        }
    }
}
