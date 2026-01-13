package com.example.neuralphotoredactor.ui.screen

import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.neuralphotoredactor.R
import com.example.neuralphotoredactor.data.storage.ShareHelper
import com.example.neuralphotoredactor.di.ShareHelperEntryPoint
import com.example.neuralphotoredactor.domain.model.ImageData
import com.example.neuralphotoredactor.domain.enums.FilterType
import com.example.neuralphotoredactor.ui.components.ErrorMessage
import com.example.neuralphotoredactor.ui.components.LoadingIndicator
import com.example.neuralphotoredactor.ui.theme.BackgroundDark
import com.example.neuralphotoredactor.ui.viewmodel.AiPreviewViewModel
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Экран предпросмотра AI фильтра.
 * Отображает обработанное изображение с возможностью сохранения или отмены.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiPreviewScreen(
    imageData: ImageData,
    filterType: FilterType,
    onSaveClick: () -> Unit,
    onCancelClick: () -> Unit,
    viewModel: AiPreviewViewModel = viewModel(),
    shareHelper: ShareHelper? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Получаем ShareHelper через EntryPointAccessors, если не передан
    val shareHelperInstance = shareHelper ?: remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            ShareHelperEntryPoint::class.java
        ).shareHelper()
    }
    
    // Launcher для запуска Intent шаринга
    val shareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { }
    
    // Устанавливаем изображение и фильтр при первом открытии экрана или при изменении
    LaunchedEffect(imageData.uri.toString(), filterType.name) {
        // Всегда устанавливаем заново, чтобы гарантировать обработку
        viewModel.setImageAndFilter(imageData, filterType)
    }
    
    // Функция для стандартного шаринга через системный диалог
    val shareImage = {
        val bitmap = uiState.processedBitmap
        if (bitmap != null) {
            coroutineScope.launch {
                val intent = withContext(Dispatchers.IO) {
                    shareHelperInstance.createShareIntent(
                        bitmap,
                        context.getString(R.string.ai_preview_share_dialog_title)
                    )
                }
                if (intent != null) {
                    try {
                        shareLauncher.launch(intent)
                    } catch (e: Exception) {
                        android.util.Log.e("AiPreviewScreen", "Ошибка открытия диалога шаринга: ${e.message}", e)
                    }
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.screen_ai_preview),
                        fontSize = 16.sp,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark
                ),
                navigationIcon = {
                    // Показываем крестик и галочку только после успешной обработки
                    if (!uiState.isLoading && uiState.processedBitmap != null && uiState.error == null) {
                        Row {
                            // Крестик для отмены
                            IconButton(onClick = onCancelClick) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.ai_preview_cancel),
                                    tint = Color.White
                                )
                            }
                            
                            // Галочка для сохранения
                            IconButton(onClick = {
                                viewModel.saveProcessedImage()
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = stringResource(R.string.ai_preview_save),
                                    tint = Color.White
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Показываем кнопки только после успешной обработки
                    if (!uiState.isLoading && uiState.processedBitmap != null && uiState.error == null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Кнопка сравнения - показываем только если есть исходное изображение
                            if (uiState.originalBitmap != null) {
                                IconButton(
                                    onClick = { viewModel.toggleImageComparison() },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.split_scene),
                                        contentDescription = stringResource(R.string.ai_preview_compare),
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            
                            // Кнопка шаринга
                            IconButton(
                                onClick = shareImage,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Share,
                                    contentDescription = stringResource(R.string.ai_preview_share),
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    // Показываем прогрессбар с процентами во время обработки
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = uiState.progress,
                                modifier = Modifier.size(64.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 6.dp
                            )
                            Text(
                                text = "${(uiState.progress * 100).toInt()}%",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                        }
                    }
                }
                
                uiState.error != null -> {
                    // Показываем ошибку
                    ErrorMessage(
                        message = uiState.error,
                        defaultMessageId = R.string.error_process_image
                    )
                }
                
                uiState.processedBitmap != null -> {
                    // Определяем какое изображение показывать (исходное или обработанное)
                    val bitmapToShow = if (uiState.showOriginal && uiState.originalBitmap != null) {
                        uiState.originalBitmap!!
                    } else {
                        uiState.processedBitmap!!
                    }
                    
                    // Показываем выбранное изображение
                    Image(
                        bitmap = bitmapToShow.asImageBitmap(),
                        contentDescription = if (uiState.showOriginal) {
                            stringResource(R.string.ai_preview_original_image)
                        } else {
                            stringResource(R.string.ai_preview_processed_image)
                        },
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Показываем индикатор сохранения, если идет сохранение
                    if (uiState.isSaving) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.Black.copy(alpha = 0.7f)
                                ),
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.ai_preview_saving),
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
                
                else -> {
                    // Пустое состояние
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.ai_preview_no_image),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
    
    // Обработка успешного сохранения
    LaunchedEffect(uiState.savedResult) {
        if (uiState.savedResult != null && !uiState.isSaving) {
            onSaveClick()
        }
    }
}

