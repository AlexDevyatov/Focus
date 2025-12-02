package com.example.neuralphotoredactor.data.mapper

import android.net.Uri
import com.example.neuralphotoredactor.data.local.entity.EditingSessionEntity
import com.example.neuralphotoredactor.domain.model.EditingSession
import com.example.neuralphotoredactor.domain.model.SessionMetadata
import org.json.JSONObject

/**
 * Mapper для преобразования между EditingSessionEntity и EditingSession.
 * 
 * Выполняет преобразование между слоями data и domain,
 * включая парсинг JSON метаданных.
 */
object EditingSessionMapper {
    
    /**
     * Преобразовать Entity в Domain модель.
     * 
     * @param entity Entity из базы данных
     * @return Domain модель EditingSession
     */
    fun toDomain(entity: EditingSessionEntity): EditingSession {
        return EditingSession(
            id = entity.id,
            originalImageUri = Uri.parse(entity.originalImageUri),
            currentImageUri = Uri.parse(entity.currentImageUri),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            metadata = parseMetadata(entity.metadata)
        )
    }
    
    /**
     * Преобразовать Domain модель в Entity.
     * 
     * @param session Domain модель EditingSession
     * @return Entity для базы данных
     */
    fun toEntity(session: EditingSession): EditingSessionEntity {
        return EditingSessionEntity(
            id = session.id,
            originalImageUri = session.originalImageUri.toString(),
            currentImageUri = session.currentImageUri.toString(),
            createdAt = session.createdAt,
            updatedAt = session.updatedAt,
            metadata = serializeMetadata(session.metadata)
        )
    }
    
    /**
     * Преобразовать список Entity в список Domain моделей.
     * 
     * @param entities Список Entity
     * @return Список Domain моделей
     */
    fun toDomainList(entities: List<EditingSessionEntity>): List<EditingSession> {
        return entities.map { toDomain(it) }
    }
    
    /**
     * Парсить JSON метаданные в SessionMetadata.
     */
    private fun parseMetadata(jsonString: String): SessionMetadata {
        return try {
            val json = JSONObject(jsonString)
            val exifData = mutableMapOf<String, String>()
            val exifObject = json.optJSONObject("exifData")
            exifObject?.let {
                it.keys().forEach { key ->
                    exifData[key] = it.getString(key)
                }
            }
            
            SessionMetadata(
                width = json.optInt("width", 0),
                height = json.optInt("height", 0),
                format = json.optString("format", "JPEG"),
                exifData = exifData
            )
        } catch (e: Exception) {
            SessionMetadata()
        }
    }
    
    /**
     * Сериализовать SessionMetadata в JSON строку.
     */
    private fun serializeMetadata(metadata: SessionMetadata): String {
        return try {
            val json = JSONObject().apply {
                put("width", metadata.width)
                put("height", metadata.height)
                put("format", metadata.format)
                put("exifData", JSONObject(metadata.exifData))
            }
            json.toString()
        } catch (e: Exception) {
            "{}"
        }
    }
}

