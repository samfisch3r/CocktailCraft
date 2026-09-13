package com.cocktailcraft.android.ui.addedit

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.cocktailcraft.android.data.local.entity.*
import com.cocktailcraft.android.domain.repository.RemoteRecipe

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRecipeScreen(
    viewModel: AddEditRecipeViewModel,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddIngredientDialog by remember { mutableStateOf(value = false) }
    var showManageCatalogDialog by remember { mutableStateOf(value = false) }
    
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> viewModel.onImageSelected(uri) }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.recipeId == null) "Add Recipe" else "Edit Recipe") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showManageCatalogDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Manage Catalog")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.saveRecipe() },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Save Recipe") },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (!uiState.isInitialLoadDone) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .windowInsetsPadding(WindowInsets.ime)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = { viewModel.onNameChange(it) },
                        label = { Text("Recipe Name") },
                        modifier = Modifier.weight(1f),
                    )
                    
                    if (uiState.isFetching) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        IconButton(onClick = { viewModel.fetchClassicSpecs() }) {
                            Icon(
                                imageVector = Icons.Default.AutoFixHigh,
                                contentDescription = "Fetch Classic Specs",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                uiState.imageUri?.let {
                    AsyncImage(
                        model = it,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentScale = ContentScale.Crop,
                    )
                }
                Button(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (uiState.imageUri != null) "Change Photo" else "Add Photo")
                }

                var glassExpanded by remember { mutableStateOf(value = false) }
                ExposedDropdownMenuBox(
                    expanded = glassExpanded,
                    onExpandedChange = { glassExpanded = !glassExpanded },
                ) {
                    OutlinedTextField(
                        value = uiState.glassType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Glass Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = glassExpanded) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable).fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = glassExpanded,
                        onDismissRequest = { glassExpanded = false },
                    ) {
                        GlassType.entries.forEach { glass ->
                            DropdownMenuItem(
                                text = { Text(glass.displayName) },
                                onClick = {
                                    viewModel.onGlassTypeChange(glass.displayName)
                                    glassExpanded = false
                                },
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Ingredients", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { showAddIngredientDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("New Catalog Item")
                    }
                }

                uiState.ingredients.forEachIndexed { index, ingredient ->
                    IngredientRow(
                        ingredient = ingredient,
                        availableIngredients = uiState.availableIngredients,
                        inventory = uiState.inventory,
                        onUpdate = { update -> viewModel.updateIngredient(index, update) },
                    ) { viewModel.removeIngredient(index) }
                }

                TextButton(onClick = { viewModel.addIngredient() }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("Add Ingredient Line")
                }

                OutlinedTextField(
                    value = uiState.instructions,
                    onValueChange = { viewModel.onInstructionsChange(it) },
                    label = { Text("Instructions") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
                
                Spacer(Modifier.height(80.dp)) // Extra space for FAB
            }
        }
    }

    if (showAddIngredientDialog) {
        NewIngredientDialog(
            onDismiss = { showAddIngredientDialog = false },
        ) { name ->
            viewModel.onAddNewIngredient(name)
            showAddIngredientDialog = false
        }
    }

    if (showManageCatalogDialog) {
        ManageCatalogDialog(
            ingredients = uiState.availableIngredients,
            usages = uiState.ingredientUsages,
            onDismiss = { showManageCatalogDialog = false },
            onDelete = { viewModel.deleteCatalogIngredient(it) },
            onRename = { ingredient, newName -> viewModel.renameIngredient(ingredient, newName) },
        )
    }

    if (uiState.searchResults.isNotEmpty()) {
        SearchResultDialog(
            results = uiState.searchResults,
            onSelected = { viewModel.onSearchResultSelected(it) },
        ) { viewModel.onSearchDialogDismiss() }
    }
}

@Composable
fun ManageCatalogDialog(
    ingredients: List<IngredientEntity>,
    usages: Map<Long, Int>,
    onDismiss: () -> Unit,
    onDelete: (IngredientEntity) -> Unit,
    onRename: (IngredientEntity, String) -> Unit,
) {
    var editingIngredient by remember { mutableStateOf<IngredientEntity?>(null) }
    var editName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Ingredient Catalog") },
        text = {
            Column {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(ingredients) { ingredient ->
                        val usageCount = usages[ingredient.id] ?: 0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            if (editingIngredient?.id == ingredient.id) {
                                OutlinedTextField(
                                    value = editName,
                                    onValueChange = { editName = it },
                                    modifier = Modifier.weight(1f),
                                    trailingIcon = {
                                        IconButton(onClick = { 
                                            onRename(ingredient, editName)
                                            editingIngredient = null
                                        }) {
                                            Icon(Icons.Default.Check, contentDescription = "Save")
                                        }
                                    },
                                )
                            } else {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ingredient.name, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        text = if (usageCount > 0) "Used in $usageCount items" else "Unused",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (usageCount > 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                                    )
                                }
                                Row {
                                    IconButton(
                                        onClick = { 
                                            editingIngredient = ingredient
                                            editName = ingredient.name
                                        }
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Rename")
                                    }
                                    IconButton(
                                        onClick = { onDelete(ingredient) },
                                        enabled = usageCount == 0,
                                    ) {
                                        Icon(
                                            Icons.Default.Delete, 
                                            contentDescription = "Delete", 
                                            tint = if (usageCount == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                                        )
                                    }
                                }
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
    )
}

@Composable
fun SearchResultDialog(
    results: List<RemoteRecipe>,
    onSelected: (RemoteRecipe) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Recipe") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(results) { recipe ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelected(recipe) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        recipe.imageUri?.let {
                            AsyncImage(
                                model = it,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                contentScale = ContentScale.Crop,
                            )
                        }
                        Text(text = recipe.name, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
fun NewIngredientDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Ingredient") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Ingredient Name (e.g. Lime Juice)") },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientRow(
    ingredient: IngredientInputState,
    availableIngredients: List<IngredientEntity>,
    inventory: List<BottleItem>,
    onUpdate: ((IngredientInputState) -> IngredientInputState) -> Unit,
    onRemove: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = ingredient.ingredient?.name ?: ingredient.pendingName ?: "Select Ingredient Type",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable).fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        colors = if (ingredient.pendingName != null) OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.primary,
                            unfocusedTextColor = MaterialTheme.colorScheme.primary
                        ) else OutlinedTextFieldDefaults.colors()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        availableIngredients.forEach { ing ->
                            DropdownMenuItem(
                                text = { Text(ing.name) },
                                onClick = {
                                    onUpdate { it.copy(ingredient = ing, pendingName = null, assignedBottleId = null, preferredBrand = null) }
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                if (ingredient.preferredBrand == null && ((ingredient.ingredient != null) || (ingredient.pendingName != null))) {
                    IconButton(onClick = { onUpdate { it.copy(preferredBrand = "") } }) {
                        Icon(Icons.Default.Add, contentDescription = "Specify Brand")
                    }
                }
                
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove")
                }
            }

            if (((ingredient.ingredient != null) || (ingredient.pendingName != null)) && (ingredient.preferredBrand != null)) {
                val ingredientName = ingredient.ingredient?.name ?: ingredient.pendingName ?: ""
                val matchingBottles = inventory.filter { it.ingredientName.equals(ingredientName, ignoreCase = true) }
                var brandExpanded by remember { mutableStateOf(false) }
                
                ExposedDropdownMenuBox(
                    expanded = brandExpanded,
                    onExpandedChange = { brandExpanded = !brandExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = ingredient.preferredBrand,
                        onValueChange = { newBrand -> onUpdate { it.copy(preferredBrand = newBrand, assignedBottleId = null) } },
                        label = { Text("Specific Brand") },
                        placeholder = { Text(if (matchingBottles.isEmpty()) "Type brand name..." else "Select or type brand...") },
                        trailingIcon = { 
                            Row {
                                if (matchingBottles.isNotEmpty()) {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = brandExpanded)
                                }
                                IconButton(onClick = { onUpdate { it.copy(preferredBrand = null, assignedBottleId = null) } }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear Brand")
                                }
                            }
                        },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable).fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    
                    if (matchingBottles.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = brandExpanded,
                            onDismissRequest = { brandExpanded = false }
                        ) {
                            matchingBottles.forEach { bottle ->
                                DropdownMenuItem(
                                    text = { Text(bottle.brandName) },
                                    onClick = {
                                        onUpdate { it.copy(assignedBottleId = bottle.id, preferredBrand = bottle.brandName) }
                                        brandExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = if (ingredient.unit == IngredientUnit.TOP_UP) "" else ingredient.amount,
                    onValueChange = { value -> onUpdate { it.copy(amount = value) } },
                    label = { Text("Amount") },
                    enabled = ingredient.unit != IngredientUnit.TOP_UP,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                
                var unitExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = unitExpanded,
                    onExpandedChange = { unitExpanded = !unitExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = ingredient.unit.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unit") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = unitExpanded,
                        onDismissRequest = { unitExpanded = false }
                    ) {
                        IngredientUnit.entries.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit.displayName) },
                                onClick = {
                                    onUpdate { it.copy(unit = unit) }
                                    unitExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
