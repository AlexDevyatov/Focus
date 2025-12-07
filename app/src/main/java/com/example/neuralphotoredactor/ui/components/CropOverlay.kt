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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
                Triple(displaySize.width, h, 0f, (displaySize.height - h) / 2f)
            } else {
                // Изображение выше - масштабируется по высоте
                val w = (displaySize.height * bitmapAspect).toInt()
                Triple(w, displaySize.height, (displaySize.width - w) / 2f, 0f)
            }
            Quadruple(imgW, imgH, offX, offY)
        }
    }
    
    // Начальная область обрезки (80% от размера изображения, по центру)
    val initialCropRect = remember(imageDisplayWidth, imageDisplayHeight) {
        if (imageDisplayWidth > 0 && imageDisplayHeight > 0) {
            val marginX = imageDisplayWidth * 0.1f
            val marginY = imageDisplayHeight * 0.1f
            Rect(
                (offsetX + marginX).toInt(),
                (offsetY + marginY).toInt(),
                (offsetX + imageDisplayWidth - marginX).toInt(),
                (offsetY + imageDisplayHeight - marginY).toInt()
            )
        } else {
            Rect(0, 0, 0, 0)
        }
    }
    
    var cropRect by remember { mutableStateOf(initialCropRect) }
    var isDragging by remember { mutableStateOf(false) }
    var dragHandle by remember { mutableStateOf<CropHandle?>(null) }
    
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
                        detectDragGestures(
                            onDragStart = { offset ->
                                dragHandle = getCropHandle(offset, cropRect)
                                isDragging = dragHandle != null
                            },
                            onDrag = { change, _ ->
                                dragHandle?.let { handle ->
                                    val newRect = when (handle) {
                                        CropHandle.TOP_LEFT -> {
                                            val newLeft = (change.position.x - offsetX).coerceIn(0f, cropRect.right - offsetX - 20f)
                                            val newTop = (change.position.y - offsetY).coerceIn(0f, cropRect.bottom - offsetY - 20f)
                                            Rect(
                                                (newLeft + offsetX).toInt(),
                                                (newTop + offsetY).toInt(),
                                                cropRect.right,
                                                cropRect.bottom
                                            )
                                        }
                                        CropHandle.TOP_RIGHT -> {
                                            val newRight = (change.position.x - offsetX).coerceIn(cropRect.left - offsetX + 20f, imageDisplayWidth.toFloat())
                                            val newTop = (change.position.y - offsetY).coerceIn(0f, cropRect.bottom - offsetY - 20f)
                                            Rect(
                                                cropRect.left,
                                                (newTop + offsetY).toInt(),
                                                (newRight + offsetX).toInt(),
                                                cropRect.bottom
                                            )
                                        }
                                        CropHandle.BOTTOM_LEFT -> {
                                            val newLeft = (change.position.x - offsetX).coerceIn(0f, cropRect.right - offsetX - 20f)
                                            val newBottom = (change.position.y - offsetY).coerceIn(cropRect.top - offsetY + 20f, imageDisplayHeight.toFloat())
                                            Rect(
                                                (newLeft + offsetX).toInt(),
                                                cropRect.top,
                                                cropRect.right,
                                                (newBottom + offsetY).toInt()
                                            )
                                        }
                                        CropHandle.BOTTOM_RIGHT -> {
                                            val newRight = (change.position.x - offsetX).coerceIn(cropRect.left - offsetX + 20f, imageDisplayWidth.toFloat())
                                            val newBottom = (change.position.y - offsetY).coerceIn(cropRect.top - offsetY + 20f, imageDisplayHeight.toFloat())
                                            Rect(
                                                cropRect.left,
                                                cropRect.top,
                                                (newRight + offsetX).toInt(),
                                                (newBottom + offsetY).toInt()
                                            )
                                        }
                                        CropHandle.CENTER -> {
                                            val dx = change.position.x - (cropRect.left + cropRect.right) / 2f
                                            val dy = change.position.y - (cropRect.top + cropRect.bottom) / 2f
                                            val newLeft = (cropRect.left + dx).coerceIn(offsetX, offsetX + imageDisplayWidth - (cropRect.right - cropRect.left))
                                            val newTop = (cropRect.top + dy).coerceIn(offsetY, offsetY + imageDisplayHeight - (cropRect.bottom - cropRect.top))
                                            Rect(
                                                newLeft.toInt(),
                                                newTop.toInt(),
                                                (newLeft + (cropRect.right - cropRect.left)).toInt(),
                                                (newTop + (cropRect.bottom - cropRect.top)).toInt()
                                            )
                                        }
                                    }
                                    cropRect = newRect
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
                val overlayPath = Path().apply {
                    addRect(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
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
                    op(cropRectPath, androidx.compose.ui.geometry.PathOperation.Difference)
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
    val handleSize = 30f // Размер области захвата в пикселях
    
    val handles = mapOf(
        CropHandle.TOP_LEFT to Offset(cropRect.left.toFloat(), cropRect.top.toFloat()),
        CropHandle.TOP_RIGHT to Offset(cropRect.right.toFloat(), cropRect.top.toFloat()),
        CropHandle.BOTTOM_LEFT to Offset(cropRect.left.toFloat(), cropRect.bottom.toFloat()),
        CropHandle.BOTTOM_RIGHT to Offset(cropRect.right.toFloat(), cropRect.bottom.toFloat())
    )
    
    // Проверяем углы
    handles.forEach { (handle, position) ->
        if (kotlin.math.abs(offset.x - position.x) < handleSize &&
            kotlin.math.abs(offset.y - position.y) < handleSize) {
            return handle
        }
    }
    
    // Проверяем центр (для перемещения всего прямоугольника)
    val centerX = (cropRect.left + cropRect.right) / 2f
    val centerY = (cropRect.top + cropRect.bottom) / 2f
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
