# Анализ возможности тестирования ML слоя

## Структура ML слоя

ML слой состоит из следующих компонентов:

### 1. **Препроцессор** (`ImagePreprocessor` / `ImagePreprocessorImpl`)
- Преобразует Bitmap в TensorImage для TFLite
- Выполняет ресайз изображений
- Использует TensorFlow Lite Support Library

### 2. **Постпроцессор** (`ImagePostprocessor` / `ImagePostprocessorImpl`)
- Преобразует TensorImage обратно в Bitmap
- Выполняет ресайз до исходного размера
- Простая логика без зависимостей от моделей

### 3. **Процессор фильтров** (`ImageFilterProcessor` / `ImageFilterProcessorImpl`)
- Применяет алгоритмические фильтры (Gaussian Blur, Vignette, Grayscale, Sepia, Sharpen)
- Использует RenderEffect, ColorMatrix, Convolution
- Работает с Bitmap напрямую

### 4. **Процессор редактирования** (`ImageEditProcessor` / `ImageEditProcessorImpl`)
- Кадрирование, поворот, отражение
- Коррекция яркости, контраста, цветового баланса
- Использует ColorMatrix и Matrix трансформации

### 5. **TFLite процессоры** (6 классов: EsrganImageProcessor, AnimeGan2ImageProcessor и др.)
- Работают с реальными TFLite моделями
- Выполняют инференс через Interpreter
- Требуют загруженные модели из assets

### 6. **ModelLoader** (утилита)
- Загружает модели из assets и файловой системы
- Создает Interpreter из MappedByteBuffer
- Требует Context для работы с assets

---

## Возможности тестирования

### ✅ **ХОРОШО ТЕСТИРУЕМЫЕ КОМПОНЕНТЫ**

#### 1. **ImagePreprocessorImpl** ⭐⭐⭐⭐⭐
**Сложность тестирования:** Низкая  
**Зависимости:** TensorFlow Lite Support Library (можно использовать в тестах)

**Что можно тестировать:**
- Преобразование Bitmap в TensorImage
- Корректность ресайза до целевых размеров
- Валидация входных параметров (размеры, null bitmap)
- Различные форматы Bitmap (ARGB_8888, RGB_565)

**Подход:**
- Использовать Robolectric для Android окружения
- Создавать тестовые Bitmap разных размеров
- Проверять размеры TensorImage после обработки
- Проверять корректность данных в TensorImage

**Пример тестов:**
```kotlin
@Test
fun `preprocess should resize bitmap to target dimensions`() = runTest {
    val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    val result = preprocessor.preprocess(bitmap, 50, 50)
    assertEquals(50, result.bitmap.width)
    assertEquals(50, result.bitmap.height)
}
```

---

#### 2. **ImagePostprocessorImpl** ⭐⭐⭐⭐⭐
**Сложность тестирования:** Низкая  
**Зависимости:** TensorFlow Lite Support Library

**Что можно тестировать:**
- Преобразование TensorImage в Bitmap
- Ресайз до исходных размеров
- Обработка случаев, когда размеры совпадают
- Обработка случаев, когда размеры не совпадают

**Подход:**
- Создавать TensorImage с известными размерами
- Проверять корректность ресайза
- Тестировать edge cases (очень маленькие/большие изображения)

---

#### 3. **ImageEditProcessorImpl** ⭐⭐⭐⭐
**Сложность тестирования:** Средняя  
**Зависимости:** Android Graphics API (Canvas, ColorMatrix, Matrix)

**Что можно тестировать:**
- ✅ Кадрирование (crop) - валидация координат, обработка граничных случаев
- ✅ Поворот (90, 180, 270 градусов) - проверка размеров и ориентации
- ✅ Отражение (горизонтальное/вертикальное) - проверка зеркальности
- ✅ Коррекция яркости - проверка изменения яркости пикселей
- ✅ Коррекция контраста - проверка изменения контраста
- ✅ Цветовой баланс (R, G, B) - проверка изменения цветов
- Обработка ошибок (recycled bitmap, некорректные параметры)

**Подход:**
- Использовать Robolectric для Android окружения
- Создавать тестовые Bitmap с известными цветами
- Проверять изменения пикселей после обработки
- Тестировать валидацию входных данных

**Пример тестов:**
```kotlin
@Test
fun `applyEdit should rotate image 90 degrees`() = runTest {
    val bitmap = createTestBitmap(100, 200) // Портретная ориентация
    val result = processor.applyEdit(bitmap, EditType.ROTATE_90)
    assertNotNull(result)
    assertEquals(200, result?.width) // После поворота стало 200x100
    assertEquals(100, result?.height)
}

@Test
fun `applyEdit should adjust brightness correctly`() = runTest {
    val bitmap = createGrayBitmap(128) // Средне-серый
    val result = processor.applyEdit(bitmap, EditType.BRIGHTNESS, 0.5f)
    assertNotNull(result)
    // Проверяем, что изображение стало светлее
    val avgBrightness = calculateAverageBrightness(result!!)
    assertTrue(avgBrightness > 128)
}
```

**Ограничения:**
- Сложно проверить точные значения пикселей (зависит от реализации ColorMatrix)
- Можно проверять общие тенденции (светлее/темнее, больше/меньше контраста)

---

#### 4. **ImageFilterProcessorImpl (алгоритмические фильтры)** ⭐⭐⭐
**Сложность тестирования:** Средняя  
**Зависимости:** Android Graphics API (RenderEffect, ColorMatrix, Convolution)

**Что можно тестировать:**
- ✅ Применение фильтров (Gaussian Blur, Vignette, Grayscale, Sepia, Sharpen)
- ✅ Обработка параметра intensity
- ✅ Режим preview (уменьшение размера)
- ✅ Последовательное применение нескольких фильтров
- ✅ Обработка ошибок (recycled bitmap, null результат)
- ✅ Оптимизация сортировки фильтров

**Подход:**
- Использовать Robolectric для Android окружения
- Тестировать каждый тип фильтра отдельно
- Проверять, что результат не null и имеет правильные размеры
- Проверять влияние intensity на результат
- Тестировать edge cases (intensity = 0, 1, null)

**Пример тестов:**
```kotlin
@Test
fun `applyFilter should apply grayscale filter`() = runTest {
    val bitmap = createColorBitmap()
    val result = processor.applyFilter(bitmap, FilterType.GRAYSCALE)
    assertNotNull(result)
    assertEquals(bitmap.width, result?.width)
    assertEquals(bitmap.height, result?.height)
    // Проверяем, что изображение действительно в оттенках серого
    assertTrue(isGrayscale(result!!))
}

@Test
fun `applyFilter should respect intensity parameter`() = runTest {
    val bitmap = createTestBitmap()
    val result1 = processor.applyFilter(bitmap, FilterType.SEPIA, 0.5f)
    val result2 = processor.applyFilter(bitmap, FilterType.SEPIA, 1.0f)
    // Результаты должны отличаться
    assertNotEquals(calculateAverageColor(result1!!), calculateAverageColor(result2!!))
}
```

**Ограничения:**
- Сложно проверить точные значения пикселей (зависит от реализации фильтров)
- RenderEffect требует API 31+ и hardware acceleration (может не работать в тестах)
- Можно проверять общие свойства (размеры, не null, визуальные изменения)

---

### ⚠️ **СЛОЖНО ТЕСТИРУЕМЫЕ КОМПОНЕНТЫ**

#### 5. **TFLite Image Processors** (EsrganImageProcessor, AnimeGan2ImageProcessor и др.) ⭐⭐
**Сложность тестирования:** Высокая  
**Зависимости:** 
- Реальные TFLite модели из assets
- TensorFlow Lite Interpreter
- Модели требуют специфичных входных/выходных форматов

**Проблемы:**
1. **Требуют реальные модели** - модели находятся в assets и имеют большой размер
2. **Инференс медленный** - выполнение модели занимает время
3. **Результаты недетерминированы** - ML модели могут давать слегка разные результаты
4. **Требуют Context** - для загрузки моделей из assets
5. **Требуют специфичные размеры** - модели ожидают определенные размеры входных данных

**Что МОЖНО тестировать:**
- ✅ Валидацию входных параметров (null bitmap, recycled bitmap)
- ✅ Обработку случая, когда Interpreter = null
- ✅ Обработку ошибок при инференсе
- ✅ Логику разбиения на патчи (для EsrganImageProcessor)
- ✅ Параллельную обработку патчей
- ✅ Валидацию размеров патчей

**Что СЛОЖНО тестировать:**
- ❌ Реальные результаты инференса (требует модели)
- ❌ Качество обработки изображений
- ❌ Производительность инференса

**Подход (частичное тестирование):**
```kotlin
@Test
fun `processImage should return null when interpreter is null`() = runTest {
    val processor = EsrganImageProcessor(interpreter = null)
    val bitmap = createTestBitmap()
    val result = processor.processImage(bitmap, FilterType.UPSCALE)
    assertNull(result)
}

@Test
fun `processImage should validate patch size`() = runTest {
    // Можно проверить логику валидации размеров патчей
    // без реального инференса
}
```

**Рекомендация:**
- Тестировать только валидацию и обработку ошибок
- Реальные результаты инференса тестировать через интеграционные тесты или вручную

---

#### 6. **ModelLoader** ⭐
**Сложность тестирования:** Очень высокая  
**Зависимости:** 
- Context для работы с assets
- Реальные файлы моделей
- File I/O операции

**Проблемы:**
1. **Требует реальные файлы** - модели в assets
2. **Требует Context** - для доступа к assets
3. **Сложно мокировать** - FileChannel, MappedByteBuffer

**Что МОЖНО тестировать:**
- ✅ Обработку ошибок (файл не найден, некорректный формат)
- ✅ Валидацию входных параметров

**Рекомендация:**
- Не тестировать напрямую, так как это утилита низкого уровня
- Тестировать через компоненты, которые её используют (TFLiteModelRepository)

---

## Рекомендации по тестированию ML слоя

### Приоритет 1: Высокий (рекомендуется тестировать)
1. ✅ **ImagePreprocessorImpl** - простой, хорошо тестируемый
2. ✅ **ImagePostprocessorImpl** - простой, хорошо тестируемый
3. ✅ **ImageEditProcessorImpl** - важная бизнес-логика, хорошо тестируемый

### Приоритет 2: Средний (рекомендуется частичное тестирование)
4. ⚠️ **ImageFilterProcessorImpl** - тестировать алгоритмические фильтры, валидацию, обработку ошибок

### Приоритет 3: Низкий (опционально)
5. ⚠️ **TFLite Image Processors** - тестировать только валидацию и обработку ошибок
6. ❌ **ModelLoader** - не тестировать напрямую

---

## Технические требования для тестирования

### Зависимости (уже есть в проекте):
- ✅ Robolectric - для Android окружения
- ✅ JUnit 4/5 - для тестов
- ✅ MockK - для мокирования (если нужно)
- ✅ kotlinx-coroutines-test - для тестирования корутин
- ✅ TensorFlow Lite Support Library - доступна в тестах

### Дополнительные зависимости (могут понадобиться):
- AndroidX Test Core - для работы с Context в тестах
- Возможно, mockito для мокирования сложных Android классов

---

## Примерная структура тестов

```
app/src/test/java/com/example/neuralphotoredactor/ml/
├── preprocessor/
│   └── ImagePreprocessorImplTest.kt
├── postprocessor/
│   └── ImagePostprocessorImplTest.kt
├── edit/
│   └── ImageEditProcessorImplTest.kt
├── filter/
│   └── ImageFilterProcessorImplTest.kt
└── interpreter/
    ├── EsrganImageProcessorTest.kt (только валидация)
    └── ...
```

---

## Итоговая оценка

| Компонент | Тестируемость | Приоритет | Рекомендация |
|-----------|---------------|-----------|--------------|
| ImagePreprocessorImpl | ⭐⭐⭐⭐⭐ | Высокий | ✅ Тестировать полностью |
| ImagePostprocessorImpl | ⭐⭐⭐⭐⭐ | Высокий | ✅ Тестировать полностью |
| ImageEditProcessorImpl | ⭐⭐⭐⭐ | Высокий | ✅ Тестировать полностью |
| ImageFilterProcessorImpl | ⭐⭐⭐ | Средний | ⚠️ Тестировать частично |
| TFLite Processors | ⭐⭐ | Низкий | ⚠️ Тестировать только валидацию |
| ModelLoader | ⭐ | Низкий | ❌ Не тестировать |

**Вывод:** ML слой **можно и нужно** тестировать, но с разной степенью покрытия в зависимости от компонента. Основной фокус - на препроцессор, постпроцессор и процессор редактирования, так как они содержат важную бизнес-логику и хорошо тестируемы.

