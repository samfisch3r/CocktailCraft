package com.cocktailcraft.android.ui.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.cocktailcraft.android.data.local.entity.BottleItem
import com.cocktailcraft.android.data.local.entity.RecipeWithMissingCount
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel,
    onAddBottleClick: (Long?) -> Unit,
    onRecipeClick: (Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState()
    var showRecipesSheet by remember { mutableStateOf(value = false) }

    LaunchedEffect(uiState.selectedBottle) {
        if (uiState.selectedBottle != null) {
            showRecipesSheet = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Inventory")
                        Text(
                            text = "${uiState.bottles.size} Items",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onAddBottleClick(null) }) {
                Icon(Icons.Default.Add, contentDescription = "Add Bottle")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar - Aligned with Library
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search inventory...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = if (uiState.searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                } else null,
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
            )

            if (uiState.bottles.isEmpty() && uiState.searchQuery.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Your bar is empty. Add some items!")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.bottles) { bottle ->
                        InventoryCard(
                            bottle = bottle,
                            onImageClick = { viewModel.showImage(bottle.imageUri) },
                            onContentClick = { viewModel.selectBottle(bottle) },
                            onEditClick = { onAddBottleClick(bottle.id) },
                        ) { viewModel.refreshBottle(bottle.id) }
                    }
                }
            }
        }
    }

    if (showRecipesSheet && (uiState.selectedBottle != null)) {
        ModalBottomSheet(
            onDismissRequest = { 
                showRecipesSheet = false
                viewModel.selectBottle(null)
            },
            sheetState = sheetState,
        ) {
            MatchingRecipesList(
                bottle = uiState.selectedBottle!!,
                recipes = uiState.matchingRecipes,
            ) { 
                showRecipesSheet = false
                viewModel.selectBottle(null)
                onRecipeClick(it)
            }
        }
    }

    uiState.selectedImageUri?.let { uri ->
        FullImageDialog(
            imageUri = uri,
        ) { viewModel.hideImage() }
    }
}

@Composable
fun InventoryCard(
    bottle: BottleItem,
    onImageClick: () -> Unit,
    onContentClick: () -> Unit,
    onEditClick: () -> Unit,
    onRefresh: () -> Unit,
) {
    val isActuallyInStock = bottle.inStock && ((bottle.expiresAt == null) || (bottle.expiresAt > System.currentTimeMillis()))
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isActuallyInStock) 1f else 0.6f),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Left: Image
            if (bottle.imageUri != null) {
                AsyncImage(
                    model = bottle.imageUri,
                    contentDescription = "Bottle Image",
                    modifier = Modifier
                        .size(64.dp)
                        .clickable(onClick = onImageClick),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.WineBar,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        tint = MaterialTheme.colorScheme.outline,
                    )
                }
            }
            
            // Middle: Text Content (Clickable for recipes)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onContentClick),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = bottle.brandName, style = MaterialTheme.typography.titleLarge)
                    if (!isActuallyInStock) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.extraSmall,
                        ) {
                            Text(
                                text = "EMPTY",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                Text(
                    text = bottle.ingredientName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                if (bottle.expiresAt != null) {
                    val remainingDays = ((bottle.expiresAt - System.currentTimeMillis()) / (24 * 60 * 60 * 1000L)).coerceAtLeast(0)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (remainingDays > 0) "Expires in $remainingDays days" else "Expired",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = onRefresh, modifier = Modifier.size(24.dp)) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.RotateRight,
                                contentDescription = "Refresh Timer",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
                if (!bottle.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = bottle.notes, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }
            }
            
            // Right: Arrow Button (Go to edit)
            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                    contentDescription = "Edit Bottle",
                    tint = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
fun MatchingRecipesList(
    bottle: BottleItem,
    recipes: List<RecipeWithMissingCount>,
    onRecipeClick: (Long) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(
                text = "What can I make with...",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = bottle.brandName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (recipes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("No matching recipes in your library.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(recipes) { recipeWithCount ->
                    val recipe = recipeWithCount.recipe
                    val missingCount = recipeWithCount.missingCount
                    val averageRating = recipeWithCount.averageRating
                    
                    Card(
                        onClick = { onRecipeClick(recipe.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            recipe.imageUri?.let {
                                AsyncImage(
                                    model = it,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = recipe.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f),
                                    )
                                    if (averageRating != null) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary,
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            val displayRating = (averageRating * 10).roundToInt() / 10f
                                            Text(
                                                text = displayRating.toString(),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }
                                }
                                
                                if (missingCount == 0) {
                                    Text(
                                        text = "Ready to make",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF4CAF50),
                                        fontWeight = FontWeight.Bold,
                                    )
                                } else {
                                    Text(
                                        text = "Missing $missingCount other ingredients",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FullImageDialog(
    imageUri: String,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(8.dp),
            ) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Full Bottle Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp),
                    contentScale = ContentScale.Fit,
                )
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    }
}
