package com.example.neuralphotoredactor.ui.screen

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.example.neuralphotoredactor.R
import com.example.neuralphotoredactor.ui.components.ErrorMessage
import com.example.neuralphotoredactor.ui.components.LoadingIndicator
import com.example.neuralphotoredactor.ui.theme.BackgroundDark
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

/**
 * Экран галереи для выбора изображений.
 *
 * Автоматически запрашивает разрешение на доступ к галерее при открытии экрана.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    images: List<com.example.neuralphotoredactor.domain.model.ImageData>,
    isLoading: Boolean,
    isRefreshing: Boolean,
    error: String?,
    onImageClick: (com.example.neuralphotoredactor.domain.model.ImageData, String?) -> Unit,
    onRefresh: () -> Unit,
    onPermissionGranted: () -> Unit = {},
    onBackClick: (() -> Unit)? = null,
    selectedFilter: String? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // Определяем необходимое разрешение в зависимости от версии Android
    val permission =
        remember {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ (API 33+)
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                // Android 12 и ниже
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
        }

    // Состояние разрешения
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                permission,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED,
        )
    }

    // Launcher для запроса разрешения
    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            hasPermission = isGranted
            // Загружаем изображения сразу после предоставления разрешения
            if (isGranted) {
                onPermissionGranted()
            }
        }

    // Запрашиваем разрешение при открытии экрана, если оно не предоставлено
    LaunchedEffect(Unit) {
        if (hasPermission) {
            // Если разрешение уже предоставлено, загружаем изображения сразу
            onPermissionGranted()
        } else {
            // Если разрешения нет, запрашиваем его
            permissionLauncher.launch(permission)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.screen_gallery),
                        fontSize = 16.sp,
                        color = Color.White,
                    )
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = BackgroundDark,
                    ),
                navigationIcon = {
                    if (onBackClick != null) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                painter = painterResource(id = R.drawable.back_arrow),
                                contentDescription = stringResource(R.string.navigate_back),
                                tint = Color.White,
                            )
                        }
                    }
                },
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            when {
                // Проверяем разрешения
                !hasPermission -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(16.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.gallery_permission_title),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = stringResource(R.string.gallery_permission_message),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Button(
                                onClick = { permissionLauncher.launch(permission) },
                            ) {
                                Text(stringResource(R.string.gallery_permission_button))
                            }
                        }
                    }
                }
                isLoading -> {
                    LoadingIndicator(Modifier.fillMaxSize())
                }
                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        ErrorMessage(error, defaultMessageId = R.string.error_load_images)
                    }
                }
                else -> {
                    val configuration = LocalConfiguration.current
                    val density = LocalDensity.current

                    // Pull to refresh state
                    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = isRefreshing)

                    // Вычисляем оптимальный размер превью на основе размера экрана
                    // Для grid с 3 колонками и отступами (4.dp между элементами)
                    val itemSize =
                        remember(configuration.screenWidthDp, density) {
                            // Приблизительный расчет: ширина экрана / 3, минус отступы
                            val screenWidthPx = configuration.screenWidthDp * density.density
                            val spacingPx = 4.dp.value * 2 * density.density // Отступы между элементами
                            val itemWidthPx = (screenWidthPx - spacingPx) / 3
                            itemWidthPx.toInt()
                        }

                    SwipeRefresh(
                        state = swipeRefreshState,
                        onRefresh = onRefresh,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(
                                items = images,
                                key = {
                                        image ->
                                    image.uri.toString()
                                }, // Стабильные ключи для переиспользования
                            ) { image ->
                                GalleryImageItem(
                                    image = image,
                                    itemSize = itemSize,
                                    onClick = { onImageClick(image, selectedFilter) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Оптимизированный элемент изображения в галерее.
 *
 * Использует remember для оптимизации перекомпозиций и правильный размер превью.
 */
@Composable
private fun GalleryImageItem(
    image: com.example.neuralphotoredactor.domain.model.ImageData,
    itemSize: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // Запоминаем ImageRequest для избежания пересоздания
    val imageRequest =
        remember(image.uri, itemSize) {
            ImageRequest.Builder(context)
                .data(image.uri)
                .size(Size(itemSize, itemSize)) // Оптимизированный размер для превью
                .crossfade(200) // Плавное появление (уменьшено для быстрой загрузки)
                .allowHardware(true) // Использование hardware bitmap для лучшей производительности
                .build()
        }

    Card(
        onClick = onClick,
        modifier =
            modifier
                .aspectRatio(1f),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = imageRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // Показываем индикатор загрузки поверх изображения (Coil сам управляет видимостью)
        }
    }
}
