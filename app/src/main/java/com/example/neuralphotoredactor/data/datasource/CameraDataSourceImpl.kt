package com.example.neuralphotoredactor.data.datasource

import com.example.neuralphotoredactor.domain.model.ImageData
import javax.inject.Inject

/**
 * Реализация источника данных для работы с камерой.
 * 
 * Захват изображения происходит через CameraX в UI слое,
 * здесь возвращаем null, так как результат приходит через Activity Result.
 */
class CameraDataSourceImpl @Inject constructor() : CameraDataSource {
    
    override suspend fun captureImage(): ImageData? {
        // Реализация захвата через CameraX будет в UI слое
        return null
    }
}

