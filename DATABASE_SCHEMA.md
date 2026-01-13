# Схема базы данных

## Общая информация

База данных построена на основе реляционного подхода и оптимизирована для работы в среде мобильных устройств с использованием локальной базы данных SQLite через Android Room.

**Текущая версия БД:** 1  
**Имя файла БД:** `neural_photo_redactor_db`

---

## Структура сущностей

### 1. ProcessingHistoryEntity (История обработки)

**Таблица:** `processing_history`

Основная таблица для хранения истории обработки изображений. Используется для отображения истории обработок в UI (`HistoryScreen`) и сохранения результатов обработки изображений.

**Поля:**

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|----------|
| `id` | Long | PRIMARY KEY, AUTOINCREMENT | Уникальный идентификатор записи |
| `originalUri` | String | NOT NULL | URI исходного изображения |
| `processedUri` | String | NOT NULL | URI обработанного изображения |
| `filterType` | String | NOT NULL | Тип примененного фильтра |
| `timestamp` | Long | NOT NULL | Время обработки в миллисекундах |

**Примечание:** Bitmap не хранится в БД, только URI изображений и метаданные.

**DAO:** `ProcessingHistoryDao`

**Основные операции:**
- Получение всей истории (отсортированной по времени)
- Вставка новой записи
- Удаление записи по ID
- Поиск по URI и timestamp
- Получение записи по ID

---

### 2. ProcessingOperationEntity (Операция обработки)

**Таблица:** `processing_operations`

Фиксирует каждое отдельное действие, выполненное пользователем в рамках сессии редактирования. Связана с записью истории через `historyId`.

**Поля:**

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|----------|
| `id` | Long | PRIMARY KEY, AUTOINCREMENT | Уникальный идентификатор операции |
| `historyId` | Long | NOT NULL, INDEXED, FOREIGN KEY → processing_history.id | Ссылка на запись в истории обработки |
| `sessionId` | Long | NOT NULL, INDEXED | Идентификатор сессии редактирования (логическая связь, без Foreign Key) |
| `filterId` | Long? | NULLABLE, INDEXED, FOREIGN KEY → filters.id | Ссылка на использованный фильтр |
| `operationType` | String | NOT NULL | Тип операции (например: "style_transfer", "super_resolution") |
| `parameters` | String | NOT NULL | Параметры выполнения в формате JSON |
| `inputImageUri` | String | NOT NULL | URI входного изображения |
| `outputImageUri` | String | NOT NULL | URI выходного изображения |
| `processingTimeMs` | Long | NOT NULL | Время обработки в миллисекундах |
| `sequenceNumber` | Int | NOT NULL | Порядковый номер операции в истории изменений (для сортировки) |

**Индексы:**
- `index_processing_operations_historyId` - на поле `historyId` (для быстрого поиска операций записи истории)
- `index_processing_operations_sessionId` - на поле `sessionId` (для быстрого поиска операций сессии)
- `index_processing_operations_filterId` - на поле `filterId` (для связей с фильтрами)

**Foreign Keys:**
- `historyId` → `processing_history.id` (ON DELETE CASCADE)
- `filterId` → `filters.id` (ON DELETE SET NULL)

**DAO:** `ProcessingOperationDao`

**Основные операции:**
- Получение всех операций для записи истории (отсортированных по sequenceNumber)
- Получение всех операций для сессии (отсортированных по sequenceNumber)
- Получение операции по ID
- Получение последней операции для сессии
- Получение максимального порядкового номера для сессии
- Вставка операции(й)
- Удаление операций (по ID или по sessionId)

---

### 3. FilterEntity (Фильтр)

**Таблица:** `filters`

Содержит информацию о доступных фильтрах для обработки изображений. Фильтры могут быть алгоритмическими (без модели) или использовать нейросетевые модели.

**Поля:**

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|----------|
| `id` | Long | PRIMARY KEY, AUTOINCREMENT | Уникальный идентификатор фильтра |
| `name` | String | NOT NULL, UNIQUE | Название фильтра (соответствует FilterType enum) |
| `modelId` | Long? | NULLABLE, INDEXED, FOREIGN KEY → neural_models.id | Ссылка на нейросетевую модель (если фильтр использует модель) |

**Индексы:**
- `index_filters_modelId` - на поле `modelId` (для связей с моделями)
- `index_filters_name` - на поле `name` (уникальный индекс)

**Foreign Keys:**
- `modelId` → `neural_models.id` (ON DELETE SET NULL)

**DAO:** `FilterDao`

**Основные операции:**
- Получение всех фильтров
- Получение фильтра по ID или имени
- Вставка фильтра(ов)

**Инициализация:**
Фильтры автоматически заполняются при первом запуске приложения через `InitializeFiltersUseCase`. Список фильтров:
- GAUSSIAN_BLUR (алгоритмический)
- NOISE_REDUCTION (алгоритмический)
- SHARPEN (алгоритмический)
- VIGNETTE (алгоритмический)
- GRAYSCALE (алгоритмический)
- SEPIA (алгоритмический)
- STYLE_TRANSFER (использует модель "AnimeGAN2 Paprika")
- DENOISE (использует модель "SplitterNet")
- UPSCALE (использует модель "ESRGAN")
- COLOR_CORRECTION (алгоритмический)

---

### 4. NeuralModelEntity (Нейросетевая модель)

**Таблица:** `neural_models`

Содержит метаинформацию о доступных нейросетевых моделях искусственного интеллекта для обработки изображений.

**Поля:**

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|----------|
| `id` | Long | PRIMARY KEY, AUTOINCREMENT | Уникальный идентификатор модели |
| `name` | String | NOT NULL | Название модели (например: "AnimeGAN2 Paprika", "ESRGAN") |
| `type` | String | NOT NULL | Тип модели ("style_transfer", "super_resolution", "filter" и т.д.) |
| `version` | String | NOT NULL | Версия модели |
| `filePath` | String | NOT NULL | Путь к файлу модели (.tflite) в хранилище устройства |
| `fileSize` | Long | NOT NULL | Размер модели в байтах |
| `isActive` | Boolean | NOT NULL, DEFAULT TRUE | Флаг активности модели (неактивные модели не используются) |
| `compatibilityLevel` | String | NOT NULL | Уровень совместимости с различными устройствами (JSON строка или enum значение) |

**DAO:** `NeuralModelDao`

**Основные операции:**
- Получение всех активных моделей
- Получение всех моделей (включая неактивные)
- Получение моделей по типу
- Получение модели по ID или имени
- Вставка модели(ей)
- Обновление модели
- Удаление модели
- Активация/деактивация модели

---

## Отношения между сущностями

### 1. ProcessingHistory → ProcessingOperation (один ко многим)

**Тип связи:** Обязательная (Foreign Key с CASCADE)

**Описание:**
- Каждая запись истории обработки может иметь несколько операций обработки
- Поле `historyId` в `ProcessingOperationEntity` обязательно и ссылается на `processing_history.id`
- При удалении записи истории все связанные операции автоматически удаляются (ON DELETE CASCADE)
- Связь позволяет получать все операции обработки для конкретной записи истории

**Особенности:**
- `ProcessingHistoryEntity` хранит упрощенную информацию для быстрого отображения в UI
- `ProcessingOperationEntity` хранит детальную информацию о каждой операции (параметры, время обработки, фильтр и т.д.)
- При сохранении изображения сначала создается запись в `processing_history`, затем операции в `processing_operations` с ссылкой на историю

**Использование:**
- Основная рабочая таблица для отображения истории в UI (`HistoryScreen`)
- Сохраняется после каждой обработки изображения через `ProcessingRepositoryImpl`
- Все операции для записи истории можно получить через `ProcessingOperationDao.getOperationsByHistoryId(historyId)`

**Пример использования:**
```kotlin
// При обработке изображения сначала создается запись в истории
val history = ProcessingHistoryEntity(
    originalUri = originalUri.toString(),
    processedUri = processedUri.toString(),
    filterType = filterType.name,
    timestamp = System.currentTimeMillis()
)
val historyId = processingHistoryDao.insert(history)

// Затем создаются операции с ссылкой на историю
val operation = ProcessingOperation(
    historyId = historyId,
    sessionId = sessionId,
    filterId = filterId,
    operationType = filterType.name,
    parameters = OperationParameters(...),
    inputImageUri = originalUri,
    outputImageUri = processedUri,
    processingTimeMs = processingTime,
    sequenceNumber = 1
)
processingOperationRepository.addOperation(operation)
```

---

### 2. ProcessingOperation → Filter (многие к одному)

**Тип связи:** Необязательная (nullable Foreign Key)

**Описание:**
- Каждая операция обработки может ссылаться на один фильтр
- Поле `filterId` в `ProcessingOperationEntity` может быть NULL (если операция не использует фильтр)
- При удалении фильтра поле `filterId` в связанных операциях устанавливается в NULL (ON DELETE SET NULL)

**Пример использования:**
```kotlin
// Операция обработки связана с фильтром
ProcessingOperationEntity(
    historyId = 1L,
    sessionId = 1L,
    filterId = 5L, // Ссылка на FilterEntity с id=5
    operationType = "STYLE_TRANSFER",
    ...
)

// Операция обработки без фильтра
ProcessingOperationEntity(
    historyId = 1L,
    sessionId = 1L,
    filterId = null, // Операция не использует фильтр
    operationType = "edit",
    ...
)
```

---

### 3. Filter → NeuralModel (многие к одному)

**Тип связи:** Необязательная (nullable Foreign Key)

**Описание:**
- Каждый фильтр может ссылаться на одну нейросетевую модель
- Поле `modelId` в `FilterEntity` может быть NULL (для алгоритмических фильтров)
- При удалении модели поле `modelId` в связанных фильтрах устанавливается в NULL (ON DELETE SET NULL)

**Пример использования:**
```kotlin
// Фильтр с моделью
FilterEntity(
    name = "STYLE_TRANSFER",
    modelId = 1L // Ссылка на NeuralModelEntity с id=1 (AnimeGAN2 Paprika)
)

// Алгоритмический фильтр без модели
FilterEntity(
    name = "GAUSSIAN_BLUR",
    modelId = null // Алгоритмический фильтр, не использует модель
)
```

---

### 4. ProcessingOperation.sessionId (логическая связь)

**Тип связи:** Логическая (без Foreign Key на уровне БД)

**Описание:**
- Поле `sessionId` в `ProcessingOperationEntity` используется для группировки операций по сессиям редактирования
- Связь не реализована через Foreign Key (нет таблицы сессий в БД)
- `sessionId` - это логический идентификатор, создаваемый на уровне приложения
- Индекс на `sessionId` обеспечивает быстрый поиск операций для конкретной сессии

**Примечание:** В будущем может быть добавлена таблица `editing_sessions` с Foreign Key, но на текущий момент это логическая связь.

---

## Диаграмма связей

```
┌─────────────────────────┐
│   NeuralModelEntity     │
│   (neural_models)       │
│─────────────────────────│
│ id (PK)                 │
│ name                    │
│ type                    │
│ version                 │
│ filePath                │
│ fileSize                │
│ isActive                │
│ compatibilityLevel      │
└───────────┬─────────────┘
            │
            │ (1)
            │
            │ (ON DELETE SET NULL)
            │
            │ (0..*)
┌───────────▼─────────────┐
│   FilterEntity          │
│   (filters)             │
│─────────────────────────│
│ id (PK)                 │
│ name (UNIQUE)           │
│ modelId (FK)            │
└───────────┬─────────────┘
            │
            │ (1)
            │
            │ (ON DELETE SET NULL)
            │
            │ (0..*)
┌───────────▼──────────────────────────────┐
│   ProcessingOperationEntity              │
│   (processing_operations)                │
│──────────────────────────────────────────│
│ id (PK)                                  │
│ historyId (FK → processing_history.id)  │
│ sessionId (INDEXED, логическая связь)   │
│ filterId (FK → filters.id)              │
│ operationType                            │
│ parameters                               │
│ inputImageUri                            │
│ outputImageUri                           │
│ processingTimeMs                         │
│ sequenceNumber                           │
└──────────────────────────────────────────┘
            │
            │ (0..*)
            │
            │ (ON DELETE CASCADE)
            │
            │ (1)
┌───────────▼─────────────┐
│ ProcessingHistoryEntity │
│ (processing_history)     │
│─────────────────────────│
│ id (PK)                 │
│ originalUri             │
│ processedUri            │
│ filterType              │
│ timestamp               │
└─────────────────────────┘
```

---

## Версии базы данных

### Версия 1 (текущая, первоначальная)

**Дата:** Текущая версия

**Описание:** Первоначальная схема базы данных

**Таблицы:**
- `processing_history` - история обработки изображений
- `processing_operations` - детальные операции обработки изображений, связанные с историей через `historyId`
- `filters` - фильтры с ссылкой на модели
- `neural_models` - метаинформация о нейросетевых моделях

**Связи:**
- `processing_operations.historyId` → `processing_history.id` (Foreign Key, ON DELETE CASCADE)
- `processing_operations.filterId` → `filters.id` (Foreign Key, ON DELETE SET NULL)
- `filters.modelId` → `neural_models.id` (Foreign Key, ON DELETE SET NULL)

**Индексы:**
- `index_processing_operations_historyId` на `processing_operations.historyId`
- `index_processing_operations_sessionId` на `processing_operations.sessionId`
- `index_processing_operations_filterId` на `processing_operations.filterId`
- `index_filters_modelId` на `filters.modelId`
- `index_filters_name` на `filters.name` (уникальный)

**Миграции:** Миграции не используются, схема является первоначальной

---

## DAO интерфейсы

### ProcessingHistoryDao

Работа с историей обработки (основная рабочая таблица).

**Основные методы:**
- `getAllHistory(): Flow<List<ProcessingHistoryEntity>>`
- `insert(entity: ProcessingHistoryEntity): Long`
- `delete(entity: ProcessingHistoryEntity)`
- `deleteById(id: Long)`
- `findByUriAndTimestamp(processedUri: String, timestamp: Long): ProcessingHistoryEntity?`
- `getHistoryById(id: Long): ProcessingHistoryEntity?`

### ProcessingOperationDao

Работа с операциями обработки.

**Основные методы:**
- `getOperationsByHistoryId(historyId: Long): Flow<List<ProcessingOperationEntity>>`
- `getOperationsByHistoryIdSuspend(historyId: Long): List<ProcessingOperationEntity>`
- `getOperationsBySessionId(sessionId: Long): Flow<List<ProcessingOperationEntity>>`
- `getOperationById(id: Long): ProcessingOperationEntity?`
- `getLastOperationBySessionId(sessionId: Long): ProcessingOperationEntity?`
- `getMaxSequenceNumber(sessionId: Long): Int`
- `insert(operation: ProcessingOperationEntity): Long`
- `insertAll(operations: List<ProcessingOperationEntity>)`
- `delete(operation: ProcessingOperationEntity)`
- `deleteById(id: Long)`
- `deleteBySessionId(sessionId: Long)`

### FilterDao

Работа с фильтрами.

**Основные методы:**
- `getAllFilters(): Flow<List<FilterEntity>>`
- `getFilterById(id: Long): FilterEntity?`
- `getFilterByName(name: String): FilterEntity?`
- `insert(filter: FilterEntity): Long`
- `insertAll(filters: List<FilterEntity>)`

### NeuralModelDao

Работа с нейросетевыми моделями.

**Основные методы:**
- `getAllActiveModels(): Flow<List<NeuralModelEntity>>`
- `getAllModels(): Flow<List<NeuralModelEntity>>`
- `getModelsByType(type: String): Flow<List<NeuralModelEntity>>`
- `getModelById(id: Long): NeuralModelEntity?`
- `getModelByName(name: String): NeuralModelEntity?`
- `insert(model: NeuralModelEntity): Long`
- `insertAll(models: List<NeuralModelEntity>)`
- `update(model: NeuralModelEntity)`
- `delete(model: NeuralModelEntity)`
- `deleteById(id: Long)`
- `setActive(id: Long, isActive: Boolean)`

---

## Архитектурные принципы

### Расположение в проекте

```
app/src/main/java/com/example/neuralphotoredactor/
├── data/
│   └── local/
│       ├── database/
│       │   └── AppDatabase.kt          # Главный класс базы данных
│       ├── dao/
│       │   ├── ProcessingHistoryDao.kt
│       │   ├── ProcessingOperationDao.kt
│       │   ├── FilterDao.kt
│       │   └── NeuralModelDao.kt
│       └── entity/
│           ├── ProcessingHistoryEntity.kt
│           ├── ProcessingOperationEntity.kt
│           ├── FilterEntity.kt
│           └── NeuralModelEntity.kt
└── di/
    └── DatabaseModule.kt                # DI модуль для БД
```

### Принципы работы

1. **Запрет хранения Bitmap в БД**
   - В базе данных хранятся только URI изображений и метаданные
   - Сами изображения хранятся в файловой системе устройства

2. **Разделение слоев**
   - Domain слой не зависит от Room
   - UI не вызывает DAO напрямую
   - Вся работа с БД идет через Repository (data слой)

3. **Использование Flow**
   - DAO методы возвращают `Flow<T>` для реактивного обновления UI
   - Поддержка suspend функций для синхронных операций

4. **Миграции**
   - Все изменения схемы требуют миграций
   - Миграции документируются в `DatabaseModule`
   - Версия БД увеличивается при каждом изменении схемы

---

## Примеры использования

### Сохранение операции обработки и истории

```kotlin
// При обработке изображения сначала создается запись в истории
val timestamp = System.currentTimeMillis()
val history = ProcessingHistoryEntity(
    originalUri = originalUri.toString(),
    processedUri = processedUri.toString(),
    filterType = filterType.name,
    timestamp = timestamp
)
val historyId = processingHistoryDao.insert(history)

// Затем создаются операции с ссылкой на историю
val filterId = filterDao.getFilterByName(filterType.name)?.id
val operation = ProcessingOperation(
    historyId = historyId,
    sessionId = timestamp,
    filterId = filterId,
    operationType = filterType.name,
    parameters = OperationParameters(
        filterType = filterType.name,
        intensity = intensity ?: 1.0f
    ),
    inputImageUri = originalUri,
    outputImageUri = processedUri,
    processingTimeMs = processingTime,
    sequenceNumber = 1
)
processingOperationRepository.addOperation(operation)
```

### Получение всех операций для записи истории

```kotlin
// Получаем запись истории
val history = processingHistoryDao.getHistoryById(historyId)

// Получаем все операции для этой записи истории
val operations = processingOperationDao.getOperationsByHistoryId(historyId)
    .collect { operationsList ->
        // Обработка всех операций для записи истории
    }
```

### Получение операций сессии

```kotlin
// В Repository
val operations = processingOperationDao.getOperationsBySessionId(sessionId)
    .collect { operationsList ->
        // Обработка операций
    }
```

### Создание операции с фильтром

```kotlin
val filterId = filterDao.getFilterByName("STYLE_TRANSFER")?.id
val operation = ProcessingOperationEntity(
    historyId = historyId,
    sessionId = currentSessionId,
    filterId = filterId,
    operationType = "STYLE_TRANSFER",
    parameters = """{"intensity": 0.8}""",
    inputImageUri = inputUri.toString(),
    outputImageUri = outputUri.toString(),
    processingTimeMs = 1234L,
    sequenceNumber = nextSequenceNumber
)

processingOperationDao.insert(operation)
```

### Получение активных моделей по типу

```kotlin
val styleModels = neuralModelDao.getModelsByType("style_transfer")
    .collect { models ->
        // Отображение доступных моделей стилизации
    }
```

---

## Дополнительные замечания

1. **sessionId как логический идентификатор**
   - `sessionId` не имеет Foreign Key на отдельную таблицу сессий
   - Это логический идентификатор, создаваемый на уровне приложения
   - Используется для группировки операций одной сессии редактирования

2. **Использование таблиц**
   - Таблица `processing_history` - основная рабочая таблица, активно используется в приложении для отображения истории обработок в UI (`HistoryScreen`)
   - Таблица `processing_operations` - таблица для детального отслеживания операций, интегрирована в бизнес-логику приложения
   - При сохранении изображения сначала создается запись в `processing_history`, затем операции в `processing_operations` с ссылкой на историю через `historyId`
   - Это позволяет получать как упрощенную информацию для UI, так и детальную информацию об операциях при необходимости

3. **Таблица filters**
   - Таблица `filters` содержит информацию о всех доступных фильтрах
   - Фильтры автоматически заполняются при первом запуске приложения через `InitializeFiltersUseCase`
   - Фильтры могут быть алгоритмическими (без модели) или использовать нейросетевые модели
   - Связь между фильтрами и моделями позволяет легко определить, какой фильтр использует какую модель

4. **JSON параметры**
   - Поле `parameters` в `ProcessingOperationEntity` хранит JSON
   - Поле `compatibilityLevel` в `NeuralModelEntity` также может быть JSON
   - Парсинг JSON выполняется на уровне Repository или Domain

5. **Индексы**
   - Индексы на `historyId`, `sessionId` и `filterId` оптимизируют запросы по связям
   - Индексы создаются автоматически Room через аннотации
