package com.example.neuralphotoredactor.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.neuralphotoredactor.ui.screen.EditorScreen
import com.example.neuralphotoredactor.ui.screen.GalleryScreen
import com.example.neuralphotoredactor.ui.screen.HistoryScreen
import com.example.neuralphotoredactor.ui.screen.MainScreen

/**
 * Граф навигации приложения.
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    mainScreen: @Composable () -> Unit,
    galleryScreen: @Composable () -> Unit,
    editorScreen: @Composable () -> Unit,
    historyScreen: @Composable () -> Unit,
    processedImagesScreen: @Composable () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") {
            mainScreen()
        }
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

