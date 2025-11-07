# Структура проекта AI Image Editor

## Созданная структура

### Domain Layer (Бизнес-логика)
- **domain/model/**
  - `ImageData.kt` - модель изображения
  - `ProcessingRequest.kt` - запрос на обработку
  - `AIResult.kt` - результат AI обработки
  - `FilterPreset.kt` - предустановки фильтров

- **domain/enums/**
  - `FilterType.kt` - типы фильтров (on-device и cloud-based)
  - `ProcessingStatus.kt` - статусы обработки

- **domain/repository/**
  - `ImageRepository.kt` - интерфейс для работы с изображениями
  - `ProcessingRepository.kt` - интерфейс для обработки изображений
  - `FilterRepository.kt` - интерфейс для работы с фильтрами

- **domain/usecase/**
  - `ProcessImageUseCase.kt` - use case для обработки изображения
  - `GetProcessingHistoryUseCase.kt` - use case для получения истории
  - `GetAllFiltersUseCase.kt` - use case для получения всех фильтров

### Data Layer (Данные)
- **data/local/**
  - `entity/ProcessingHistoryEntity.kt` - Room entity для истории обработок
  - `dao/ProcessingHistoryDao.kt` - DAO для работы с историей
  - `database/AppDatabase.kt` - база данных Room

- **data/remote/**
  - `dto/ProcessingRequestDto.kt` - DTO для запроса обработки
  - `dto/ProcessingResponseDto.kt` - DTO для ответа обработки
  - `api/AIServiceApi.kt` - Retrofit API интерфейс

- **data/datasource/**
  - `CameraDataSource.kt` - интерфейс для работы с камерой
  - `CameraDataSourceImpl.kt` - реализация (placeholder)
  - `GalleryDataSource.kt` - интерфейс для работы с галереей
  - `GalleryDataSourceImpl.kt` - реализация (placeholder)

- **data/repository/**
  - `ImageRepositoryImpl.kt` - реализация ImageRepository
  - `ProcessingRepositoryImpl.kt` - реализация ProcessingRepository (placeholder)
  - `FilterRepositoryImpl.kt` - реализация FilterRepository (placeholder)

### Presentation Layer (UI)
- **presentation/ui/**
  - `components/` - папка для переиспользуемых Composables (пусто)
  - `screen/` - папка для экранов (пусто)
  - `theme/` - тема приложения (уже существует)

- **presentation/viewmodel/**
  - папка для ViewModels (пусто, будет создаваться при разработке экранов)

- **presentation/navigation/**
  - `NavGraph.kt` - определение экранов и навигации

- **presentation/state/**
  - `ImageEditorState.kt` - состояние редактора изображений

### Dependency Injection
- **di/**
  - `App.kt` - Application класс с @HiltAndroidApp
  - `DatabaseModule.kt` - модуль для Room базы данных
  - `NetworkModule.kt` - модуль для Retrofit и OkHttp
  - `RepositoryModule.kt` - модуль для репозиториев и datasources

## Подключенные зависимости

### Core
- Jetpack Compose
- Navigation Compose
- Lifecycle & ViewModel
- Coroutines

### Dependency Injection
- Dagger Hilt
- Hilt Navigation Compose

### Networking
- Retrofit
- OkHttp
- Gson

### Database
- Room Database

### Camera
- CameraX (core, camera2, lifecycle, view)

### Image Loading
- Coil Compose

### AI/ML
- TensorFlow Lite
- TensorFlow Lite GPU
- ML Kit Image Labeling

## Разрешения в AndroidManifest.xml
- INTERNET
- READ_EXTERNAL_STORAGE (для Android < 13)
- READ_MEDIA_IMAGES (для Android 13+)
- CAMERA
- WRITE_EXTERNAL_STORAGE (для Android < 10)

## Следующие шаги
1. Реализовать экраны в `presentation/ui/screen/`
2. Создать ViewModels в `presentation/viewmodel/`
3. Реализовать datasources (Camera, Gallery)
4. Реализовать ProcessingRepository с интеграцией Room и API
5. Добавить TensorFlow Lite модели для on-device обработки
6. Настроить навигацию между экранами

