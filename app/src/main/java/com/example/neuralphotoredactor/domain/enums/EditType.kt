package com.example.neuralphotoredactor.domain.enums

/**
 * Типы редактирования изображений.
 */
enum class EditType {
    /** Кадрирование изображения */
    CROP,

    /** Поворот на 90 градусов по часовой стрелке */
    ROTATE_90,

    /** Поворот на 180 градусов */
    ROTATE_180,

    /** Поворот на 270 градусов (или -90) */
    ROTATE_270,

    /** Отражение по горизонтали */
    FLIP_HORIZONTAL,

    /** Отражение по вертикали */
    FLIP_VERTICAL,

    /** Коррекция яркости */
    BRIGHTNESS,

    /** Коррекция контраста */
    CONTRAST,

    /** Настройка цветового баланса (красный) */
    COLOR_BALANCE_RED,

    /** Настройка цветового баланса (зеленый) */
    COLOR_BALANCE_GREEN,

    /** Настройка цветового баланса (синий) */
    COLOR_BALANCE_BLUE,
}
