package com.example.neuralphotoredactor.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

/**
 * Граф навигации приложения.
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    mainScreen: @Composable () -> Unit,
    galleryScreen: @Composable (String?) -> Unit,
    editorScreen: @Composable () -> Unit,
    historyScreen: @Composable () -> Unit,
    processedImagesScreen: @Composable () -> Unit,
    aiFiltersScreen: @Composable () -> Unit,
    aiPreviewScreen: @Composable (String?, String?) -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = "main",
    ) {
        composable("main") {
            mainScreen()
        }
        composable(
            route = "gallery?selectedFilter={selectedFilter}",
            arguments =
                listOf(
                    navArgument("selectedFilter") {
                        type = NavType.StringType
                        defaultValue = ""
                        nullable = true
                    },
                ),
        ) { backStackEntry ->
            val selectedFilter = backStackEntry.arguments?.getString("selectedFilter")
            galleryScreen(if (selectedFilter.isNullOrEmpty()) null else selectedFilter)
        }
        composable("editor") {
            editorScreen()
        }
        composable("history") {
            historyScreen()
        }
        composable("processed") {
            processedImagesScreen()
        }
        composable("ai_filters") {
            aiFiltersScreen()
        }
        composable(
            route = "ai_preview?imageUri={imageUri}&filterType={filterType}",
            arguments =
                listOf(
                    navArgument("imageUri") {
                        type = NavType.StringType
                    },
                    navArgument("filterType") {
                        type = NavType.StringType
                    },
                ),
        ) { backStackEntry ->
            val imageUriString = backStackEntry.arguments?.getString("imageUri")
            val filterTypeString = backStackEntry.arguments?.getString("filterType")
            aiPreviewScreen(imageUriString, filterTypeString)
        }
    }
}
