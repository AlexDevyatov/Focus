package com.example.neuralphotoredactor.presentation.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.example.neuralphotoredactor.presentation.navigation.Screen

/**
 * Компонент нижней навигационной панели.
 * 
 * @param currentRoute Текущий маршрут экрана
 * @param onNavigate Обработчик навигации
 * @param modifier Модификатор для настройки внешнего вида
 */
@Composable
fun BottomNavigationBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(modifier = modifier) {
        NavigationBarItem(
            selected = currentRoute == Screen.Gallery.route,
            onClick = { onNavigate(Screen.Gallery.route) },
            icon = { 
                Icon(
                    painter = painterResource(android.R.drawable.ic_menu_gallery),
                    contentDescription = "Gallery"
                )
            },
            label = { Text("Gallery") }
        )
        NavigationBarItem(
            selected = currentRoute == Screen.History.route,
            onClick = { onNavigate(Screen.History.route) },
            icon = { 
                Icon(
                    painter = painterResource(android.R.drawable.ic_menu_recent_history),
                    contentDescription = "History"
                )
            },
            label = { Text("History") }
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Settings.route,
            onClick = { onNavigate(Screen.Settings.route) },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") }
        )
    }
}

