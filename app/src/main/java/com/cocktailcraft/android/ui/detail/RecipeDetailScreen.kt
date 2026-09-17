package com.cocktailcraft.android.ui.detail

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.cocktailcraft.android.data.local.entity.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    viewModel: RecipeDetailViewModel,
    onEditClick: (Long) -> Unit,
    onDeleteSuccess: () -> Unit,
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val recipe = uiState.recipe ?: return

    var showRatingDialog by remember { mutableStateOf(value = false) }
    var showDeleteConfirmation by remember { mutableStateOf(value = false) }
    var showGlassInfo by remember { mutableStateOf(value = false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(recipe.name) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onEditClick(recipe.id) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Recipe")
                    }
                    IconButton(onClick = { showDeleteConfirmation = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Recipe")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                if (recipe.imageUri != null) {
                    AsyncImage(
                        model = recipe.imageUri,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Text(text = recipe.name, style = MaterialTheme.typography.headlineMedium)
                
                Text(
                    text = "Glass: ${recipe.glassType}", 
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = TextDecoration.Underline,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { showGlassInfo = true }
                )
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Ingredients", style = MaterialTheme.typography.titleMedium)
                        uiState.ingredients.forEach { ingredient ->
                            IngredientItem(
                                ingredient = ingredient,
                            ) { viewModel.loadMatchingBottles(ingredient.ingredientId, ingredient.ingredientName) }
                        }
                    }
                }
            }

            item {
                Text(text = "Instructions", style = MaterialTheme.typography.titleMedium)
                Text(text = recipe.instructions, style = MaterialTheme.typography.bodyLarge)
            }

            item {
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "Ratings", style = MaterialTheme.typography.titleMedium)
                    Button(onClick = { showRatingDialog = true }) {
                        Text("Add Rating")
                    }
                }
            }

            item {
                RatingsSection(
                    ratings = uiState.ratings,
                ) { viewModel.selectRating(it) }
            }
        }
    }

    if (showGlassInfo) {
        val glass = GlassType.fromString(recipe.glassType)
        GlassInfoDialog(
            glassType = glass,
        ) { showGlassInfo = false }
    }

    if (showRatingDialog) {
        AddRatingDialog(
            onDismiss = { showRatingDialog = false },
            onSave = { rating, notes ->
                viewModel.addRating(rating, notes)
                showRatingDialog = false
            }
        )
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Recipe?") },
            text = { Text("Are you sure you want to delete this recipe? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteRecipe(onDeleteSuccess)
                        showDeleteConfirmation = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    uiState.selectedRating?.let { rating ->
        RatingSnapshotDialog(
            rating = rating,
            ingredients = uiState.selectedRatingIngredients,
            onDismiss = { viewModel.selectRating(null) },
            onRestore = { viewModel.restoreRating(rating) }
        )
    }

    if (uiState.matchingBottles.isNotEmpty() || (uiState.selectedIngredientName != null)) {
        MatchingBottlesDialog(
            ingredientName = uiState.selectedIngredientName ?: "",
            bottles = uiState.matchingBottles,
            onDismiss = { viewModel.clearMatchingBottles() }
        )
    }
}

@Composable
fun GlassInfoDialog(
    glassType: GlassType,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(glassType.displayName) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    painter = painterResource(glassType.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(120.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = glassType.description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it")
            }
        }
    )
}

@Composable
fun IngredientItem(
    ingredient: RecipeIngredient,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            val displayName = ingredient.preferredBrand ?: ingredient.assignedBottleName ?: ingredient.ingredientName
            Text(
                text = displayName,
                style = if (ingredient.isAvailable) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.error)
            )
            
            if (ingredient.isAvailable) {
                val subText = when {
                    ingredient.assignedBottleId != null -> "Specific brand selected"
                    ingredient.preferredBrand != null -> "Preferred brand in stock"
                    else -> "Stocked: ${ingredient.bottleNames}"
                }
                Text(
                    text = subText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                val errorText = when {
                    ingredient.preferredBrand != null -> "Missing preferred brand: ${ingredient.preferredBrand}"
                    ingredient.assignedBottleId != null -> "Missing specific brand: ${ingredient.assignedBottleName}"
                    else -> "Missing"
                }
                Text(
                    text = errorText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        Text(
            text = if (ingredient.unit == IngredientUnit.TOP_UP) {
                ingredient.unit.displayName
            } else {
                "${ingredient.amount.formatAmount()} ${ingredient.unit.displayName}"
            },
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun MatchingBottlesDialog(
    ingredientName: String,
    bottles: List<BottleStockEntity>,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Inventory for $ingredientName") },
        text = {
            if (bottles.isEmpty()) {
                Text("No bottles of this type in your inventory.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    bottles.forEach { bottle ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = bottle.name, style = MaterialTheme.typography.titleMedium)
                                if (!bottle.notes.isNullOrBlank()) {
                                    Text(text = bottle.notes, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun RatingsSection(
    ratings: List<RecipeVersionHistoryEntity>,
    onRatingClick: (RecipeVersionHistoryEntity) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(ratings) { rating ->
            RatingCard(rating, onClick = { onRatingClick(rating) })
        }
    }
}

@Composable
fun RatingCard(
    rating: RecipeVersionHistoryEntity,
    onClick: () -> Unit,
) {
    val date = remember(rating.timestamp) {
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(rating.timestamp))
    }
    Card(
        modifier = Modifier
            .width(200.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(text = "#${rating.versionNumber}", fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(text = rating.rating.toString())
                }
            }
            Text(text = date, style = MaterialTheme.typography.labelSmall)
            rating.tweakNotes?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = it, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            }
        }
    }
}

@Composable
fun RatingSnapshotDialog(
    rating: RecipeVersionHistoryEntity,
    ingredients: List<IngredientSnapshot>,
    onDismiss: () -> Unit,
    onRestore: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rating #${rating.versionNumber} Details") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "Ingredients", style = MaterialTheme.typography.titleSmall)
                ingredients.forEach { 
                    val name = it.preferredBrand ?: it.assignedBottleName ?: it.ingredientName
                    val text = if (it.unit == IngredientUnit.TOP_UP) {
                        "• $name: ${it.unit.displayName}"
                    } else {
                        "• $name: ${it.amount.formatAmount()} ${it.unit.displayName}"
                    }
                    Text(text = text, style = MaterialTheme.typography.bodySmall)
                }
                HorizontalDivider()
                Text(text = "Instructions", style = MaterialTheme.typography.titleSmall)
                Text(text = rating.instructions, style = MaterialTheme.typography.bodySmall)
                if (!rating.tweakNotes.isNullOrBlank()) {
                    HorizontalDivider()
                    Text(text = "Notes", style = MaterialTheme.typography.titleSmall)
                    Text(text = rating.tweakNotes, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = onRestore) {
                Text("Restore this spec")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun AddRatingDialog(
    onDismiss: () -> Unit,
    onSave: (Float, String) -> Unit,
) {
    var rating by remember { mutableFloatStateOf(4f) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Rating") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Rating: ${rating.toInt()} Stars")
                Slider(
                    value = rating,
                    onValueChange = { rating = it },
                    valueRange = 1f..5f,
                    steps = 3
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(rating, notes) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
