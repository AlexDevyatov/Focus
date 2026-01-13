# Схема базы данных

## Общая информация

База данных построена на основе реляционного подхода и оптимизирована для работы в среде мобильных устройств с использованием локальной базы данных SQLite через Android Room.

**Текущая версия БД:** 2  
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
| `timestamp` | Long | NOT NULL | Время обработки в миллисекундах |

**Примечание:** 
- Bitmap не хранится в БД, только URI изображений и метаданные.
- Информация о примененных фильтрах хранится в таблице `processing_operations` и может быть получена через `historyId`.

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
| `filterId` | Long | NOT NULL, INDEXED, FOREIGN KEY → filters.id | Ссылка на использованный фильтр (обязательное поле) |
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
- `filterId` → `filters.id` (ON DELETE RESTRICT) - фильтр нельзя удалить, если он используется в операциях

**Примечание:**
- Поле `filterId` является обязательным (NOT NULL) - каждая операция должна быть связана с фильтром
- Поле `operationType` было удалено в версии 2 БД - информация о типе операции хранится в связанном фильтре

**DAO:** `ProcessingOperationDao`

**Основные операции:**
- Получение всех операций для записи истории (отсортированных по sequenceNumber) - Flow и suspend версии
- Получение всех операций для сессии (отсортированных по sequenceNumber) - Flow и suspend версии
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
    timestamp = System.currentTimeMillis()
)
val historyId = processingHistoryDao.insert(history)

// Затем создаются операции с ссылкой на историю
// filterId должен быть NOT NULL - ссылается на фильтр из таблицы filters
val operation = ProcessingOperation(
    historyId = historyId,
    sessionId = sessionId,
    filterId = filterId, // NOT NULL - всегда должен быть фильтр или операция редактирования
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

**Тип связи:** Обязательная (NOT NULL Foreign Key)

**Описание:**
- Каждая операция обработки обязательно ссылается на один фильтр
- Поле `filterId` в `ProcessingOperationEntity` является обязательным (NOT NULL)
- При удалении фильтра операция не может быть удалена (ON DELETE RESTRICT) - фильтр нельзя удалить, если он используется в операциях
- Информация о типе операции хранится в связанном фильтре через поле `name`

**Пример использования:**
```kotlin
// Операция обработки связана с фильтром
val filterId = filterDao.getFilterByName("STYLE_TRANSFER")?.id
    ?: throw IllegalStateException("Фильтр не найден")

ProcessingOperationEntity(
    historyId = 1L,
    sessionId = 1L,
    filterId = filterId, // Обязательное поле - всегда должен быть фильтр
    parameters = """{"intensity": 0.8}""",
    inputImageUri = inputUri.toString(),
    outputImageUri = outputUri.toString(),
    processingTimeMs = 1234L,
    sequenceNumber = 1
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
│ filterId (FK → filters.id, NOT NULL)    │
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
│ timestamp               │
└─────────────────────────┘
```

---

## Версии базы данных

### Версия 2 (текущая)

**Дата:** Текущая версия

**Описание:** Обновленная схема базы данных

**Изменения по сравнению с версией 1:**
- Удалено поле `filterType` из таблицы `processing_history`
- Удалено поле `operationType` из таблицы `processing_operations`
- Поле `filterId` в таблице `processing_operations` стало обязательным (NOT NULL)
- Изменен тип связи `filterId` с `ON DELETE SET NULL` на `ON DELETE RESTRICT`

**Таблицы:**
- `processing_history` - история обработки изображений
- `processing_operations` - детальные операции обработки изображений, связанные с историей через `historyId`
- `filters` - фильтры с ссылкой на модели
- `neural_models` - метаинформация о нейросетевых моделях

**Связи:**
- `processing_operations.historyId` → `processing_history.id` (Foreign Key, ON DELETE CASCADE)
- `processing_operations.filterId` → `filters.id` (Foreign Key, NOT NULL, ON DELETE RESTRICT)
- `filters.modelId` → `neural_models.id` (Foreign Key, ON DELETE SET NULL)

**Индексы:**
- `index_processing_operations_historyId` на `processing_operations.historyId`
- `index_processing_operations_sessionId` на `processing_operations.sessionId`
- `index_processing_operations_filterId` на `processing_operations.filterId`
- `index_filters_modelId` на `filters.modelId`
- `index_filters_name` на `filters.name` (уникальный)

**Миграции:** 
- Используется `fallbackToDestructiveMigration()` для разработки
- При изменении версии БД данные будут удалены и база пересоздана
- Для production необходимо добавить миграции в `DatabaseModule`

---

### Версия 1 (устаревшая)

**Описание:** Первоначальная схема базы данных

**Таблицы:**
- `processing_history` - история обработки изображений (содержала поле `filterType`)
- `processing_operations` - детальные операции обработки изображений (содержала поле `operationType`, `filterId` был nullable)
- `filters` - фильтры с ссылкой на модели
- `neural_models` - метаинформация о нейросетевых моделях

**Изменения в версии 2:**
- Удалено поле `filterType` из `processing_history`
- Удалено поле `operationType` из `processing_operations`
- Поле `filterId` стало обязательным (NOT NULL)
- Изменен тип связи `filterId` с `ON DELETE SET NULL` на `ON DELETE RESTRICT`

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
- `getOperationsByHistoryId(historyId: Long): Flow<List<ProcessingOperationEntity>>` - получение операций по historyId (реактивный)
- `getOperationsByHistoryIdSuspend(historyId: Long): List<ProcessingOperationEntity>` - получение операций по historyId (suspend)
- `getOperationsBySessionId(sessionId: Long): Flow<List<ProcessingOperationEntity>>` - получение операций по sessionId (реактивный)
- `getOperationsBySessionIdSuspend(sessionId: Long): List<ProcessingOperationEntity>` - получение операций по sessionId (suspend)
- `getOperationById(id: Long): ProcessingOperationEntity?` - получение операции по ID
- `getLastOperationBySessionId(sessionId: Long): ProcessingOperationEntity?` - получение последней операции сессии
- `getMaxSequenceNumber(sessionId: Long): Int` - получение максимального порядкового номера для сессии
- `insert(operation: ProcessingOperationEntity): Long` - вставка одной операции
- `insertAll(operations: List<ProcessingOperationEntity>)` - вставка нескольких операций
- `delete(operation: ProcessingOperationEntity)` - удаление операции
- `deleteById(id: Long)` - удаление операции по ID
- `deleteBySessionId(sessionId: Long)` - удаление всех операций сессии

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

### Дополнительные методы AppDatabase

**Метод `saveHistoryWithOperations()`:**
- Сохраняет запись истории и связанные операции в одной транзакции
- Автоматически устанавливает `historyId` для всех операций
- Обеспечивает целостность данных при сохранении
- Используется в `ProcessingRepositoryImpl` для атомарного сохранения истории и операций

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
    timestamp = timestamp
)
val historyId = processingHistoryDao.insert(history)

// Затем создаются операции с ссылкой на историю
// filterId должен быть NOT NULL - ссылается на фильтр из таблицы filters
val filterId = filterDao.getFilterByName(filterType.name)?.id
    ?: throw IllegalStateException("Фильтр ${filterType.name} не найден в базе данных")
val operation = ProcessingOperationEntity(
    historyId = historyId,
    sessionId = timestamp,
    filterId = filterId, // NOT NULL - всегда должен быть фильтр
    parameters = """{"intensity": ${intensity ?: 1.0f}}""", // JSON строка
    inputImageUri = originalUri.toString(),
    outputImageUri = processedUri.toString(),
    processingTimeMs = processingTime,
    sequenceNumber = 1
)
processingOperationDao.insert(operation)
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
// Получаем фильтр (обязательно должен существовать)
val filterId = filterDao.getFilterByName("STYLE_TRANSFER")?.id
    ?: throw IllegalStateException("Фильтр STYLE_TRANSFER не найден в базе данных")

val operation = ProcessingOperationEntity(
    historyId = historyId,
    sessionId = currentSessionId,
    filterId = filterId, // Обязательное поле - NOT NULL
    parameters = """{"intensity": 0.8}""",
    inputImageUri = inputUri.toString(),
    outputImageUri = outputUri.toString(),
    processingTimeMs = 1234L,
    sequenceNumber = nextSequenceNumber
)

processingOperationDao.insert(operation)
```

### Сохранение истории с операциями в транзакции

```kotlin
// Использование метода AppDatabase.saveHistoryWithOperations()
val history = ProcessingHistoryEntity(
    originalUri = originalUri.toString(),
    processedUri = processedUri.toString(),
    timestamp = System.currentTimeMillis()
)

val operations = listOf(
    ProcessingOperationEntity(
        id = 0, // Будет установлен автоматически
        historyId = 0, // Будет установлен автоматически
        sessionId = sessionId,
        filterId = filterId,
        parameters = """{"intensity": 1.0}""",
        inputImageUri = originalUri.toString(),
        outputImageUri = processedUri.toString(),
        processingTimeMs = processingTime,
        sequenceNumber = 1
    )
)

val historyId = appDatabase.saveHistoryWithOperations(history, operations)
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
   - Для атомарного сохранения истории и операций используется метод `AppDatabase.saveHistoryWithOperations()`

3. **Таблица filters**
   - Таблица `filters` содержит информацию о всех доступных фильтрах
   - Фильтры автоматически заполняются при первом запуске приложения через `InitializeFiltersUseCase`
   - Фильтры могут быть алгоритмическими (без модели) или использовать нейросетевые модели
   - Связь между фильтрами и моделями позволяет легко определить, какой фильтр использует какую модель

4. **JSON параметры**
   - Поле `parameters` в `ProcessingOperationEntity` хранит JSON строку с параметрами операции
   - Поле `compatibilityLevel` в `NeuralModelEntity` также может быть JSON строкой
   - Парсинг JSON выполняется на уровне Repository или Domain

5. **Обязательность filterId**
   - Поле `filterId` в `ProcessingOperationEntity` является обязательным (NOT NULL)
   - Каждая операция обработки должна быть связана с фильтром из таблицы `filters`
   - Информация о типе операции хранится в связанном фильтре через поле `name`
   - При удалении фильтра операция не может быть удалена (ON DELETE RESTRICT) - это защищает от потери данных

5. **Индексы**
   - Индексы на `historyId`, `sessionId` и `filterId` оптимизируют запросы по связям
   - Индексы создаются автоматически Room через аннотации
