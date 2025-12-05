package com.example.neuralphotoredactor.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.neuralphotoredactor.ui.screen.EditorScreen
import com.example.neuralphotoredactor.ui.screen.GalleryScreen
import com.example.neuralphotoredactor.ui.screen.HistoryScreen

/**
 * Граф навигации приложения.
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    galleryScreen: @Composable () -> Unit,
    editorScreen: @Composable () -> Unit,
    historyScreen: @Composable () -> Unit,
    processedImagesScreen: @Composable () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = "gallery"
    ) {
        composable("gallery") {
            galleryScreen()
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
    }
}

