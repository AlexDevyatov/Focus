package com.example.neuralphotoredactor.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Светлая цветовая схема Material Design 3.
 */
private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    secondary = SecondaryLight,
    tertiary = TertiaryLight,
    surface = SurfaceLight,
    background = BackgroundLight,
    error = ErrorLight,
    onPrimary = BackgroundLight,
    onSecondary = BackgroundLight,
    onTertiary = BackgroundLight,
    onSurface = PrimaryLight,
    onBackground = PrimaryLight,
    onError = BackgroundLight
)

/**
 * Темная цветовая схема Material Design 3.
 */
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    secondary = SecondaryDark,
    tertiary = TertiaryDark,
    surface = SurfaceDark,
    background = BackgroundDark,
    error = ErrorDark,
    onPrimary = BackgroundDark,
    onSecondary = BackgroundDark,
    onTertiary = BackgroundDark,
    onSurface = SecondaryDark,
    onBackground = SecondaryDark,
    onError = BackgroundDark
)

/**
 * Основная тема приложения.
 * 
 * Поддерживает светлую и темную тему с указанными цветами.
 * 
 * @param darkTheme Если true, используется темная тема, иначе светлая
 * @param content Composable контент, который будет обернут в тему
 */
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    // Настройка системного статус-бара
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}

/**
 * Старая функция темы для обратной совместимости.
 * 
 * @deprecated Используйте AppTheme вместо этого
 */
@Composable
@Deprecated("Use AppTheme instead", ReplaceWith("AppTheme(darkTheme, content)"))
fun NeuralPhotoRedactorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    AppTheme(darkTheme = darkTheme, content = content)
}

