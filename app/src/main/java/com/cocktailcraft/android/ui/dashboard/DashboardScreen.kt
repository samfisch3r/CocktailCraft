package com.cocktailcraft.android.ui.dashboard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.cocktailcraft.android.data.local.entity.CocktailRecipeEntity
import com.cocktailcraft.android.data.local.entity.RecipeWithRating
import com.cocktailcraft.android.util.OnShakeListener
import java.io.InputStream
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onRecipeClick: (Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showSettings by remember { mutableStateOf(value = false) }
    var surpriseRecipe by remember { mutableStateOf<RecipeWithRating?>(null) }

    fun pickSurpriseRecipe() {
        val availableCandidates = uiState.recipes.filter { it.averageRating == null || it.averageRating > 2.0f }
        val pool = availableCandidates.ifEmpty {
            uiState.unratedRecipes.filter { it.averageRating == null || it.averageRating > 2.0f }
        }
        if (pool.isNotEmpty()) {
            val currentId = surpriseRecipe?.recipe?.id
            val candidates = if (pool.size > 1 && currentId != null) {
                pool.filter { it.recipe.id != currentId }
            } else {
                pool
            }
            surpriseRecipe = candidates.random()
        }
    }

    OnShakeListener(enabled = uiState.recipes.isNotEmpty() || uiState.unratedRecipes.isNotEmpty()) {
        pickSurpriseRecipe()
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        uri?.let {
            viewModel.createBackup { json ->
                context.contentResolver.openOutputStream(it)?.use { output ->
                    output.write(json.toByteArray())
                }
            }
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let {
            val inputStream: InputStream? = context.contentResolver.openInputStream(it)
            inputStream?.bufferedReader()?.use { reader ->
                viewModel.restoreBackup(reader.readText()) {
                    // Refresh or notify
                }
            }
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
                        Text("My Bar")
                        Text(
                            text = "${uiState.recipes.size} Ready",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (uiState.recipes.isNotEmpty()) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.recipes.isEmpty() && uiState.unratedRecipes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        text = "Your bar is empty!",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Add some bottles to your inventory and recipes to your library to get started.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.recipes.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "What You Can Make Now",
                                style = MaterialTheme.typography.titleLarge
                            )
                            FilledTonalButton(
                                onClick = { pickSurpriseRecipe() },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("🎲 Surprise Me")
                            }
                        }
                    }
                    items(uiState.recipes) { item ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            RecipeItem(
                                recipe = item.recipe,
                                averageRating = item.averageRating,
                            ) { onRecipeClick(item.recipe.id) }
                        }
                    }
                }

                if (uiState.unratedRecipes.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Try These Next (Unrated)",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                    items(uiState.unratedRecipes) { item ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            RecipeItem(
                                recipe = item.recipe,
                                averageRating = item.averageRating,
                            ) { onRecipeClick(item.recipe.id) }
                        }
                    }
                }
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            onDismiss = { showSettings = false },
            onExport = { 
                showSettings = false
                createDocumentLauncher.launch("cocktail_backup.json")
            },
        ) {
            showSettings = false
            openDocumentLauncher.launch(arrayOf("application/json"))
        }
    }

    surpriseRecipe?.let { selected ->
        SurpriseRecipeDialog(
            recipeWithRating = selected,
            onDismiss = { surpriseRecipe = null },
            onReshuffle = { pickSurpriseRecipe() },
            onRecipeClick = { id ->
                surpriseRecipe = null
                onRecipeClick(id)
            }
        )
    }
}

@Composable
fun SurpriseRecipeDialog(
    recipeWithRating: RecipeWithRating,
    onDismiss: () -> Unit,
    onReshuffle: () -> Unit,
    onRecipeClick: (Long) -> Unit,
) {
    val recipe = recipeWithRating.recipe
    val rating = recipeWithRating.averageRating

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Tonight's Choice",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                recipe.imageUri?.let { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .clip(MaterialTheme.shapes.medium),
                        contentScale = ContentScale.Fit
                    )
                }

                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                if (rating != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(4.dp))
                        val displayRating = (rating * 10).roundToInt() / 10f
                        Text(
                            text = "$displayRating / 5.0",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = "Unrated",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onReshuffle,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text("🎲 Reshuffle")
                }
                Button(
                    onClick = {
                        onDismiss()
                        onRecipeClick(recipe.id)
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text("🍸 Let's Make It!")
                }
            }
        }
    )
}

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings & Maintenance") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Export your data to a file for backup, or restore from a previously saved file.", style = MaterialTheme.typography.bodyMedium)
                
                Button(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Export Bar Backup")
                }
                
                OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Clear, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Restore from Backup")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
fun RecipeItem(
    recipe: CocktailRecipeEntity,
    averageRating: Float?,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp), 
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            recipe.imageUri?.let {
                AsyncImage(
                    model = it,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = recipe.name, 
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (averageRating != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(4.dp))
                            val displayRating = (averageRating * 10).roundToInt() / 10f
                            Text(
                                text = displayRating.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
