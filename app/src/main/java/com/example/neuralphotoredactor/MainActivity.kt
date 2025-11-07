package com.example.neuralphotoredactor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.neuralphotoredactor.presentation.navigation.AppNavigation
import com.example.neuralphotoredactor.ui.theme.NeuralPhotoRedactorTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Главная Activity приложения.
 * 
 * Инициализирует Hilt, настраивает тему и навигацию.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NeuralPhotoRedactorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}