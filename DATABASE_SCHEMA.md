# Схема базы данных

## Логическая модель данных

База данных построена на основе реляционного подхода и оптимизирована для работы в среде мобильных устройств с использованием локальной базы данных SQLite через Android Room.

### Структура сущностей

#### 1. EditingSession (Сессия редактирования) - Ключевая сущность

Таблица: `editing_sessions`

Представляет рабочую сессию пользователя с одним изображением.

**Поля:**
- `id` (Long, Primary Key, AutoGenerate) - Уникальный идентификатор сессии
- `originalImageUri` (String) - URI исходного изображения
- `currentImageUri` (String) - URI текущего состояния изображения после примененных операций
- `createdAt` (Long) - Временная метка создания сессии (миллисекунды)
- `updatedAt` (Long) - Временная метка последнего изменения сессии (миллисекунды)
- `metadata` (String) - Метаданные сессии в формате JSON (разрешение, формат файла, EXIF-данные)

#### 2. ProcessingOperation (Операция обработки)

Таблица: `processing_operations`

Фиксирует каждое отдельное действие, выполненное пользователем в рамках сессии.

**Поля:**
- `id` (Long, Primary Key, AutoGenerate) - Уникальный идентификатор операции
- `sessionId` (Long, Foreign Key -> EditingSession) - Связь с сессией редактирования
- `modelId` (Long?, Foreign Key -> NeuralModel) - Ссылка на использованную нейросетевую модель
- `operationType` (String) - Тип операции
- `parameters` (String) - Параметры выполнения в формате JSON
- `inputImageUri` (String) - URI входного изображения
- `outputImageUri` (String) - URI выходного изображения
- `processingTimeMs` (Long) - Время обработки в миллисекундах
- `sequenceNumber` (Int) - Порядковый номер операции в истории изменений

**Индексы:**
- `index_processing_operations_sessionId` - на поле `sessionId`
- `index_processing_operations_modelId` - на поле `modelId`

#### 3. NeuralModel (Нейросетевая модель)

Таблица: `neural_models`

Содержит метаинформацию о доступных моделях искусственного интеллекта.

**Поля:**
- `id` (Long, Primary Key, AutoGenerate) - Уникальный идентификатор модели
- `name` (String) - Название модели
- `type` (String) - Тип модели (стилизация, супер-разрешение и т.д.)
- `version` (String) - Версия модели
- `filePath` (String) - Путь к файлу модели в хранилище устройства
- `fileSize` (Long) - Размер модели в байтах
- `isActive` (Boolean) - Флаг активности модели
- `compatibilityLevel` (String) - Уровень совместимости с различными устройствами

### Отношения между сущностями

1. **EditingSession → ProcessingOperation** (один ко многим)
   - Одна сессия редактирования может содержать множество операций обработки
   - Foreign Key с каскадным удалением (ON DELETE CASCADE)

2. **ProcessingOperation → NeuralModel** (многие к одному)
   - Каждая операция обработки использует одну конкретную нейросетевую модель
   - Foreign Key с установкой NULL при удалении (ON DELETE SET NULL)

### Версии базы данных

- **Версия 2** - Добавлены таблицы `editing_sessions`, `processing_operations`, `neural_models`
- **Версия 1** - Начальная схема с таблицей `processing_history` (для обратной совместимости)

### DAO интерфейсы

- `EditingSessionDao` - CRUD операции для сессий редактирования
- `ProcessingOperationDao` - CRUD операции для операций обработки
- `NeuralModelDao` - CRUD операции для нейросетевых моделей
- `ProcessingHistoryDao` - CRUD операции для старой таблицы истории (обратная совместимость)

