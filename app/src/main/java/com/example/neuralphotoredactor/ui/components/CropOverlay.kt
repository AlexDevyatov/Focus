package com.example.neuralphotoredactor.ui.components

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.neuralphotoredactor.R
import kotlin.math.max
import kotlin.math.min

/**
 * Overlay для кадрирования изображения.
 * Позволяет пользователю выбрать область обрезки путем перетаскивания углов прямоугольника.
 */
@Composable
fun CropOverlay(
    bitmap: Bitmap?,
    onCropApply: (Rect) -> Unit,
    onCropCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (bitmap == null) return
    
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    var displaySize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    
    // Вычисляем размеры изображения с учетом ContentScale.Fit
    val (imageDisplayWidth, imageDisplayHeight, offsetX, offsetY) = remember(
        imageSize.width,
        imageSize.height,
        displaySize.width,
        displaySize.height
    ) {
        if (imageSize.width == 0 || imageSize.height == 0 || 
            displaySize.width == 0 || displaySize.height == 0) {
            Quadruple(0, 0, 0f, 0f)
        } else {
            val bitmapAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
            val displayAspect = displaySize.width.toFloat() / displaySize.height.toFloat()
            
            val (imgW, imgH, offX, offY) = if (bitmapAspect > displayAspect) {
                // Изображение шире - масштабируется по ширине
                val h = (displaySize.width / bitmapAspect).toInt()
                Quadruple(displaySize.width, h, 0f, (displaySize.height - h) / 2f)
            } else {
                // Изображение выше - масштабируется по высоте
                val w = (displaySize.height * bitmapAspect).toInt()
                Quadruple(w, displaySize.height, (displaySize.width - w) / 2f, 0f)
            }
            Quadruple(imgW, imgH, offX, offY)
        }
    }
    
    // Начальная область обрезки (размером с изображение)
    var cropRect by remember { mutableStateOf(Rect(0, 0, 0, 0)) }
    
    // Обновляем cropRect когда размеры изображения становятся доступными
    LaunchedEffect(imageDisplayWidth, imageDisplayHeight, offsetX, offsetY) {
        if (imageDisplayWidth > 0 && imageDisplayHeight > 0) {
            cropRect = Rect(
                offsetX.toInt(),
                offsetY.toInt(),
                (offsetX + imageDisplayWidth).toInt(),
                (offsetY + imageDisplayHeight).toInt()
            )
        }
    }
    
    // Callback для обновления cropRect из pointerInput
    val updateCropRect: (Rect) -> Unit = { newRect ->
        cropRect = newRect
    }
    var isDragging by remember { mutableStateOf(false) }
    
    // Используем rememberUpdatedState для правильного захвата значений в pointerInput
    val currentCropRect = rememberUpdatedState(cropRect)
    val currentOffsetX = rememberUpdatedState(offsetX)
    val currentOffsetY = rememberUpdatedState(offsetY)
    val currentImageDisplayWidth = rememberUpdatedState(imageDisplayWidth)
    val currentImageDisplayHeight = rememberUpdatedState(imageDisplayHeight)
    val updateCropRectState = rememberUpdatedState(updateCropRect)
    
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Отображаем изображение
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    displaySize = coordinates.size
                }
        ) {
            androidx.compose.foundation.Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = stringResource(R.string.editor_original_image),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coordinates ->
                        imageSize = coordinates.size
                    }
            )
            
            // Overlay с затемнением и рамкой обрезки
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        var dragHandle: CropHandle? = null
                        var dragStartRect = Rect(0, 0, 0, 0)
                        var dragStartOffset = Offset.Zero
                        
                        detectDragGestures(
                            onDragStart = { offset ->
                                val rect = currentCropRect.value
                                dragHandle = getCropHandle(offset, rect)
                                if (dragHandle != null) {
                                    isDragging = true
                                    dragStartRect = rect
                                    dragStartOffset = offset
                                }
                            },
                            onDrag = { change, _ ->
                                dragHandle?.let { handle ->
                                    val currentRect = dragStartRect
                                    val deltaX = change.position.x - dragStartOffset.x
                                    val deltaY = change.position.y - dragStartOffset.y
                                    val offX = currentOffsetX.value
                                    val offY = currentOffsetY.value
                                    val imgW = currentImageDisplayWidth.value
                                    val imgH = currentImageDisplayHeight.value
                                    
                                    val newRect = when (handle) {
                                        CropHandle.TOP_LEFT -> {
                                            val newLeft = (currentRect.left + deltaX).coerceIn(
                                                offX,
                                                (currentRect.right - 20).toFloat()
                                            ).toInt()
                                            val newTop = (currentRect.top + deltaY).coerceIn(
                                                offY,
                                                (currentRect.bottom - 20).toFloat()
                                            ).toInt()
                                            Rect(
                                                newLeft,
                                                newTop,
                                                currentRect.right,
                                                currentRect.bottom
                                            )
                                        }
                                        CropHandle.TOP_RIGHT -> {
                                            val newRight = (currentRect.right + deltaX).coerceIn(
                                                (currentRect.left + 20).toFloat(),
                                                offX + imgW
                                            ).toInt()
                                            val newTop = (currentRect.top + deltaY).coerceIn(
                                                offY,
                                                (currentRect.bottom - 20).toFloat()
                                            ).toInt()
                                            Rect(
                                                currentRect.left,
                                                newTop,
                                                newRight,
                                                currentRect.bottom
                                            )
                                        }
                                        CropHandle.BOTTOM_LEFT -> {
                                            val newLeft = (currentRect.left + deltaX).coerceIn(
                                                offX,
                                                (currentRect.right - 20).toFloat()
                                            ).toInt()
                                            val newBottom = (currentRect.bottom + deltaY).coerceIn(
                                                (currentRect.top + 20).toFloat(),
                                                offY + imgH
                                            ).toInt()
                                            Rect(
                                                newLeft,
                                                currentRect.top,
                                                currentRect.right,
                                                newBottom
                                            )
                                        }
                                        CropHandle.BOTTOM_RIGHT -> {
                                            val newRight = (currentRect.right + deltaX).coerceIn(
                                                (currentRect.left + 20).toFloat(),
                                                offX + imgW
                                            ).toInt()
                                            val newBottom = (currentRect.bottom + deltaY).coerceIn(
                                                (currentRect.top + 20).toFloat(),
                                                offY + imgH
                                            ).toInt()
                                            Rect(
                                                currentRect.left,
                                                currentRect.top,
                                                newRight,
                                                newBottom
                                            )
                                        }
                                        CropHandle.CENTER -> {
                                            val rectWidth = currentRect.right - currentRect.left
                                            val rectHeight = currentRect.bottom - currentRect.top
                                            val newLeft = (currentRect.left + deltaX).coerceIn(
                                                offX,
                                                offX + imgW - rectWidth
                                            ).toInt()
                                            val newTop = (currentRect.top + deltaY).coerceIn(
                                                offY,
                                                offY + imgH - rectHeight
                                            ).toInt()
                                            Rect(
                                                newLeft,
                                                newTop,
                                                newLeft + rectWidth,
                                                newTop + rectHeight
                                            )
                                        }
                                    }
                                    updateCropRectState.value(newRect)
                                }
                            },
                            onDragEnd = {
                                isDragging = false
                                dragHandle = null
                            }
                        )
                    }
            ) {
                // Затемнение области вне обрезки
                val fullRectPath = Path().apply {
                    addRect(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
                }
                val cropRectPath = Path().apply {
                    addRect(
                        androidx.compose.ui.geometry.Rect(
                            cropRect.left.toFloat(),
                            cropRect.top.toFloat(),
                            cropRect.right.toFloat(),
                            cropRect.bottom.toFloat()
                        )
                    )
                }
                val overlayPath = Path().apply {
                    op(fullRectPath, cropRectPath, PathOperation.Difference)
                }
                drawPath(overlayPath, Color.Black.copy(alpha = 0.5f))
                
                // Рамка обрезки
                drawRect(
                    color = Color.White,
                    topLeft = Offset(cropRect.left.toFloat(), cropRect.top.toFloat()),
                    size = Size(
                        (cropRect.right - cropRect.left).toFloat(),
                        (cropRect.bottom - cropRect.top).toFloat()
                    ),
                    style = Stroke(width = 2.dp.toPx())
                )
                
                // Углы для перетаскивания
                val handleSize = 20.dp.toPx()
                val handles = listOf(
                    Offset(cropRect.left.toFloat(), cropRect.top.toFloat()),
                    Offset(cropRect.right.toFloat(), cropRect.top.toFloat()),
                    Offset(cropRect.left.toFloat(), cropRect.bottom.toFloat()),
                    Offset(cropRect.right.toFloat(), cropRect.bottom.toFloat())
                )
                handles.forEach { handle ->
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(handle.x - handleSize / 2, handle.y - handleSize / 2),
                        size = Size(handleSize, handleSize),
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawRect(
                        color = Color.Blue.copy(alpha = 0.3f),
                        topLeft = Offset(handle.x - handleSize / 2, handle.y - handleSize / 2),
                        size = Size(handleSize, handleSize)
                    )
                }
            }
        }
        
        // Кнопки управления
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FilledTonalButton(
                onClick = onCropCancel,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Close, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.edit_cancel))
            }
            Button(
                onClick = {
                    // Масштабируем координаты из UI в координаты bitmap
                    val scaledRect = scaleCropRectToBitmap(
                        cropRect,
                        bitmap.width,
                        bitmap.height,
                        imageDisplayWidth,
                        imageDisplayHeight,
                        offsetX,
                        offsetY
                    )
                    onCropApply(scaledRect)
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.edit_apply))
            }
        }
    }
}

/**
 * Масштабирует координаты обрезки из координат UI в координаты bitmap.
 */
private fun scaleCropRectToBitmap(
    uiRect: Rect,
    bitmapWidth: Int,
    bitmapHeight: Int,
    displayWidth: Int,
    displayHeight: Int,
    offsetX: Float,
    offsetY: Float
): Rect {
    // Нормализуем координаты относительно отображаемого изображения
    val normalizedLeft = (uiRect.left - offsetX).coerceIn(0f, displayWidth.toFloat())
    val normalizedTop = (uiRect.top - offsetY).coerceIn(0f, displayHeight.toFloat())
    val normalizedRight = (uiRect.right - offsetX).coerceIn(normalizedLeft, displayWidth.toFloat())
    val normalizedBottom = (uiRect.bottom - offsetY).coerceIn(normalizedTop, displayHeight.toFloat())
    
    // Масштабируем в координаты bitmap
    val scaleX = bitmapWidth.toFloat() / displayWidth.toFloat()
    val scaleY = bitmapHeight.toFloat() / displayHeight.toFloat()
    
    return Rect(
        (normalizedLeft * scaleX).toInt().coerceIn(0, bitmapWidth),
        (normalizedTop * scaleY).toInt().coerceIn(0, bitmapHeight),
        (normalizedRight * scaleX).toInt().coerceIn(0, bitmapWidth),
        (normalizedBottom * scaleY).toInt().coerceIn(0, bitmapHeight)
    )
}

/**
 * Определяет, какой угол прямоугольника обрезки был захвачен для перетаскивания.
 */
private fun getCropHandle(offset: Offset, cropRect: Rect): CropHandle? {
    val handleSize = 50f // Размер области захвата в пикселях (увеличен для удобства)
    
    // Проверяем углы в первую очередь (приоритет)
    val handles = listOf(
        CropHandle.TOP_LEFT to Offset(cropRect.left.toFloat(), cropRect.top.toFloat()),
        CropHandle.TOP_RIGHT to Offset(cropRect.right.toFloat(), cropRect.top.toFloat()),
        CropHandle.BOTTOM_LEFT to Offset(cropRect.left.toFloat(), cropRect.bottom.toFloat()),
        CropHandle.BOTTOM_RIGHT to Offset(cropRect.right.toFloat(), cropRect.bottom.toFloat())
    )
    
    // Проверяем углы
    for ((handle, position) in handles) {
        val distanceX = kotlin.math.abs(offset.x - position.x)
        val distanceY = kotlin.math.abs(offset.y - position.y)
        if (distanceX < handleSize && distanceY < handleSize) {
            return handle
        }
    }
    
    // Проверяем центр (для перемещения всего прямоугольника) только если не попали в угол
    if (offset.x >= cropRect.left && offset.x <= cropRect.right &&
        offset.y >= cropRect.top && offset.y <= cropRect.bottom) {
        return CropHandle.CENTER
    }
    
    return null
}

private enum class CropHandle {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    CENTER
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
