package com.example.neuralphotoredactor.ui.components

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.neuralphotoredactor.ml.edit.ImageEditProcessor

/**
 * Overlay для кадрирования изображения.
 * Позволяет пользователю выбрать область для обрезки с помощью интерактивного прямоугольника.
 */
@Composable
fun CropOverlay(
    bitmap: Bitmap?,
    imageEditProcessor: ImageEditProcessor,
    onCropApply: (Bitmap) -> Unit,
    onCropCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (bitmap == null) return
    
    val density = LocalDensity.current
    
    // Состояние для области обрезки (в координатах изображения на экране)
    var cropRect by remember { mutableStateOf<ComposeRect?>(null) }
    var containerSize by remember { mutableStateOf<IntSize?>(null) }
    var displayedImageSize by remember { mutableStateOf<IntSize?>(null) }
    var imageOffset by remember { mutableStateOf<Offset?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    var dragHandle by remember { mutableStateOf<CropHandle?>(null) }
    
    // Вычисляем размер отображаемого изображения для ContentScale.Fit
    val imageScale = remember(containerSize) {
        if (containerSize == null) return@remember 1f
        val container = containerSize!!
        val scaleX = container.width.toFloat() / bitmap.width
        val scaleY = container.height.toFloat() / bitmap.height
        minOf(scaleX, scaleY)
    }
    
    val displayedSize = remember(containerSize, imageScale) {
        if (containerSize == null) return@remember null
        val scale = imageScale
        IntSize(
            (bitmap.width * scale).toInt(),
            (bitmap.height * scale).toInt()
        )
    }
    
    val imageTopLeft = remember(containerSize, displayedSize) {
        if (containerSize == null || displayedSize == null) return@remember null
        Offset(
            (containerSize!!.width - displayedSize!!.width) / 2f,
            (containerSize!!.height - displayedSize!!.height) / 2f
        )
    }
    
    // Инициализация области обрезки (80% от размера изображения по центру)
    LaunchedEffect(displayedSize) {
        if (displayedSize != null && cropRect == null) {
            val size = displayedSize!!
            val padding = 0.1f
            cropRect = ComposeRect(
                left = size.width * padding,
                top = size.height * padding,
                right = size.width * (1f - padding),
                bottom = size.height * (1f - padding)
            )
        }
    }
    
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Отображаем изображение
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    containerSize = coordinates.size
                    displayedImageSize = displayedSize
                    imageOffset = coordinates.positionInRoot()
                }
        ) {
            androidx.compose.foundation.Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Image to crop",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
            
            // Overlay с затемнением и рамкой обрезки
            if (cropRect != null && displayedSize != null && imageTopLeft != null) {
                CropOverlayCanvas(
                    cropRect = cropRect!!,
                    imageTopLeft = imageTopLeft!!,
                    displayedImageSize = displayedSize!!,
                    onCropRectChange = { newRect ->
                        cropRect = newRect
                    },
                    isDragging = isDragging,
                    onDragStart = { handle ->
                        dragHandle = handle
                        isDragging = true
                    },
                    onDragEnd = {
                        isDragging = false
                        dragHandle = null
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        
        // Кнопки управления
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onCropCancel) {
                    Text("Отмена")
                }
                Button(
                    onClick = {
                        if (cropRect != null && displayedSize != null && imageTopLeft != null) {
                            // Вычисляем координаты обрезки в пикселях исходного изображения
                            val actualCropRect = calculateActualCropRect(
                                uiRect = cropRect!!,
                                imageTopLeft = imageTopLeft!!,
                                displayedImageSize = displayedSize!!,
                                bitmapSize = IntSize(bitmap.width, bitmap.height),
                                imageScale = imageScale
                            )
                            
                            // Применяем обрезку через ImageEditProcessor
                            val croppedBitmap = imageEditProcessor.applyEdit(
                                bitmap = bitmap,
                                editType = com.example.neuralphotoredactor.domain.enums.EditType.CROP,
                                cropRect = actualCropRect
                            )
                            
                            if (croppedBitmap != null) {
                                onCropApply(croppedBitmap)
                            } else {
                                // Fallback: возвращаем исходное изображение
                                onCropApply(bitmap)
                            }
                        } else {
                            onCropApply(bitmap)
                        }
                    },
                    enabled = cropRect != null
                ) {
                    Text("Применить")
                }
            }
        }
    }
}

/**
 * Вычисляет координаты обрезки в пикселях исходного изображения.
 */
private fun calculateActualCropRect(
    uiRect: ComposeRect,
    imageTopLeft: Offset,
    displayedImageSize: IntSize,
    bitmapSize: IntSize,
    imageScale: Float
): Rect {
    // Преобразуем координаты UI (относительно контейнера) в координаты отображаемого изображения
    val relativeLeft = (uiRect.left - imageTopLeft.x).coerceIn(0f, displayedImageSize.width.toFloat())
    val relativeTop = (uiRect.top - imageTopLeft.y).coerceIn(0f, displayedImageSize.height.toFloat())
    val relativeRight = (uiRect.right - imageTopLeft.x).coerceIn(relativeLeft, displayedImageSize.width.toFloat())
    val relativeBottom = (uiRect.bottom - imageTopLeft.y).coerceIn(relativeTop, displayedImageSize.height.toFloat())
    
    // Преобразуем координаты отображаемого изображения в координаты исходного bitmap
    val left = (relativeLeft / imageScale).coerceIn(0f, bitmapSize.width.toFloat()).toInt()
    val top = (relativeTop / imageScale).coerceIn(0f, bitmapSize.height.toFloat()).toInt()
    val right = (relativeRight / imageScale).coerceIn(left.toFloat(), bitmapSize.width.toFloat()).toInt()
    val bottom = (relativeBottom / imageScale).coerceIn(top.toFloat(), bitmapSize.height.toFloat()).toInt()
    
    return Rect(left, top, right, bottom)
}

/**
 * Тип ручки для изменения размера области обрезки.
 */
private enum class CropHandle {
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT,
    TOP, BOTTOM, LEFT, RIGHT,
    CENTER
}

/**
 * Canvas для отображения overlay обрезки с интерактивными элементами.
 */
@Composable
private fun CropOverlayCanvas(
    cropRect: ComposeRect,
    imageTopLeft: Offset,
    displayedImageSize: IntSize,
    onCropRectChange: (ComposeRect) -> Unit,
    isDragging: Boolean,
    onDragStart: (CropHandle) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dragStartOffset by remember { mutableStateOf<Offset?>(null) }
    var initialCropRect by remember { mutableStateOf<ComposeRect?>(null) }
    var currentHandle by remember { mutableStateOf<CropHandle?>(null) }
    
    val handleSize = 24.dp
    val handleSizePx = with(LocalDensity.current) { handleSize.toPx() }
    val minCropSize = 50.dp
    val minCropSizePx = with(LocalDensity.current) { minCropSize.toPx() }
    
    // Адаптируем cropRect к координатам контейнера (с учетом смещения изображения)
    val adjustedCropRect = ComposeRect(
        left = cropRect.left + imageTopLeft.x,
        top = cropRect.top + imageTopLeft.y,
        right = cropRect.right + imageTopLeft.x,
        bottom = cropRect.bottom + imageTopLeft.y
    )
    
    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        // Определяем, на какую ручку нажали
                        val handle = getHandleAt(offset, adjustedCropRect, handleSizePx)
                        if (handle != null) {
                            currentHandle = handle
                            dragStartOffset = offset
                            initialCropRect = cropRect
                            onDragStart(handle)
                        }
                    },
                    onDrag = { change, _ ->
                        if (dragStartOffset != null && initialCropRect != null && currentHandle != null) {
                            val delta = change.position - dragStartOffset!!
                            val newRect = adjustCropRect(
                                initialCropRect!!,
                                currentHandle!!,
                                delta,
                                minCropSizePx,
                                displayedImageSize
                            )
                            onCropRectChange(newRect)
                        }
                    },
                    onDragEnd = {
                        dragStartOffset = null
                        initialCropRect = null
                        currentHandle = null
                        onDragEnd()
                    }
                )
            }
    ) {
        // Затемнение области вне обрезки
        val overlayColor = Color.Black.copy(alpha = 0.5f)
        
        // Верхняя область
        drawRect(
            color = overlayColor,
            topLeft = Offset(0f, 0f),
            size = Size(size.width, adjustedCropRect.top)
        )
        
        // Нижняя область
        drawRect(
            color = overlayColor,
            topLeft = Offset(0f, adjustedCropRect.bottom),
            size = Size(size.width, size.height - adjustedCropRect.bottom)
        )
        
        // Левая область
        drawRect(
            color = overlayColor,
            topLeft = Offset(0f, adjustedCropRect.top),
            size = Size(adjustedCropRect.left, adjustedCropRect.height)
        )
        
        // Правая область
        drawRect(
            color = overlayColor,
            topLeft = Offset(adjustedCropRect.right, adjustedCropRect.top),
            size = Size(size.width - adjustedCropRect.right, adjustedCropRect.height)
        )
        
        // Рамка области обрезки
        val borderColor = Color.White
        val borderWidth = 2.dp.toPx()
        
        // Верхняя линия
        drawLine(
            color = borderColor,
            start = Offset(adjustedCropRect.left, adjustedCropRect.top),
            end = Offset(adjustedCropRect.right, adjustedCropRect.top),
            strokeWidth = borderWidth
        )
        
        // Нижняя линия
        drawLine(
            color = borderColor,
            start = Offset(adjustedCropRect.left, adjustedCropRect.bottom),
            end = Offset(adjustedCropRect.right, adjustedCropRect.bottom),
            strokeWidth = borderWidth
        )
        
        // Левая линия
        drawLine(
            color = borderColor,
            start = Offset(adjustedCropRect.left, adjustedCropRect.top),
            end = Offset(adjustedCropRect.left, adjustedCropRect.bottom),
            strokeWidth = borderWidth
        )
        
        // Правая линия
        drawLine(
            color = borderColor,
            start = Offset(adjustedCropRect.right, adjustedCropRect.top),
            end = Offset(adjustedCropRect.right, adjustedCropRect.bottom),
            strokeWidth = borderWidth
        )
        
        // Ручки для изменения размера
        val handleColor = Color.White
        val handleStrokeWidth = 3.dp.toPx()
        
        // Угловые ручки
        drawCircle(handleColor, handleSizePx / 2, Offset(adjustedCropRect.left, adjustedCropRect.top))
        drawCircle(handleColor, handleSizePx / 2, Offset(adjustedCropRect.right, adjustedCropRect.top))
        drawCircle(handleColor, handleSizePx / 2, Offset(adjustedCropRect.left, adjustedCropRect.bottom))
        drawCircle(handleColor, handleSizePx / 2, Offset(adjustedCropRect.right, adjustedCropRect.bottom))
        
        // Боковые ручки
        drawCircle(handleColor, handleSizePx / 2, Offset(adjustedCropRect.center.x, adjustedCropRect.top))
        drawCircle(handleColor, handleSizePx / 2, Offset(adjustedCropRect.center.x, adjustedCropRect.bottom))
        drawCircle(handleColor, handleSizePx / 2, Offset(adjustedCropRect.left, adjustedCropRect.center.y))
        drawCircle(handleColor, handleSizePx / 2, Offset(adjustedCropRect.right, adjustedCropRect.center.y))
    }
}

/**
 * Определяет, на какую ручку нажали.
 */
private fun getHandleAt(
    offset: Offset,
    cropRect: ComposeRect,
    handleSize: Float
): CropHandle? {
    val halfHandle = handleSize / 2
    
    // Вспомогательная функция для расчета расстояния
    fun distance(p1: Offset, p2: Offset): Float {
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
    
    // Угловые ручки
    if (distance(offset, Offset(cropRect.left, cropRect.top)) < halfHandle) {
        return CropHandle.TOP_LEFT
    }
    if (distance(offset, Offset(cropRect.right, cropRect.top)) < halfHandle) {
        return CropHandle.TOP_RIGHT
    }
    if (distance(offset, Offset(cropRect.left, cropRect.bottom)) < halfHandle) {
        return CropHandle.BOTTOM_LEFT
    }
    if (distance(offset, Offset(cropRect.right, cropRect.bottom)) < halfHandle) {
        return CropHandle.BOTTOM_RIGHT
    }
    
    // Боковые ручки
    if (kotlin.math.abs(offset.x - cropRect.center.x) < halfHandle &&
        offset.y in (cropRect.top - halfHandle)..(cropRect.top + halfHandle)) {
        return CropHandle.TOP
    }
    if (kotlin.math.abs(offset.x - cropRect.center.x) < halfHandle &&
        offset.y in (cropRect.bottom - halfHandle)..(cropRect.bottom + halfHandle)) {
        return CropHandle.BOTTOM
    }
    if (kotlin.math.abs(offset.y - cropRect.center.y) < halfHandle &&
        offset.x in (cropRect.left - halfHandle)..(cropRect.left + halfHandle)) {
        return CropHandle.LEFT
    }
    if (kotlin.math.abs(offset.y - cropRect.center.y) < halfHandle &&
        offset.x in (cropRect.right - halfHandle)..(cropRect.right + halfHandle)) {
        return CropHandle.RIGHT
    }
    
    // Центр (для перемещения)
    if (offset.x in cropRect.left..cropRect.right &&
        offset.y in cropRect.top..cropRect.bottom) {
        return CropHandle.CENTER
    }
    
    return null
}

/**
 * Изменяет область обрезки в зависимости от типа ручки и смещения.
 */
private fun adjustCropRect(
    initialRect: ComposeRect,
    handle: CropHandle,
    delta: Offset,
    minSize: Float,
    displayedImageSize: IntSize
): ComposeRect {
    var left = initialRect.left
    var top = initialRect.top
    var right = initialRect.right
    var bottom = initialRect.bottom
    
    when (handle) {
        CropHandle.TOP_LEFT -> {
            left = (initialRect.left + delta.x).coerceIn(0f, right - minSize)
            top = (initialRect.top + delta.y).coerceIn(0f, bottom - minSize)
        }
        CropHandle.TOP_RIGHT -> {
            right = (initialRect.right + delta.x).coerceIn(left + minSize, displayedImageSize.width.toFloat())
            top = (initialRect.top + delta.y).coerceIn(0f, bottom - minSize)
        }
        CropHandle.BOTTOM_LEFT -> {
            left = (initialRect.left + delta.x).coerceIn(0f, right - minSize)
            bottom = (initialRect.bottom + delta.y).coerceIn(top + minSize, displayedImageSize.height.toFloat())
        }
        CropHandle.BOTTOM_RIGHT -> {
            right = (initialRect.right + delta.x).coerceIn(left + minSize, displayedImageSize.width.toFloat())
            bottom = (initialRect.bottom + delta.y).coerceIn(top + minSize, displayedImageSize.height.toFloat())
        }
        CropHandle.TOP -> {
            top = (initialRect.top + delta.y).coerceIn(0f, bottom - minSize)
        }
        CropHandle.BOTTOM -> {
            bottom = (initialRect.bottom + delta.y).coerceIn(top + minSize, displayedImageSize.height.toFloat())
        }
        CropHandle.LEFT -> {
            left = (initialRect.left + delta.x).coerceIn(0f, right - minSize)
        }
        CropHandle.RIGHT -> {
            right = (initialRect.right + delta.x).coerceIn(left + minSize, displayedImageSize.width.toFloat())
        }
        CropHandle.CENTER -> {
            val width = initialRect.width
            val height = initialRect.height
            left = (initialRect.left + delta.x).coerceIn(0f, displayedImageSize.width.toFloat() - width)
            top = (initialRect.top + delta.y).coerceIn(0f, displayedImageSize.height.toFloat() - height)
            right = left + width
            bottom = top + height
        }
    }
    
    return ComposeRect(left, top, right, bottom)
}
