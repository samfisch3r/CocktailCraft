package com.cocktailcraft.android.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cocktailcraft.android.data.local.entity.RecipeWithRating
import com.cocktailcraft.android.domain.repository.CocktailRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class DashboardUiState(
    val recipes: List<RecipeWithRating> = emptyList(),
    val unratedRecipes: List<RecipeWithRating> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: CocktailRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.getAvailableRecipes(System.currentTimeMillis()),
        repository.getUnratedRecipes(limit = 10)
    ) { available, unrated ->
        val availableIds = available.map { it.recipe.id }.toSet()
        DashboardUiState(
            recipes = available,
            unratedRecipes = unrated.filter { it.recipe.id !in availableIds },
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(isLoading = true)
    )
}
