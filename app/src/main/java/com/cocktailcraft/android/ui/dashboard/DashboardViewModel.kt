package com.cocktailcraft.android.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cocktailcraft.android.data.local.entity.RecipeWithRating
import com.cocktailcraft.android.domain.model.BarBackup
import com.cocktailcraft.android.domain.repository.CocktailRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.text.Collator
import javax.inject.Inject

data class DashboardUiState(
    val recipes: List<RecipeWithRating> = emptyList(),
    val unratedRecipes: List<RecipeWithRating> = emptyList(),
    val isLoading: Boolean = false,
    val backupJson: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: CocktailRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.getAvailableRecipes(System.currentTimeMillis()),
        repository.getUnratedRecipes()
    ) { available, unrated ->
        val collator = Collator.getInstance()
        val sortedAvailable = available.sortedWith(
            compareBy<RecipeWithRating> { it.averageRating != null }
                .thenComparator { a, b -> collator.compare(a.recipe.name, b.recipe.name) }
        )
        val availableIds = sortedAvailable.asSequence().map { it.recipe.id }.toSet()
        DashboardUiState(
            recipes = sortedAvailable,
            unratedRecipes = unrated.filter { it.recipe.id !in availableIds }.take(5),
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(isLoading = true)
    )

    fun createBackup(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val backup = repository.getFullBackup()
            val json = Json.encodeToString(backup)
            onResult(json)
        }
    }

    fun restoreBackup(json: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val backup = Json.decodeFromString<BarBackup>(json)
                repository.restoreBackup(backup)
                onSuccess()
            } catch (_: Exception) {
                // Handle error
            }
        }
    }
}
