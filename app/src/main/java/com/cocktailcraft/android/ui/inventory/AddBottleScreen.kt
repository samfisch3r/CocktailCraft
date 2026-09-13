package com.cocktailcraft.android.ui.inventory

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.cocktailcraft.android.data.local.entity.IngredientEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBottleScreen(
    viewModel: AddBottleViewModel,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showAddIngredientDialog by remember { mutableStateOf(value = false) }
    var showManageCatalogDialog by remember { mutableStateOf(value = false) }
    var showDeleteConfirmation by remember { mutableStateOf(value = false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> viewModel.onImageSelected(uri) }

    LaunchedEffect(uiState.isSaved, uiState.isDeleted) {
        if (uiState.isSaved || uiState.isDeleted) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.bottleId == null) "Add Item" else "Edit Item") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.bottleId != null) {
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Item", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    IconButton(onClick = { showManageCatalogDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Manage Catalog")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.saveBottle() },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Save Item") },
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
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = { viewModel.onNameChange(it) },
                    label = { Text("Brand/Item Name (e.g. Gin Mare)") },
                    modifier = Modifier.fillMaxWidth(),
                )

                uiState.imageUri?.let {
                    AsyncImage(
                        model = it,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (uiState.imageUri != null) "Change Photo" else "Add Photo")
                    }
                    
                    OutlinedButton(
                        onClick = {
                            val query = uiState.name.ifBlank { "liquor bottle" }
                            val intent = Intent(Intent.ACTION_VIEW, "https://www.google.com/search?q=$query+bottle&tbm=isch".toUri())
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Search Web")
                    }
                }

                Text(
                    text = "Link to Catalog Ingredient",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IngredientPicker(
                        selected = uiState.selectedIngredient,
                        available = uiState.availableIngredients,
                        onSelected = { viewModel.onIngredientChange(it) },
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { showAddIngredientDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "New Ingredient Type")
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = "Currently In Stock", modifier = Modifier.weight(1f))
                    Switch(
                        checked = uiState.inStock,
                        onCheckedChange = { viewModel.onInStockChange(it) },
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = "Perishable (expires in 1 week)", modifier = Modifier.weight(1f))
                    Switch(
                        checked = uiState.isTemporary,
                        onCheckedChange = { viewModel.onTemporaryChange(it) },
                    )
                }

                OutlinedTextField(
                    value = uiState.notes,
                    onValueChange = { viewModel.onNotesChange(it) },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
                
                Spacer(modifier = Modifier.height(80.dp))
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

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Item?") },
            text = { Text("Are you sure you want to remove this item from your inventory?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteBottle()
                        showDeleteConfirmation = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            },
        )
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
                                    IconButton(onClick = { 
                                        editingIngredient = ingredient
                                        editName = ingredient.name
                                    }) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientPicker(
    selected: IngredientEntity?,
    available: List<IngredientEntity>,
    onSelected: (IngredientEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(value = false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selected?.name ?: "Select Catalog Ingredient (e.g. Gin)",
            onValueChange = {},
            readOnly = true,
            label = { Text("Required for Recipes") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            available.forEach { ingredient ->
                DropdownMenuItem(
                    text = { Text(ingredient.name) },
                    onClick = {
                        onSelected(ingredient)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun NewIngredientDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Catalog Ingredient") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (e.g. Gin)") },
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
