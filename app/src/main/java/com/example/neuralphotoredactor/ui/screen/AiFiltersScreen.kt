package com.example.neuralphotoredactor.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.neuralphotoredactor.R
import com.example.neuralphotoredactor.ui.theme.BackgroundDark

/**
 * Экран с AI фильтрами.
 * Отображает карточки: Суперразрешение, Умное шумоподавление, Художественный стиль,
 * и отдельные карточки для моделей стилизации (AnimeGAN Face Paint, CelebA Distill, Hayao).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiFiltersScreen(
    onBackClick: () -> Unit,
    onFilterClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.screen_ai_filters),
                        fontSize = 16.sp,
                        color = Color.White,
                    )
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = BackgroundDark,
                    ),
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.back_arrow),
                            contentDescription = stringResource(R.string.navigate_back),
                            tint = Color.White,
                        )
                    }
                },
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        // Список фильтров для отображения
        val filters =
            listOf(
                FilterItem(
                    title = stringResource(R.string.ai_filter_upscale),
                    icon = Icons.Filled.Hd,
                    imageResId = R.drawable.img_superresolution,
                    onClick = { onFilterClick("upscale") },
                ),
                FilterItem(
                    title = stringResource(R.string.ai_filter_denoise),
                    icon = Icons.Filled.Gradient,
                    imageResId = R.drawable.img_denoising,
                    onClick = { onFilterClick("denoise") },
                ),
                FilterItem(
                    title = stringResource(R.string.ai_filter_style_transfer),
                    icon = Icons.Filled.Spa,
                    imageResId = R.drawable.img_style_transfer,
                    onClick = { onFilterClick("style_transfer") },
                ),
                FilterItem(
                    title = stringResource(R.string.ai_filter_animegan_face_paint),
                    icon = Icons.Filled.Palette,
                    imageResId = R.drawable.img_style_transfer,
                    onClick = { onFilterClick("style_transfer:AnimeGAN Face Paint") },
                ),
                FilterItem(
                    title = stringResource(R.string.ai_filter_celeba_distill),
                    icon = Icons.Filled.Brush,
                    imageResId = R.drawable.img_style_transfer,
                    onClick = { onFilterClick("style_transfer:CelebA Distill") },
                ),
                FilterItem(
                    title = stringResource(R.string.ai_filter_hayao),
                    icon = Icons.Filled.ColorLens,
                    imageResId = R.drawable.img_style_transfer,
                    onClick = { onFilterClick("style_transfer:Hayao") },
                ),
            )

        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = 42.dp,
                        vertical = paddingValues.calculateTopPadding(),
                    ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            items(
                items = filters,
                key = { it.title },
            ) { filter ->
                AiFilterCard(
                    title = filter.title,
                    icon = filter.icon,
                    imageResId = filter.imageResId,
                    onClick = filter.onClick,
                    modifier = Modifier.fillMaxWidth().height(138.dp),
                )
            }
        }
    }
}

/**
 * Модель данных для карточки фильтра.
 */
private data class FilterItem(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val imageResId: Int,
    val onClick: () -> Unit,
)

/**
 * Карточка AI фильтра.
 *
 * @param title Название фильтра
 * @param icon Иконка фильтра
 * @param imageResId Ресурс изображения для карточки
 * @param onClick Обработчик клика по карточке
 * @param modifier Модификатор для настройки внешнего вида
 */
@Composable
private fun AiFilterCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    imageResId: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardColor = Color(0xFF313630)
    val cardShape = RoundedCornerShape(12.dp)

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = cardShape,
        colors =
            CardDefaults.cardColors(
                containerColor = cardColor,
            ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Фоновое изображение
            Image(
                painter = painterResource(id = imageResId),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            // Затемнение для лучшей читаемости текста
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
            )

            // Иконка и текст по левому краю (иконка сверху, текст снизу)
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp),
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontSize = 16.sp,
                    color = Color.White,
                    textAlign = TextAlign.Start,
                )
            }
        }
    }
}
