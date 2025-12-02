# Новый функционал для работы с сессиями редактирования

## Обзор

Добавлен полный функционал для работы с новой логической моделью данных, включающей:
- **EditingSession** (Сессии редактирования)
- **ProcessingOperation** (Операции обработки)
- **NeuralModel** (Нейросетевые модели)

## Структура

### Domain модели

1. **EditingSession** - Сессия редактирования изображения
   - Содержит URI исходного и текущего изображения
   - Метаданные (разрешение, формат, EXIF)

2. **ProcessingOperation** - Операция обработки
   - Связь с сессией и моделью
   - Параметры операции
   - Временные метки и порядковый номер

3. **NeuralModel** - Нейросетевая модель
   - Тип модели (стилизация, супер-разрешение и т.д.)
   - Метаданные модели
   - Уровень совместимости

### Репозитории

1. **EditingSessionRepository** - Управление сессиями
   - Создание, обновление, удаление сессий
   - Получение сессий по ID или всех сессий

2. **ProcessingOperationRepository** - Управление операциями
   - Добавление операций обработки
   - Получение операций по сессии
   - Управление историей операций

3. **NeuralModelRepository** - Управление моделями
   - Добавление и обновление моделей
   - Получение моделей по типу
   - Активация/деактивация моделей

### UseCase

1. **CreateEditingSessionUseCase** - Создание новой сессии
2. **GetAllEditingSessionsUseCase** - Получение всех сессий
3. **GetEditingSessionByIdUseCase** - Получение сессии по ID
4. **AddProcessingOperationUseCase** - Добавление операции обработки
5. **GetOperationsBySessionIdUseCase** - Получение операций сессии
6. **GetAllNeuralModelsUseCase** - Получение всех активных моделей
7. **AddNeuralModelUseCase** - Добавление новой модели

## Использование

### Пример создания сессии редактирования

```kotlin
@Inject lateinit var createSessionUseCase: CreateEditingSessionUseCase

val metadata = SessionMetadata(
    width = 1920,
    height = 1080,
    format = "JPEG",
    exifData = mapOf("ISO" to "400", "Exposure" to "1/60")
)

val sessionId = createSessionUseCase.invoke(
    originalImageUri = imageUri,
    metadata = metadata
)
```

### Пример добавления операции обработки

```kotlin
@Inject lateinit var addOperationUseCase: AddProcessingOperationUseCase

val operationId = addOperationUseCase.invoke(
    sessionId = sessionId,
    modelId = modelId,
    operationType = "FILTER",
    parameters = OperationParameters(
        filterType = "STYLE_TRANSFER",
        intensity = 0.8f
    ),
    inputImageUri = inputUri,
    outputImageUri = outputUri,
    processingTimeMs = 1500
)
```

### Пример получения всех сессий

```kotlin
@Inject lateinit var getAllSessionsUseCase: GetAllEditingSessionsUseCase

viewModelScope.launch {
    getAllSessionsUseCase.invoke.collect { sessions ->
        // Обработка списка сессий
    }
}
```

## Dependency Injection

Все репозитории и UseCase автоматически инжектируются через Hilt:
- `EditingSessionRepository` → `EditingSessionRepositoryImpl`
- `ProcessingOperationRepository` → `ProcessingOperationRepositoryImpl`
- `NeuralModelRepository` → `NeuralModelRepositoryImpl`

## База данных

Все данные хранятся в Room Database:
- Таблица `editing_sessions`
- Таблица `processing_operations`
- Таблица `neural_models`

Связи между таблицами:
- `processing_operations.sessionId` → `editing_sessions.id` (CASCADE)
- `processing_operations.modelId` → `neural_models.id` (SET NULL)

