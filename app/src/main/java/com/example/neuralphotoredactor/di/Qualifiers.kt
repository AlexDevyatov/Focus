package com.example.neuralphotoredactor.di

import javax.inject.Qualifier

/**
 * Квалификатор для Interpreter модели ESRGAN (super resolution).
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class EsrganInterpreter

/**
 * Квалификатор для Interpreter модели SplitterNet (удаление шумов).
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SplitterNetInterpreter

