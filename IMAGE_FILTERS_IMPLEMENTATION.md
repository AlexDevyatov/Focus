# Реализация модуля фильтров изображений

## ✅ Полная реализация завершена

Модуль фильтров изображений реализован с соблюдением Clean Architecture и всех требований проекта.

---

## 📋 Реализованные фильтры

### 1. Gaussian Blur (Размытие по Гауссу)
- **Технология:** Алгоритмический подход (Convolution с ядром Гаусса)
- **Параметры:** Интенсивность (0.0 - 1.0, соответствует радиусу 0-10px)
- **Реализация:** `applyGaussianBlurAlgorithmic()`
- **Ядро:** Динамически генерируемое ядро Гаусса

### 2. Noise Reduction (Удаление шумов)
- **Технология:** Медианный фильтр
- **Параметры:** Интенсивность (0.0 - 1.0, размер ядра 1-5)
- **Реализация:** `applyMedianFilter()`
- **Алгоритм:** Медиана по каждому цветовому каналу

### 3. Sharpen (Резкость / Unsharp Mask)
- **Технология:** Convolution фильтр
- **Параметры:** Интенсивность (0.0 - 1.0, сила 0-2.0)
- **Реализация:** `applySharpen()` с ядром резкости
- **Ядро:** 3x3 ядро для повышения резкости

### 4. Vignette (Виньетка)
- **Технология:** Алгоритмический подход (пиксельная обработка)
- **Параметры:** Интенсивность (0.0 - 1.0)
- **Реализация:** `applyVignetteAlgorithmic()`
- **Алгоритм:** Радиальное затемнение от центра к краям

### 5. Grayscale (Чёрно-белое)
- **Технология:** ColorMatrix
- **Параметры:** Нет (фиксированный эффект)
- **Реализация:** `applyGrayscale()`
- **Метод:** Удаление насыщенности через ColorMatrix

### 6. Sepia (Сепия)
- **Технология:** ColorMatrix
- **Параметры:** Интенсивность (0.0 - 1.0)
- **Реализация:** `applySepia()`
- **Метод:** Комбинация grayscale + сепия тонирование

---

## 🏗️ Архитектура

### ML Layer (`ml/filter/`)

#### ImageFilterProcessor (интерфейс)
```kotlin
interface ImageFilterProcessor {
    fun applyFilter(bitmap: Bitmap, filterType: FilterType, intensity: Float?): Bitmap?
}
```

#### ImageFilterProcessorImpl (реализация)
- Все фильтры реализованы в одном классе
- Использует различные технологии в зависимости от фильтра
- Полностью оффлайн, без сетевых вызовов

**Технологии:**
- **ColorMatrix** - для Grayscale и Sepia
- **Convolution** - для Gaussian Blur и Sharpen
- **Median Filter** - для Noise Reduction
- **Pixel Processing** - для Vignette

### Data Layer

#### ProcessingRepositoryImpl
- Определяет, какой процессор использовать (ImageFilterProcessor или ImageProcessor)
- Новые фильтры используют ImageFilterProcessor
- Старые ML-фильтры используют ImageProcessor (требуют TFLite модели)

### Domain Layer

#### FilterType (enum)
- Добавлены новые типы фильтров
- Старые типы сохранены для обратной совместимости

### DI

#### MLModule
- Добавлен биндинг `ImageFilterProcessor` → `ImageFilterProcessorImpl`

---

## 🔄 Поток обработки

```
UI: Пользователь выбирает фильтр
    ↓
ViewModel: applyFilter(filterType)
    ↓
UseCase: ProcessImageUseCase.invoke()
    ↓
Repository: ProcessingRepositoryImpl.processImage()
    ↓
Определение процессора:
    - Новые фильтры → ImageFilterProcessor
    - ML-фильтры → ImageProcessor
    ↓
ML Layer: applyFilter() или processImage()
    ├─ Загрузка Bitmap из URI
    ├─ Применение фильтра
    └─ Возврат обработанного Bitmap
    ↓
Repository: Сохранение результата
    ↓
ViewModel: Обновление state
    ↓
UI: Отображение результата
```

---

## 📝 Детали реализации фильтров

### Gaussian Blur

**Алгоритм:**
1. Генерация ядра Гаусса заданного радиуса
2. Нормализация ядра
3. Применение convolution фильтра

**Ядро Гаусса:**
```kotlin
val value = exp(-(x² + y²) / (2σ²))
// где σ = radius / 3
```

### Noise Reduction

**Алгоритм:**
1. Для каждого пикселя собираем соседей в окне kernelSize×kernelSize
2. Вычисляем медиану по каждому каналу (R, G, B)
3. Заменяем пиксель на медианное значение

### Sharpen

**Ядро резкости:**
```
[  0  -s   0 ]
[ -s  1+4s -s ]
[  0  -s   0 ]
```
где s = strength (интенсивность)

### Vignette

**Алгоритм:**
1. Вычисляем расстояние от центра до каждого пикселя
2. Применяем квадратичное затухание от центра к краям
3. Умножаем цвет пикселя на фактор виньетки

**Формула:**
```kotlin
vignetteFactor = 1.0 - intensity * t²
// где t = (dist - startRadius) / (maxRadius - startRadius)
```

### Grayscale

**ColorMatrix:**
```kotlin
setSaturation(0f) // Убирает насыщенность
```

### Sepia

**ColorMatrix:**
1. Сначала применяем grayscale
2. Затем применяем сепия матрицу:
```
[ 0.393  0.769  0.189  0  0 ]
[ 0.349  0.686  0.168  0  0 ]
[ 0.272  0.534  0.131  0  0 ]
[ 0      0      0      1  0 ]
[ 0      0      0      0  1 ]
```

---

## ✅ Соблюдение требований

### 1. ML pipeline в ml/ слое ✅
- Все фильтры реализованы в `ml/filter/` пакете
- Bitmap обработка изолирована в ML слое
- Никаких TensorBuffer в слоях выше

### 2. Repository ✅
- Предоставляет методы для передачи изображений в ML слой
- Возвращает обработанное RGB изображение (через URI)
- Координирует работу ML и Storage

### 3. Domain слой ✅
- UseCase содержит только бизнес-логику
- Не знает про детали реализации фильтров
- Работает только с domain моделями

### 4. ViewModel ✅
- Получает результат из UseCase
- Передает UI состояние (успех/ошибка)
- НЕТ Context, ресурсов, Android зависимостей

### 5. UI слой ✅
- Jetpack Compose + Material Design 3
- Отображает финальное изображение
- НЕ выполняет обработку изображений напрямую

### 6. Документация ✅
- Все публичные классы документированы через KDoc

### 7. Функциональность ✅
- Без сетевых вызовов (полностью оффлайн)
- Минимальные изменения проекта

---

## 🚀 Использование

### В UI:

```kotlin
val viewModel: EditorViewModel = hiltViewModel()

// Применение фильтра
viewModel.applyFilter(FilterType.GAUSSIAN_BLUR)
viewModel.applyFilter(FilterType.SHARPEN)
viewModel.applyFilter(FilterType.VIGNETTE)
// и т.д.
```

### Доступные фильтры:

- `FilterType.GAUSSIAN_BLUR` - Размытие
- `FilterType.NOISE_REDUCTION` - Удаление шумов
- `FilterType.SHARPEN` - Резкость
- `FilterType.VIGNETTE` - Виньетка
- `FilterType.GRAYSCALE` - Чёрно-белое
- `FilterType.SEPIA` - Сепия

---

## 📦 Структура файлов

```
ml/
└── filter/
    ├── ImageFilterProcessor.kt ✅ (интерфейс)
    └── ImageFilterProcessorImpl.kt ✅ (реализация)

domain/
└── enums/
    └── FilterType.kt ✅ (обновлен)

data/
└── repository/
    └── ProcessingRepositoryImpl.kt ✅ (обновлен)

di/
└── MLModule.kt ✅ (обновлен)
```

---

## 🎯 Готово к использованию

Все фильтры реализованы и готовы к использованию. Проект компилируется без ошибок.

**Для использования:**
1. Выберите изображение в галерее
2. Выберите фильтр из списка
3. Результат отобразится автоматически

---

**Модуль фильтров реализован! ✅**

