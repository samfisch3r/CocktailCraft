package com.cocktailcraft.android.ui.dashboard

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cocktailcraft.android.data.local.entity.RecipeWithRating
import com.cocktailcraft.android.domain.repository.CocktailRepository
import com.cocktailcraft.android.util.FilePersistenceHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import javax.inject.Inject

data class DashboardUiState(
    val recipes: List<RecipeWithRating> = emptyList(),
    val unratedRecipes: List<RecipeWithRating> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: CocktailRepository,
    private val filePersistenceHelper: FilePersistenceHelper
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

    fun exportBackup(context: Context, uri: Uri, onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val backup = repository.getFullBackup()
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    filePersistenceHelper.exportBackupZip(backup, outputStream)
                }
                withContext(Dispatchers.Main) {
                    onComplete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun restoreBackup(context: Context, uri: Uri, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val backup = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    filePersistenceHelper.importBackupZip(inputStream)
                }
                if (backup != null) {
                    repository.restoreBackup(backup)
                    withContext(Dispatchers.Main) {
                        onSuccess()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
