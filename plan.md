# Plan: AI Image Editor App

## Архитектура
- **Паттерн**: MVVM + Clean Architecture
- **Язык**: Kotlin
- **Минимальная версия Android**: API 24 (Android 7.0)
- **Библиотеки**:
    - Jetpack Compose
    - CameraX (работа с камерой)
    - Room Database (история обработок)
    - Retrofit + OkHttp (API вызовы к AI сервисам)
    - Dagger Hilt (dependency injection)
    - Coil (загрузка изображений)
    - TensorFlow Lite (on-device AI модели)
    - ML Kit (Google AI функции)

## Структура пакетов
```
app/
├── data/
│ ├── local/ (Room entities, DAOs, история обработок)
│ ├── remote/ (API interfaces, DTOs для AI сервисов)
│ ├── repository/ (Repository implementations)
│ └── datasource/ (Camera, Gallery datasources)
├── domain/
│ ├── model/ (Business models: ImageFilter, ProcessingResult)
│ ├── repository/ (Repository interfaces)
│ ├── usecase/ (Use cases для операций с изображениями)
│ └── enums/ (FilterType, ProcessingStatus)
└── presentation/
├── ui/
│ ├── components/ (Reusable Composables)
│ ├── screen/ (Основные экраны)
│ └── theme/ (Custom theme)
├── viewmodel/ (ViewModels)
├── navigation/ (Navigation graph)
└── state/ (UI state classes)
```

## Основные экраны
1. **Gallery Screen** - выбор изображения из галереи или камеры
2. **Editor Screen** - панель инструментов и предпросмотр
3. **Filters Screen** - список AI фильтров и эффектов
4. **History Screen** - история обработок и сравнение
5. **Settings Screen** - настройки качества и API ключи

## AI Функции обработки
### On-Device (TensorFlow Lite)
- **Style Transfer** - перенос стиля с референсного изображения
- **Super Resolution** - увеличение разрешения
- **Background Removal** - удаление фона
- **Colorization** - раскрашивание черно-белых фото
- **Face Enhancement** - улучшение лиц

### Cloud-Based (API)
- **DeepArt Effects** - художественные фильтры
- **Background Replacement** - замена фона
- **Object Removal** - удаление объектов
- **AI Upscaling** - профессиональное увеличение

## Модели данных
```kotlin
// Основные модели
ImageData - исходное изображение + метаданные
ProcessingRequest - запрос на обработку
AIResult - результат AI обработки
FilterPreset - предустановки фильтров
```
## Цветовая схема
- Primary: #6750A4
- Secondary: #EADDFF
- Background: #FFFBFE