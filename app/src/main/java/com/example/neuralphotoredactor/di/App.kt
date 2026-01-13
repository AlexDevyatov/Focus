package com.example.neuralphotoredactor.di

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Главный класс Application приложения с поддержкой Hilt Dependency Injection.
 */
@HiltAndroidApp
class App : Application() {
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    override fun onCreate() {
        super.onCreate()
        
        // Инициализируем модели при первом запуске
        // Используем EntryPoint для получения зависимостей в Application
        applicationScope.launch {
            try {
                val entryPoint = AppEntryPoint.get(this@App)
                entryPoint.initializeNeuralModelsUseCase().invoke()
            } catch (e: Exception) {
                android.util.Log.e("App", "Ошибка инициализации моделей: ${e.message}", e)
            }
        }
    }
}

