package com.example.neuralphotoredactor.data.mapper

import android.net.Uri
import com.example.neuralphotoredactor.data.local.entity.ProcessingOperationEntity
import com.example.neuralphotoredactor.domain.model.OperationParameters
import com.example.neuralphotoredactor.domain.model.ProcessingOperation
import org.json.JSONObject

/**
 * Mapper для преобразования между ProcessingOperationEntity и ProcessingOperation.
 * 
 * Выполняет преобразование между слоями data и domain,
 * включая парсинг JSON параметров.
 */
object ProcessingOperationMapper {
    
    /**
     * Преобразовать Entity в Domain модель.
     * 
     * @param entity Entity из базы данных
     * @return Domain модель ProcessingOperation
     */
    fun toDomain(entity: ProcessingOperationEntity): ProcessingOperation {
        return ProcessingOperation(
            id = entity.id,
            historyId = entity.historyId,
            sessionId = entity.sessionId,
            filterId = entity.filterId,
            parameters = parseParameters(entity.parameters),
            inputImageUri = Uri.parse(entity.inputImageUri),
            outputImageUri = Uri.parse(entity.outputImageUri),
            processingTimeMs = entity.processingTimeMs,
            sequenceNumber = entity.sequenceNumber
        )
    }
    
    /**
     * Преобразовать Domain модель в Entity.
     * 
     * @param operation Domain модель ProcessingOperation
     * @return Entity для базы данных
     */
    fun toEntity(operation: ProcessingOperation): ProcessingOperationEntity {
        return ProcessingOperationEntity(
            id = operation.id,
            historyId = operation.historyId,
            sessionId = operation.sessionId,
            filterId = operation.filterId,
            parameters = serializeParameters(operation.parameters),
            inputImageUri = operation.inputImageUri.toString(),
            outputImageUri = operation.outputImageUri.toString(),
            processingTimeMs = operation.processingTimeMs,
            sequenceNumber = operation.sequenceNumber
        )
    }
    
    /**
     * Преобразовать список Entity в список Domain моделей.
     * 
     * @param entities Список Entity
     * @return Список Domain моделей
     */
    fun toDomainList(entities: List<ProcessingOperationEntity>): List<ProcessingOperation> {
        return entities.map { toDomain(it) }
    }
    
    /**
     * Парсить JSON параметры в OperationParameters.
     */
    private fun parseParameters(jsonString: String): OperationParameters {
        return try {
            val json = JSONObject(jsonString)
            val additionalParams = mutableMapOf<String, Any>()
            val additionalObject = json.optJSONObject("additionalParams")
            additionalObject?.let {
                it.keys().forEach { key ->
                    val value = it.get(key)
                    additionalParams[key] = when (value) {
                        is String -> value
                        is Number -> value
                        is Boolean -> value
                        else -> value.toString()
                    }
                }
            }
            
            OperationParameters(
                filterType = if (json.has("filterType") && !json.isNull("filterType")) {
                    json.getString("filterType")
                } else {
                    null
                },
                intensity = json.optDouble("intensity", 1.0).toFloat(),
                additionalParams = additionalParams
            )
        } catch (e: Exception) {
            OperationParameters()
        }
    }
    
    /**
     * Сериализовать OperationParameters в JSON строку.
     */
    private fun serializeParameters(parameters: OperationParameters): String {
        return try {
            val json = JSONObject().apply {
                parameters.filterType?.let { put("filterType", it) }
                put("intensity", parameters.intensity)
                put("additionalParams", JSONObject(parameters.additionalParams.mapValues { it.value.toString() }))
            }
            json.toString()
        } catch (e: Exception) {
            "{}"
        }
    }
}

