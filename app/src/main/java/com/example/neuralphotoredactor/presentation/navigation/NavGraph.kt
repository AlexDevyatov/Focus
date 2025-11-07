package com.example.neuralphotoredactor.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.neuralphotoredactor.presentation.ui.components.BottomNavigationBar
import com.example.neuralphotoredactor.presentation.ui.screen.EditorScreen
import com.example.neuralphotoredactor.presentation.ui.screen.FiltersScreen
import com.example.neuralphotoredactor.presentation.ui.screen.GalleryScreen
import com.example.neuralphotoredactor.presentation.ui.screen.HistoryScreen
import com.example.neuralphotoredactor.presentation.ui.screen.SettingsScreen

/**
 * Sealed class для определения экранов приложения в навигации.
 * 
 * Используется с Jetpack Navigation Compose для определения маршрутов между экранами.
 * Каждый объект представляет отдельный экран приложения с уникальным route.
 * 
 * @property route Уникальный строковый идентификатор маршрута экрана
 */
sealed class Screen(val route: String) {
    /** Экран выбора изображения из галереи или камеры */
    object Gallery : Screen("gallery")
    
    /** Экран редактора изображений с панелью инструментов и предпросмотром */
    object Editor : Screen("editor/{imageUri}") {
        fun createRoute(imageUri: String) = "editor/$imageUri"
    }
    
    /** Экран со списком доступных AI фильтров и эффектов */
    object Filters : Screen("filters")
    
    /** Экран истории обработок с возможностью сравнения результатов */
    object History : Screen("history")
    
    /** Экран настроек приложения (качество обработки, API ключи и т.д.) */
    object Settings : Screen("settings")
}

/**
 * Навигационный граф приложения.
 * 
 * Определяет все маршруты и переходы между экранами.
 * 
 * @param navController NavController для управления навигацией
 * @param startDestination Начальный экран приложения
 */
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Gallery.route
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    androidx.compose.material3.Scaffold(
        bottomBar = {
            // Показываем BottomNavigationBar только на основных экранах
            if (currentRoute in listOf(Screen.Gallery.route, Screen.History.route, Screen.Settings.route)) {
                BottomNavigationBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            // Очищаем back stack при навигации к основным экранам
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Gallery.route) {
                GalleryScreen(
                    onImageSelected = { imageData ->
                        navController.navigate(Screen.Editor.createRoute(imageData.uri.toString()))
                    }
                )
            }
            
            composable(Screen.Editor.route) { backStackEntry ->
                val imageUri = backStackEntry.arguments?.getString("imageUri")
                val imageData = imageUri?.let { 
                    com.example.neuralphotoredactor.domain.model.ImageData(
                        uri = android.net.Uri.parse(it)
                    )
                }
                
                EditorScreen(
                    imageData = imageData,
                    onNavigateToFilters = {
                        navController.navigate(Screen.Filters.route)
                    }
                )
            }
            
            composable(Screen.Filters.route) {
                FiltersScreen(
                    onFilterSelected = { filter ->
                        // TODO: Применить фильтр и вернуться на экран редактора
                        navController.popBackStack()
                    }
                )
            }
            
            composable(Screen.History.route) {
                HistoryScreen()
            }
            
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}

