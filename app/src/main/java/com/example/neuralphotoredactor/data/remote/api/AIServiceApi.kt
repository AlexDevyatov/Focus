package com.example.neuralphotoredactor.data.remote.api

import com.example.neuralphotoredactor.data.remote.dto.ProcessingRequestDto
import com.example.neuralphotoredactor.data.remote.dto.ProcessingResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit API интерфейс для взаимодействия с облачным AI сервисом обработки изображений.
 * 
 * Определяет endpoints для отправки запросов на обработку изображений через HTTP.
 * Используется в ProcessingRepository для cloud-based фильтров.
 * 
 * @see com.example.neuralphotoredactor.data.repository.ProcessingRepositoryImpl
 */
interface AIServiceApi {
    /**
     * Отправляет запрос на обработку изображения на сервер.
     * 
     * @param request DTO с данными запроса (изображение в Base64, тип фильтра, параметры)
     * @return ProcessingResponseDto с результатом обработки
     */
    @POST("process")
    suspend fun processImage(@Body request: ProcessingRequestDto): ProcessingResponseDto
}

