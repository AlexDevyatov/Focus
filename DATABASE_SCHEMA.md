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
| `filterType` | String | NOT NULL | Тип примененного фильтра |
| `timestamp` | Long | NOT NULL | Время обработки в миллисекундах |

**Примечание:** Bitmap не хранится в БД, только URI изображений и метаданные.

**DAO:** `ProcessingHistoryDao`

**Основные операции:**
- Получение всей истории (отсортированной по времени)
- Вставка новой записи
- Удаление записи по ID
- Поиск по URI и timestamp

---

### 2. ProcessingOperationEntity (Операция обработки)

**Таблица:** `processing_operations`

Фиксирует каждое отдельное действие, выполненное пользователем в рамках сессии редактирования.

**Поля:**

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|----------|
| `id` | Long | PRIMARY KEY, AUTOINCREMENT | Уникальный идентификатор операции |
| `sessionId` | Long | NOT NULL, INDEXED | Идентификатор сессии редактирования (логическая связь, без Foreign Key) |
| `modelId` | Long? | NULLABLE, INDEXED, FOREIGN KEY → neural_models.id | Ссылка на использованную нейросетевую модель |
| `operationType` | String | NOT NULL | Тип операции (например: "style_transfer", "super_resolution") |
| `parameters` | String | NOT NULL | Параметры выполнения в формате JSON |
| `inputImageUri` | String | NOT NULL | URI входного изображения |
| `outputImageUri` | String | NOT NULL | URI выходного изображения |
| `processingTimeMs` | Long | NOT NULL | Время обработки в миллисекундах |
| `sequenceNumber` | Int | NOT NULL | Порядковый номер операции в истории изменений (для сортировки) |

**Индексы:**
- `index_processing_operations_sessionId` - на поле `sessionId` (для быстрого поиска операций сессии)
- `index_processing_operations_modelId` - на поле `modelId` (для связей с моделями)

**Foreign Keys:**
- `modelId` → `neural_models.id` (ON DELETE SET NULL)

**DAO:** `ProcessingOperationDao`

**Основные операции:**
- Получение всех операций для сессии (отсортированных по sequenceNumber)
- Получение операции по ID
- Получение последней операции для сессии
- Получение максимального порядкового номера для сессии
- Вставка операции(й)
- Удаление операций (по ID или по sessionId)

---

### 3. NeuralModelEntity (Нейросетевая модель)

**Таблица:** `neural_models`

Содержит метаинформацию о доступных нейросетевых моделях искусственного интеллекта для обработки изображений.

**Поля:**

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|----------|
| `id` | Long | PRIMARY KEY, AUTOINCREMENT | Уникальный идентификатор модели |
| `name` | String | NOT NULL | Название модели (например: "AnimeGAN2", "ESRGAN") |
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

### 1. ProcessingHistoryEntity (независимая таблица)

**Тип связи:** Нет Foreign Keys, независимая таблица

**Описание:**
- `ProcessingHistoryEntity` **не имеет Foreign Keys** и не связана напрямую с другими сущностями на уровне базы данных
- Это независимая таблица, которая хранит финальные результаты обработки изображений
- Используется для отображения истории обработок в UI (`HistoryScreen`)
- Хранит упрощенную информацию: URI исходного и обработанного изображения, тип фильтра и timestamp

**Особенности:**
- Нет связей с `ProcessingOperationEntity` или `NeuralModelEntity` на уровне БД
- Может содержать похожую информацию с `ProcessingOperationEntity`, но с другой структурой данных
- Предназначена для быстрого доступа к истории обработок без сложных JOIN-запросов

**Использование:**
- Основная рабочая таблица для отображения истории в приложении
- Сохраняется после каждой обработки изображения через `ProcessingRepositoryImpl`

---

### 2. ProcessingOperation → NeuralModel (многие к одному)

**Тип связи:** Необязательная (nullable Foreign Key)

**Описание:**
- Каждая операция обработки может ссылаться на одну нейросетевую модель
- Поле `modelId` в `ProcessingOperationEntity` может быть NULL (если операция не использует модель)
- При удалении модели (`NeuralModelEntity`) поле `modelId` в связанных операциях устанавливается в NULL (ON DELETE SET NULL)

**Пример использования:**
```kotlin
// Операция обработки связана с моделью
ProcessingOperationEntity(
    sessionId = 1L,
    modelId = 5L, // Ссылка на NeuralModelEntity с id=5
    operationType = "style_transfer",
    ...
)

// Операция обработки без модели
ProcessingOperationEntity(
    sessionId = 1L,
    modelId = null, // Операция не использует модель
    operationType = "rotate",
    ...
)
```

### 3. ProcessingOperation.sessionId (логическая связь)

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
┌───────────▼──────────────────────────────┐
│   ProcessingOperationEntity              │
│   (processing_operations)                │
│──────────────────────────────────────────│
│ id (PK)                                  │
│ sessionId (INDEXED, логическая связь)   │
│ modelId (FK → neural_models.id)         │
│ operationType                            │
│ parameters                               │
│ inputImageUri                            │
│ outputImageUri                           │
│ processingTimeMs                         │
│ sequenceNumber                           │
└──────────────────────────────────────────┘

┌─────────────────────────┐
│ ProcessingHistoryEntity │
│ (processing_history)     │
│─────────────────────────│
│ id (PK)                 │
│ originalUri             │
│ processedUri            │
│ filterType              │
│ timestamp               │
└─────────────────────────┘

(независимая таблица, нет Foreign Keys)
```

---

## Версии базы данных

### Версия 2 (текущая)

**Дата:** Текущая версия

**Изменения:**
- Добавлена таблица `neural_models` для хранения метаинформации о нейросетевых моделях
- Добавлена таблица `processing_operations` для детального отслеживания операций обработки
- Добавлены индексы на `sessionId` и `modelId` в таблице `processing_operations`
- Создан Foreign Key между `processing_operations.modelId` и `neural_models.id`

**Миграция:** `MIGRATION_1_2` в `DatabaseModule`

### Версия 1

**Описание:** Начальная схема базы данных

**Таблицы:**
- `processing_history` - история обработки изображений (основная рабочая таблица)

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

### ProcessingOperationDao

Работа с операциями обработки.

**Основные методы:**
- `getOperationsBySessionId(sessionId: Long): Flow<List<ProcessingOperationEntity>>`
- `getOperationById(id: Long): ProcessingOperationEntity?`
- `getLastOperationBySessionId(sessionId: Long): ProcessingOperationEntity?`
- `getMaxSequenceNumber(sessionId: Long): Int`
- `insert(operation: ProcessingOperationEntity): Long`
- `insertAll(operations: List<ProcessingOperationEntity>)`
- `delete(operation: ProcessingOperationEntity)`
- `deleteById(id: Long)`
- `deleteBySessionId(sessionId: Long)`

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
│       │   └── NeuralModelDao.kt
│       └── entity/
│           ├── ProcessingHistoryEntity.kt
│           ├── ProcessingOperationEntity.kt
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

### Получение операций сессии

```kotlin
// В Repository
val operations = processingOperationDao.getOperationsBySessionId(sessionId)
    .collect { operationsList ->
        // Обработка операций
    }
```

### Создание операции с моделью

```kotlin
val operation = ProcessingOperationEntity(
    sessionId = currentSessionId,
    modelId = model.id,
    operationType = "style_transfer",
    parameters = """{"strength": 0.8}""",
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
   - Таблица `processing_history` - основная рабочая таблица, активно используется в приложении для отображения истории обработок
   - Таблица `processing_operations` - дополнительная таблица для детального отслеживания операций (инфраструктура готова, но пока не интегрирована в основной UI)

3. **JSON параметры**
   - Поле `parameters` в `ProcessingOperationEntity` хранит JSON
   - Поле `compatibilityLevel` в `NeuralModelEntity` также может быть JSON
   - Парсинг JSON выполняется на уровне Repository или Domain

4. **Индексы**
   - Индексы на `sessionId` и `modelId` оптимизируют запросы по связям
   - Индексы создаются автоматически Room через аннотации
