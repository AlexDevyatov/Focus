package com.example.neuralphotoredactor.di

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Главный класс Application приложения с поддержкой Hilt Dependency Injection.
 */
@HiltAndroidApp
class App : Application()

