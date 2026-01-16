package com.example.neuralphotoredactor.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Preview компонент, демонстрирующий использование темы.
 *
 * Показывает кнопки и карточки в светлой и темной темах.
 */
@Preview(name = "Light Theme", showBackground = true)
@Composable
private fun LightThemePreview() {
    AppTheme(darkTheme = false) {
        ThemePreviewContent()
    }
}

@Preview(name = "Dark Theme", showBackground = true)
@Composable
private fun DarkThemePreview() {
    AppTheme(darkTheme = true) {
        ThemePreviewContent()
    }
}

@Composable
private fun ThemePreviewContent() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Заголовок
            Text(
                text = "Material Theme Preview",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            // Кнопки
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Primary")
                }

                OutlinedButton(
                    onClick = { },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Secondary")
                }
            }

            TextButton(onClick = { }) {
                Text("Tertiary Button")
            }

            // Карточки с разными размерами
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { },
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Card (Medium Shape)",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "This card uses medium rounded corners (8dp)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Карточка с small shape
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Surface (Small Shape)",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = "This uses small rounded corners (4dp)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Карточка с large shape
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                onClick = { },
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Card (Large Shape)",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = "This card uses large rounded corners (12dp)",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Error компонент
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.medium,
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Error Message",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }

            // Цветовая палитра
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Color Palette",
                    style = MaterialTheme.typography.titleMedium,
                )

                ColorSwatch(
                    label = "Primary",
                    color = MaterialTheme.colorScheme.primary,
                    onColor = MaterialTheme.colorScheme.onPrimary,
                )
                ColorSwatch(
                    label = "Secondary",
                    color = MaterialTheme.colorScheme.secondary,
                    onColor = MaterialTheme.colorScheme.onSecondary,
                )
                ColorSwatch(
                    label = "Tertiary",
                    color = MaterialTheme.colorScheme.tertiary,
                    onColor = MaterialTheme.colorScheme.onTertiary,
                )
                ColorSwatch(
                    label = "Surface",
                    color = MaterialTheme.colorScheme.surface,
                    onColor = MaterialTheme.colorScheme.onSurface,
                )
                ColorSwatch(
                    label = "Error",
                    color = MaterialTheme.colorScheme.error,
                    onColor = MaterialTheme.colorScheme.onError,
                )
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    label: String,
    color: androidx.compose.ui.graphics.Color,
    onColor: androidx.compose.ui.graphics.Color,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(48.dp),
        color = color,
        shape = MaterialTheme.shapes.small,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = onColor,
            )
        }
    }
}
