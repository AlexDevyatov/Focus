package com.example.neuralphotoredactor.ui.screen

import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.animateRectAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.neuralphotoredactor.R
import com.example.neuralphotoredactor.ui.theme.AppTheme
import com.example.neuralphotoredactor.ui.theme.PrimaryLight
import com.example.neuralphotoredactor.ui.theme.SecondaryLight
import com.example.neuralphotoredactor.ui.theme.TertiaryLight
import java.security.spec.EllipticCurve

/**
 * Главный экран приложения с кнопками для навигации.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onGalleryClick: () -> Unit,
    onAiEditClick: () -> Unit,
    onCameraClick: () -> Unit,
    onHistoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Фоновое изображение
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // Контент поверх фона
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.weight(1f))
            
            FilledTonalButton(
                onClick = onGalleryClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Photo,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.main_button_gallery),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                FilledTonalButton(
                    onClick = onAiEditClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.main_button_ai_edit),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                }
                
                FilledTonalButton(
                    onClick = onCameraClick,
                    modifier = Modifier
                        .width(120.dp)
                        .height(80.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = stringResource(R.string.main_button_camera),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            FilledTonalButton(
                onClick = onHistoryClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.History,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.main_button_history),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

/**
 * Preview функция для предпросмотра MainScreen в Android Studio.
 * 
 * Использует жестко закодированные строки, так как Preview не имеет доступа к ресурсам.
 */
@Preview(name = "MainScreen Light", showBackground = true)
@Composable
private fun MainScreenPreview() {
    AppTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            // Фоновое изображение
            Image(
                painter = painterResource(id = R.drawable.background),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // Контент поверх фона
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(90.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Spacer(modifier = Modifier.weight(1f))
                
                FilledTonalButton(
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .border(width = 1.dp, color = SecondaryLight, shape = RoundedCornerShape(35.dp)),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Photo,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = TertiaryLight
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Галерея",
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 16.sp,
                        color = TertiaryLight,
                        textAlign = TextAlign.Center
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    FilledTonalButton(
                        onClick = {},
                        modifier = Modifier
                            .weight(1f)
                            .height(70.dp)
                            .border(width = 1.dp, color = SecondaryLight, shape = RoundedCornerShape(35.dp)),
                        shape = MaterialTheme.shapes.large,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = PrimaryLight
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "ИИ",
                            style = MaterialTheme.typography.titleLarge,
                            fontSize = 16.sp,
                            color = PrimaryLight,
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    // Кнопка "Камера" - меньшая, фиксированной ширины, только иконка
                    FilledTonalButton(
                        onClick = {},
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .width(80.dp)
                            .height(70.dp)
                            .border(width = 1.dp, color = SecondaryLight, shape = RoundedCornerShape(34.dp)),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = "Камера",
                            modifier = Modifier.size(25.dp),
                            tint = TertiaryLight
                        )
                    }
                }
                
                FilledTonalButton(
                    onClick = {},
                    modifier = Modifier
                        .width(150.dp)
                        .height(60.dp)
                        .border(width = 1.dp, color = SecondaryLight, shape = RoundedCornerShape(35.dp)),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = TertiaryLight
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "История",
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 16.sp,
                        color = TertiaryLight,
                        textAlign = TextAlign.Center
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

