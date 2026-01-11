package com.example.neuralphotoredactor.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Светлая цветовая схема Material Design 3.
 */
private val ColorScheme = lightColorScheme(
    primary = BackgroundDark,
    secondary = SecondaryLight,
    tertiary = TertiaryLight,
    surface = SurfaceLight,
    background = BackgroundDark,
    error = ErrorLight,
    onPrimary = Color(0xFFFFFFFF), // Белый текст на ярких цветах
    onSecondary = Color(0xFFFFFFFF),
    onTertiary = Color(0xFFFFFFFF),
    onSurface = Color(0xFF212121), // Темно-серый текст на светлых поверхностях для лучшей читаемости
    onBackground = Color(0xFF212121),
    onError = Color(0xFFFFFFFF)
)

/**
 * Основная тема приложения.
 * 
 * Использует светлую тему с указанными цветами.
 * 
 * @param darkTheme Игнорируется - всегда используется светлая тема
 * @param content Composable контент, который будет обернут в тему
 */
@Composable
fun AppTheme(
    darkTheme: Boolean = false, // Всегда используем светлую тему
    content: @Composable () -> Unit
) {
    val colorScheme = ColorScheme
    
    // Настройка системного статус-бара
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
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

