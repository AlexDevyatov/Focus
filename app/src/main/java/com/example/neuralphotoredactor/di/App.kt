package com.example.neuralphotoredactor.di

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Главный класс Application приложения с поддержкой Hilt Dependency Injection.
 * 
 * Аннотация @HiltAndroidApp инициализирует Hilt и делает Application класс
 * точкой входа для системы внедрения зависимостей. Все Hilt модули будут
 * доступны во всем приложении после инициализации этого класса.
 * 
 * Указывается в AndroidManifest.xml как android:name=".di.App"
 */
@HiltAndroidApp
class App : Application()

