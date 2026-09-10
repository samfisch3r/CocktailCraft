package com.cocktailcraft.android.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cocktailcraft.android.data.local.entity.RecipeWithMissingCount
import com.cocktailcraft.android.domain.repository.CocktailRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class RecipeLibraryUiState(
    val recipes: List<RecipeWithMissingCount> = emptyList(),
    val searchQuery: String = "",
    val onlyReadyToMake: Boolean = false,
    val isLoading: Boolean = false
)

@HiltViewModel
class RecipeLibraryViewModel @Inject constructor(
    private val repository: CocktailRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _onlyReadyToMake = MutableStateFlow(false)

    val uiState: StateFlow<RecipeLibraryUiState> = combine(
        repository.getAllRecipesWithMissingCount(System.currentTimeMillis()),
        _searchQuery,
        _onlyReadyToMake
    ) { recipes, query, onlyReady ->
        val filtered = recipes.filter { 
            it.recipe.name.contains(query, ignoreCase = true) ||
            it.recipe.glassType.contains(query, ignoreCase = true) &&
            (!onlyReady || it.missingCount == 0)
        }
        RecipeLibraryUiState(
            recipes = filtered,
            searchQuery = query,
            onlyReadyToMake = onlyReady
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RecipeLibraryUiState(isLoading = true)
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onOnlyReadyToMakeChange(onlyReady: Boolean) {
        _onlyReadyToMake.value = onlyReady
    }
}
