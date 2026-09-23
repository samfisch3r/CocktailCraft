package com.cocktailcraft.android.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cocktailcraft.android.data.local.entity.RecipeWithMissingCount
import com.cocktailcraft.android.domain.repository.CocktailRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.text.Collator
import javax.inject.Inject

data class RecipeLibraryUiState(
    val recipes: List<RecipeWithMissingCount> = emptyList(),
    val searchQuery: String = "",
    val onlyReadyToMake: Boolean = false,
    val isLoading: Boolean = false
)

@HiltViewModel
class RecipeLibraryViewModel @Inject constructor(
    repository: CocktailRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _onlyReadyToMake = MutableStateFlow(false)

    val uiState: StateFlow<RecipeLibraryUiState> = combine(
        repository.getAllRecipesWithMissingCount(System.currentTimeMillis()),
        _searchQuery,
        _onlyReadyToMake
    ) { recipes, query, onlyReady ->
        val collator = Collator.getInstance()
        val filtered = recipes
            .filter { item ->
                val matchesQuery = query.isBlank() ||
                        item.recipe.name.contains(query, ignoreCase = true) ||
                        item.recipe.glassType.contains(query, ignoreCase = true)
                val matchesReady = !onlyReady || item.missingCount == 0
                matchesQuery && matchesReady
            }
            .sortedWith { a, b -> collator.compare(a.recipe.name, b.recipe.name) }

        RecipeLibraryUiState(
            recipes = filtered,
            searchQuery = query,
            onlyReadyToMake = onlyReady,
            isLoading = false
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
