package com.example.neuralphotoredactor.ui.screen

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.example.neuralphotoredactor.R
import com.example.neuralphotoredactor.domain.model.ImageData
import com.example.neuralphotoredactor.ui.components.ErrorMessage
import com.example.neuralphotoredactor.ui.components.LoadingIndicator
import com.example.neuralphotoredactor.ui.theme.BackgroundDark

/**
 * Экран обработанных изображений.
 * 
 * Отображает все изображения из папки processed в виде сетки, похожей на галерею.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessedImagesScreen(
    images: List<Uri>,
    isLoading: Boolean,
    isRefreshing: Boolean,
    error: String?,
    onImageClick: (ImageData) -> Unit,
    onRefresh: () -> Unit,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.screen_processed_images),
                        fontSize = 16.sp,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark
                ),
                navigationIcon = {
                    if (onBackClick != null) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                painter = painterResource(id = R.drawable.back_arrow),
                                contentDescription = stringResource(R.string.navigate_back),
                                tint = Color.White
                            )
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        when {
            isLoading && images.isEmpty() -> {
                LoadingIndicator(Modifier.padding(paddingValues))
            }
            error != null && images.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    ErrorMessage(error, defaultMessageId = R.string.error_load_images)
                }
            }
            images.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.processed_images_empty),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            else -> {
                val configuration = LocalConfiguration.current
                val density = LocalDensity.current
                
                // Pull to refresh state
                val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = isRefreshing)
                
                // Вычисляем оптимальный размер превью на основе размера экрана
                val itemSize = remember(configuration.screenWidthDp, density) {
                    val screenWidthPx = configuration.screenWidthDp * density.density
                    val spacingPx = 4.dp.value * 2 * density.density
                    val itemWidthPx = (screenWidthPx - spacingPx) / 3
                    itemWidthPx.toInt()
                }
                
                SwipeRefresh(
                    state = swipeRefreshState,
                    onRefresh = onRefresh,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(0.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = images,
                            key = { uri -> uri.toString() }
                        ) { uri ->
                            ProcessedImageItem(
                                uri = uri,
                                itemSize = itemSize,
                                onClick = { 
                                    onImageClick(ImageData(uri))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Элемент изображения в сетке обработанных изображений.
 */
@Composable
private fun ProcessedImageItem(
    uri: Uri,
    itemSize: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    val imageRequest = remember(uri, itemSize) {
        ImageRequest.Builder(context)
            .data(uri)
            .size(Size(itemSize, itemSize))
            .crossfade(200)
            .allowHardware(true)
            .build()
    }
    
    Card(
        onClick = onClick,
        modifier = modifier
            .aspectRatio(1f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = imageRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

