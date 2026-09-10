package com.cocktailcraft.android.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cocktailcraft.android.ui.addedit.AddEditRecipeScreen
import com.cocktailcraft.android.ui.addedit.AddEditRecipeViewModel
import com.cocktailcraft.android.ui.dashboard.DashboardScreen
import com.cocktailcraft.android.ui.dashboard.DashboardViewModel
import com.cocktailcraft.android.ui.detail.RecipeDetailScreen
import com.cocktailcraft.android.ui.detail.RecipeDetailViewModel
import com.cocktailcraft.android.ui.inventory.AddBottleScreen
import com.cocktailcraft.android.ui.inventory.AddBottleViewModel
import com.cocktailcraft.android.ui.inventory.InventoryScreen
import com.cocktailcraft.android.ui.inventory.InventoryViewModel
import com.cocktailcraft.android.ui.library.RecipeLibraryScreen
import com.cocktailcraft.android.ui.library.RecipeLibraryViewModel
import com.cocktailcraft.android.ui.navigation.Destination
import com.cocktailcraft.android.ui.theme.CocktailCraftTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CocktailCraftApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = remember(currentDestination) {
        currentDestination?.hasRoute<Destination.Dashboard>() == true ||
        currentDestination?.hasRoute<Destination.BottleInventory>() == true ||
        currentDestination?.hasRoute<Destination.RecipeLibrary>() == true
    }
    
    CocktailCraftTheme {
        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Home, contentDescription = null) },
                            label = { Text("My Bar") },
                            selected = currentDestination?.hasRoute<Destination.Dashboard>() == true,
                            onClick = {
                                navController.navigate(Destination.Dashboard) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = false
                                }
                            }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                            label = { Text("Inventory") },
                            selected = currentDestination?.hasRoute<Destination.BottleInventory>() == true,
                            onClick = {
                                navController.navigate(Destination.BottleInventory) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = false
                                }
                            }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.AutoMirrored.Filled.LibraryBooks, contentDescription = null) },
                            label = { Text("Library") },
                            selected = currentDestination?.hasRoute<Destination.RecipeLibrary>() == true ||
                                       currentDestination?.hasRoute<Destination.RecipeDetail>() == true,
                            onClick = {
                                navController.navigate(Destination.RecipeLibrary) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = false
                                }
                            }
                        )
                    }
                }
            },
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Destination.Dashboard,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable<Destination.Dashboard> {
                    val viewModel: DashboardViewModel = hiltViewModel()
                    DashboardScreen(
                        viewModel = viewModel,
                        onRecipeClick = { recipeId ->
                            navController.navigate(Destination.RecipeDetail(recipeId))
                        }
                    )
                }
                composable<Destination.BottleInventory> {
                    val viewModel: InventoryViewModel = hiltViewModel()
                    InventoryScreen(
                        viewModel = viewModel,
                        onAddBottleClick = { bottleId -> navController.navigate(Destination.AddBottle(bottleId)) },
                        onRecipeClick = { recipeId -> navController.navigate(Destination.RecipeDetail(recipeId)) }
                    )
                }
                composable<Destination.AddBottle> {
                    val viewModel: AddBottleViewModel = hiltViewModel()
                    AddBottleScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable<Destination.RecipeLibrary> {
                    val viewModel: RecipeLibraryViewModel = hiltViewModel()
                    RecipeLibraryScreen(
                        viewModel = viewModel,
                        onRecipeClick = { recipeId ->
                            navController.navigate(Destination.RecipeDetail(recipeId))
                        },
                        onAddRecipeClick = {
                            navController.navigate(Destination.AddEditRecipe())
                        }
                    )
                }
                composable<Destination.AddEditRecipe> {
                    val viewModel: AddEditRecipeViewModel = hiltViewModel()
                    AddEditRecipeScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable<Destination.RecipeDetail> {
                    val viewModel: RecipeDetailViewModel = hiltViewModel()
                    RecipeDetailScreen(
                        viewModel = viewModel,
                        onEditClick = { recipeId ->
                            navController.navigate(Destination.AddEditRecipe(recipeId))
                        },
                        onDeleteSuccess = {
                            navController.popBackStack()
                        },
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}
