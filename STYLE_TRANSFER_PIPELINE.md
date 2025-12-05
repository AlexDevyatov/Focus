# Pipeline для применения художественного стиля к фотографии

## ✅ Полная реализация завершена

Pipeline реализован с соблюдением Clean Architecture и всех требований проекта.

---

## 🔄 Полный Pipeline

```
Контентное изображение (Bitmap)
    +
Изображение стиля (Bitmap)
    ↓
┌─────────────────────────────────────────┐
│  ML Layer (StyleTransferProcessor)      │
│                                         │
│  1. Preprocessing                       │
│     ├─ Контент: Bitmap → TensorImage   │
│     └─ Стиль: Bitmap → TensorImage     │
│     (Ресайз до 256x256, BILINEAR)      │
│                                         │
│  2. Style Prediction                    │
│     ├─ Загрузка prediction модели      │
│     ├─ Инференс: styleImage → features │
│     └─ Результат: ByteBuffer (стиль)   │
│                                         │
│  3. Style Transfer                      │
│     ├─ Загрузка transfer модели        │
│     ├─ Инференс: content + style       │
│     └─ Результат: TensorImage           │
│                                         │
│  4. Postprocessing                      │
│     ├─ TensorImage → Bitmap            │
│     └─ Ресайз до исходных размеров     │
└─────────────────────────────────────────┘
    ↓
Финальное изображение (Bitmap)
    ↓
Сохранение через ImageStorage
    ↓
StyleTransferResult (URI)
```

---

## 📋 Детальное описание компонентов

### 1. ML Layer - StyleTransferProcessor

**Расположение:** `ml/interpreter/StyleTransferProcessor.kt`

**Ответственность:**
- Полный ML pipeline для стилизации
- Работа с TFLite моделями
- Preprocessing и Postprocessing

**Методы:**

#### `applyStyle(contentBitmap: Bitmap, styleBitmap: Bitmap): Bitmap?`

**Входные данные:**
- `contentBitmap`: Контентное изображение (RGB Bitmap)
- `styleBitmap`: Изображение стиля (RGB Bitmap)

**Процесс:**

1. **Загрузка моделей:**
   ```kotlin
   val predictionModel = modelManager.loadModel("style_prediction.tflite")
   val transferModel = modelManager.loadModel("style_transfer.tflite")
   ```

2. **Preprocessing (для обоих изображений):**
   ```kotlin
   val styleImage = preprocessor.preprocess(styleBitmap, 256, 256)
   val contentImage = preprocessor.preprocess(contentBitmap, 256, 256)
   ```
   - Ресайз до размеров модели (256x256)
   - Преобразование Bitmap → TensorImage
   - Нормализация (если требуется)

3. **Style Prediction:**
   ```kotlin
   val styleFeatures = extractStyleFeatures(styleBitmap, predictionModel, 256, 256)
   ```
   - Инференс prediction модели
   - Извлечение стилевого вектора (ByteBuffer)
   - Выход: стилевые признаки

4. **Style Transfer:**
   ```kotlin
   val styledBitmap = applyStyleToContent(
       contentBitmap, 
       styleFeatures, 
       transferModel, 
       256, 256, 
       originalWidth, 
       originalHeight
   )
   ```
   - Инференс transfer модели
   - Вход: контентное изображение + стилевой вектор
   - Выход: стилизованное TensorImage

5. **Postprocessing:**
   ```kotlin
   val finalBitmap = postprocessor.postprocess(outputImage, originalWidth, originalHeight)
   ```
   - Преобразование TensorImage → Bitmap
   - Ресайз до исходных размеров контентного изображения
   - Возврат финального Bitmap

**Выходные данные:**
- Стилизованное изображение (RGB Bitmap) или null в случае ошибки

---

### 2. Preprocessing - ImagePreprocessor

**Расположение:** `ml/preprocessor/ImagePreprocessorImpl.kt`

**Функция:**
```kotlin
fun preprocess(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): TensorImage
```

**Процесс:**
1. Создание ImageProcessor с ResizeOp (BILINEAR)
2. Преобразование Bitmap → TensorImage
3. Применение ресайза до целевых размеров
4. Возврат TensorImage готового для инференса

**Пример:**
```kotlin
val imageProcessor = ImageProcessor.Builder()
    .add(ResizeOp(targetHeight, targetWidth, ResizeOp.ResizeMethod.BILINEAR))
    .build()

val tensorImage = TensorImage.fromBitmap(bitmap)
return imageProcessor.process(tensorImage)
```

---

### 3. Postprocessing - ImagePostprocessor

**Расположение:** `ml/postprocessor/ImagePostprocessorImpl.kt`

**Функция:**
```kotlin
fun postprocess(tensorImage: TensorImage, originalWidth: Int, originalHeight: Int): Bitmap
```

**Процесс:**
1. Извлечение Bitmap из TensorImage
2. Проверка размеров
3. Ресайз до исходных размеров (если необходимо)
4. Возврат финального Bitmap

**Пример:**
```kotlin
var bitmap = tensorImage.bitmap

if (bitmap.width != originalWidth || bitmap.height != originalHeight) {
    bitmap = Bitmap.createScaledBitmap(
        bitmap,
        originalWidth,
        originalHeight,
        true // BILINEAR filtering
    )
}

return bitmap
```

---

### 4. Data Layer - StyleTransferRepositoryImpl

**Расположение:** `data/repository/StyleTransferRepositoryImpl.kt`

**Ответственность:**
- Загрузка Bitmap из URI
- Вызов ML pipeline
- Сохранение результата
- Возврат domain модели

**Метод:**
```kotlin
suspend fun applyStyleTransfer(
    contentImage: ImageData,
    styleImage: ImageData
): StyleTransferResult?
```

**Процесс:**
1. Загрузка Bitmap из URI (ContentResolver)
2. Вызов `styleTransferProcessor.applyStyle(contentBitmap, styleBitmap)`
3. Сохранение результата через `ImageStorage`
4. Возврат `StyleTransferResult` с URI результата

**Важно:**
- Вся работа с Bitmap происходит в data слое
- Domain слой видит только ImageData и StyleTransferResult
- ML операции делегируются в ml/ слой

---

### 5. Domain Layer - ApplyStyleTransferUseCase

**Расположение:** `domain/usecase/ApplyStyleTransferUseCase.kt`

**Ответственность:**
- Бизнес-логика применения стиля
- Валидация входных данных
- Координация работы с репозиторием

**Метод:**
```kotlin
suspend operator fun invoke(
    contentImage: ImageData,
    styleImage: ImageData
): StyleTransferResult?
```

**Процесс:**
1. Валидация URI изображений
2. Вызов репозитория
3. Возврат результата

**Важно:**
- Не знает про TFLite, Bitmap, TensorBuffer
- Работает только с domain моделями
- Содержит только бизнес-логику

---

### 6. ViewModel - EditorViewModel

**Расположение:** `ui/viewmodel/EditorViewModel.kt`

**Ответственность:**
- Управление состоянием UI
- Координация UseCase
- Передача результатов в UI

**Метод:**
```kotlin
fun applyStyleTransfer() {
    val contentImage = _uiState.value.contentImage
    val styleImage = _uiState.value.styleImage
    
    viewModelScope.launch {
        val result = applyStyleTransferUseCase.invoke(contentImage, styleImage)
        _uiState.value = _uiState.value.copy(styledResult = result)
    }
}
```

**Состояние:**
```kotlin
data class EditorUiState(
    val contentImage: ImageData?,
    val styleImage: ImageData?,
    val styledResult: StyleTransferResult?,
    val isLoading: Boolean,
    val error: String?
)
```

**Важно:**
- НЕТ Context, Resources, Android зависимостей
- Работает только с domain моделями и UseCase
- Ошибки передаются как e.message, UI добавляет дефолтное сообщение

---

### 7. UI Layer - EditorScreen

**Расположение:** `ui/screen/EditorScreen.kt`

**Ответственность:**
- Отображение UI для стилизации
- Выбор изображений
- Отображение результата

**Компоненты:**
- `StyleTransferContent()` - контент для стилизации
- `ImageSelectionCard()` - карточка выбора изображения
- Отображение результата через `AsyncImage`

**Важно:**
- НЕ выполняет обработку изображений напрямую
- Получает состояние через ViewModel
- Использует Material Design 3 компоненты

---

## 🔄 Полный поток данных

```
1. UI: Пользователь выбирает изображения
   ↓
2. ViewModel: setContentImage() / setStyleImage()
   ↓
3. UI: Пользователь нажимает "Применить стиль"
   ↓
4. ViewModel: applyStyleTransfer()
   ↓
5. UseCase: ApplyStyleTransferUseCase.invoke()
   ↓
6. Repository: StyleTransferRepositoryImpl.applyStyleTransfer()
   ↓
7. Data: Загрузка Bitmap из URI (ContentResolver)
   ↓
8. ML: StyleTransferProcessor.applyStyle()
   │
   ├─ Preprocessing
   │  ├─ contentBitmap → TensorImage (256x256)
   │  └─ styleBitmap → TensorImage (256x256)
   │
   ├─ Style Prediction
   │  ├─ Загрузка prediction модели
   │  ├─ Инференс: styleImage → styleFeatures
   │  └─ Результат: ByteBuffer
   │
   ├─ Style Transfer
   │  ├─ Загрузка transfer модели
   │  ├─ Инференс: contentImage + styleFeatures → styledImage
   │  └─ Результат: TensorImage
   │
   └─ Postprocessing
      ├─ TensorImage → Bitmap
      └─ Ресайз до исходных размеров
   ↓
9. Data: Сохранение результата (ImageStorage)
   ↓
10. Repository: Возврат StyleTransferResult
   ↓
11. UseCase: Возврат результата
   ↓
12. ViewModel: Обновление state (styledResult)
   ↓
13. UI: Отображение результата (AsyncImage)
```

---

## ✅ Соблюдение требований

### 1. ML pipeline в ml/ слое ✅
- Все операции с TFLite в `ml/` пакете
- Bitmap preprocessing/postprocessing в ML слое
- Никаких TensorBuffer в слоях выше

### 2. Repository ✅
- Предоставляет методы для передачи изображений в ML слой
- Возвращает обработанное RGB изображение (через URI)
- Координирует работу ML и Storage

### 3. Domain слой ✅
- UseCase содержит только бизнес-логику
- Не знает про TFLite и Bitmap
- Работает только с domain моделями

### 4. ViewModel ✅
- Получает результат из UseCase
- Передает UI состояние (успех/ошибка) и готовое изображение (URI)
- НЕТ Context, ресурсов, Android зависимостей

### 5. UI слой ✅
- Jetpack Compose + Material Design 3
- Отображает финальное изображение
- НЕ выполняет обработку изображений напрямую

### 6. Документация ✅
- Все публичные классы документированы через KDoc

### 7. Функциональность ✅
- Без сетевых вызовов (полностью оффлайн)
- Без изменения TFLite моделей
- Минимальные изменения проекта

---

## 📦 Структура файлов

```
ml/
├── interpreter/
│   └── StyleTransferProcessor.kt ✅ (полный pipeline)
├── preprocessor/
│   ├── ImagePreprocessor.kt ✅
│   └── ImagePreprocessorImpl.kt ✅
├── postprocessor/
│   ├── ImagePostprocessor.kt ✅
│   └── ImagePostprocessorImpl.kt ✅
└── util/
    ├── ModelManager.kt ✅
    └── ModelLoader.kt ✅

data/
└── repository/
    └── StyleTransferRepositoryImpl.kt ✅

domain/
├── model/
│   ├── StyleTransferRequest.kt ✅
│   └── StyleTransferResult.kt ✅
├── repository/
│   └── StyleTransferRepository.kt ✅
└── usecase/
    └── ApplyStyleTransferUseCase.kt ✅

ui/
├── screen/
│   └── EditorScreen.kt ✅ (расширен)
└── viewmodel/
    └── EditorViewModel.kt ✅ (расширен)
```

---

## 🎯 Готово к использованию

Pipeline полностью реализован и готов к использованию. Все компоненты интегрированы и следуют принципам Clean Architecture.

**Для использования:**
1. Добавьте модели в `app/src/main/assets/`:
   - `style_prediction.tflite` или `arbitrary_image_stylization_256_int8_prediction.tflite`
   - `style_transfer.tflite` или `arbitrary_image_stylization_256_int8_transfer.tflite`

2. Выберите изображения в UI
3. Нажмите "Применить стиль"
4. Результат отобразится автоматически

---

**Pipeline реализован! ✅**

