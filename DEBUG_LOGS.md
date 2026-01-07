# Инструкция по просмотру логов для отладки

## Как открыть Logcat в Android Studio

1. **В Android Studio:**
   - Внизу экрана найдите вкладку **"Logcat"**
   - Если её нет: `View` → `Tool Windows` → `Logcat`
   - Или используйте горячую клавишу: `Alt + 6` (Windows/Linux) или `Cmd + 6` (Mac)

2. **Через терминал (adb):**
   ```bash
   adb logcat
   ```

## Фильтры для отладки цветокоррекции

### Основные теги для фильтрации:

1. **MirNetProcessor** - логи процессора MIRNet
   ```
   tag:MirNetProcessor
   ```

2. **ProcessingRepository** - логи репозитория обработки
   ```
   tag:ProcessingRepository
   ```

3. **EditorViewModel** - логи ViewModel
   ```
   tag:EditorViewModel
   ```

4. **MLModule** - логи загрузки модели
   ```
   tag:MLModule
   ```

### Комбинированный фильтр (все важные логи):
```
tag:MirNetProcessor | tag:ProcessingRepository | tag:EditorViewModel | tag:MLModule
```

## Что искать в логах

### 1. Загрузка модели (при старте приложения):
```
MLModule: MIRNet Interpreter создан с CPU
```
**Если видите ошибку:**
```
MLModule: MIRNet модель не найдена: ...
```
→ Модель `mirnet.tflite` не найдена в `app/src/main/assets/`

### 2. При использовании цветокоррекции:

**Успешный путь:**
```
MirNetProcessor: Начало цветокоррекции: brightness=0.5, contrast=0.3, saturation=0.2
MirNetProcessor: Применяем корректировки цвета...
MirNetProcessor: Корректировки цвета применены
MirNetProcessor: Размеры модели: 256x256
MirNetProcessor: Обрабатываем изображение целиком
MirNetProcessor: Обработка изображения: 1920x1080 -> модель: 256x256 -> результат: 1920x1080
MirNetProcessor: Препроцессинг выполнен
MirNetProcessor: Выходной тензор: shape=[1, 256, 256, 3], dtype=FLOAT32
MirNetProcessor: Запуск инференса MIRNet...
MirNetProcessor: Инференс завершен
MirNetProcessor: Постпроцессинг выполнен, результат: 1920x1080
MirNetProcessor: Цветокоррекция успешно применена: 1920x1080
ProcessingRepository: Цветокоррекция применена успешно: 1920x1080
```

**Ошибки:**
```
MirNetProcessor: TFLite Interpreter не инициализирован
→ Модель не загрузилась при старте приложения

MirNetProcessor: Не удалось применить корректировки цвета
→ Ошибка применения ColorMatrix

MirNetProcessor: MIRNet вернул null
→ Ошибка при инференсе модели

MirNetProcessor: Ошибка обработки изображения: ...
→ Детали ошибки в стектрейсе

ProcessingRepository: Цветокоррекция вернула null
→ Ошибка на уровне репозитория
```

## Команды для терминала (adb)

### Просмотр всех логов MIRNet:
```bash
adb logcat -s MirNetProcessor
```

### Просмотр всех логов обработки:
```bash
adb logcat -s MirNetProcessor ProcessingRepository EditorViewModel
```

### Просмотр только ошибок:
```bash
adb logcat *:E
```

### Просмотр с фильтром по тегу и уровню:
```bash
adb logcat MirNetProcessor:D ProcessingRepository:D EditorViewModel:D *:S
```

### Очистка логов и просмотр в реальном времени:
```bash
adb logcat -c && adb logcat -s MirNetProcessor ProcessingRepository
```

## Типичные проблемы и их признаки в логах

### Проблема 1: Модель не загружена
**Логи:**
```
MLModule: MIRNet модель не найдена: ...
MirNetProcessor: TFLite Interpreter не инициализирован
```
**Решение:** Проверьте наличие файла `mirnet.tflite` в `app/src/main/assets/`

### Проблема 2: Ошибка при инференсе
**Логи:**
```
MirNetProcessor: Запуск инференса MIRNet...
MirNetProcessor: Ошибка обработки изображения: ...
```
**Решение:** Проверьте формат входных данных модели и размеры тензоров

### Проблема 3: Bitmap переработан
**Логи:**
```
ProcessingRepository: Bitmap переработан, невозможно применить цветокоррекцию
```
**Решение:** Проблема с управлением памятью, bitmap был освобожден

### Проблема 4: Ошибка применения ColorMatrix
**Логи:**
```
MirNetProcessor: Не удалось применить корректировки цвета
MirNetProcessor: Ошибка применения корректировок цвета: ...
```
**Решение:** Проблема с применением корректировок цвета

## Полезные команды для отладки

### Просмотр размера модели:
```bash
adb logcat | grep "Размеры модели"
```

### Просмотр всех ошибок:
```bash
adb logcat *:E | grep -E "MirNetProcessor|ProcessingRepository|EditorViewModel"
```

### Сохранение логов в файл:
```bash
adb logcat -s MirNetProcessor ProcessingRepository > debug_logs.txt
```
