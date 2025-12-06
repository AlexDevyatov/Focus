package com.example.neuralphotoredactor.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.neuralphotoredactor.R

/**
 * Нижняя панель навигации приложения.
 */
@Composable
fun BottomNavigationBar(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Скрываем bottom navigation на экране редактора
    if (currentRoute == "editor") {
        return@BottomNavigationBar
    }
    
    NavigationBar(modifier = modifier) {
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Photo, contentDescription = null) },
            label = { Text(stringResource(R.string.screen_gallery)) },
            selected = currentRoute == "gallery",
            onClick = {
                if (currentRoute != "gallery") {
                    navController.navigate("gallery") {
                        popUpTo("gallery") { inclusive = false }
                        launchSingleTop = true
                    }
                }
            }
        )
        
        NavigationBarItem(
            icon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null) },
            label = { Text(stringResource(R.string.screen_processed_images)) },
            selected = currentRoute == "processed",
            onClick = {
                if (currentRoute != "processed") {
                    navController.navigate("processed") {
                        popUpTo("gallery") { inclusive = false }
                        launchSingleTop = true
                    }
                }
            }
        )
        
        NavigationBarItem(
            icon = { Icon(Icons.Filled.History, contentDescription = null) },
            label = { Text(stringResource(R.string.screen_history)) },
            selected = currentRoute == "history",
            onClick = {
                if (currentRoute != "history") {
                    navController.navigate("history") {
                        popUpTo("gallery") { inclusive = false }
                        launchSingleTop = true
                    }
                }
            }
        )
    }
}

