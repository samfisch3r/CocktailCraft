package com.cocktailcraft.android.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.cocktailcraft.android.data.local.entity.CocktailRecipeEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeLibraryScreen(
    viewModel: RecipeLibraryViewModel,
    onRecipeClick: (Long) -> Unit,
    onAddRecipeClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Library") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddRecipeClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Recipe")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search recipes...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            // Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    FilterChip(
                        selected = uiState.onlyReadyToMake,
                        onClick = { viewModel.onOnlyReadyToMakeChange(!uiState.onlyReadyToMake) },
                        label = { Text("Ready to Make") }
                    )
                }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.recipes) { recipeWithCount ->
                        RecipeLibraryItem(
                            recipe = recipeWithCount.recipe,
                            missingCount = recipeWithCount.missingCount,
                            onClick = { onRecipeClick(recipeWithCount.recipe.id) }
                        )
                    }
                    if (uiState.recipes.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No recipes found.", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecipeLibraryItem(
    recipe: CocktailRecipeEntity,
    missingCount: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp), 
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (recipe.imageUri != null) {
                AsyncImage(
                    model = recipe.imageUri,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = recipe.name, style = MaterialTheme.typography.titleLarge)
                
                Spacer(modifier = Modifier.height(4.dp))
                
                if (missingCount == 0) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("Ready to Make") },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            labelColor = Color(0xFF4CAF50)
                        )
                    )
                } else {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("Missing $missingCount ingredients") },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            labelColor = MaterialTheme.colorScheme.error
                        )
                    )
                }
            }
        }
    }
}
