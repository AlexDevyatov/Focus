package com.example.neuralphotoredactor.data.repository

import com.example.neuralphotoredactor.data.local.dao.EditingSessionDao
import com.example.neuralphotoredactor.data.mapper.EditingSessionMapper
import com.example.neuralphotoredactor.domain.model.EditingSession
import com.example.neuralphotoredactor.domain.repository.EditingSessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Реализация репозитория для работы с сессиями редактирования.
 * 
 * Использует Room Database для хранения сессий.
 * Вся работа с БД происходит через DAO, обеспечивая изоляцию слоев.
 */
class EditingSessionRepositoryImpl @Inject constructor(
    private val editingSessionDao: EditingSessionDao
) : EditingSessionRepository {
    
    override fun getAllSessions(): Flow<List<EditingSession>> {
        return editingSessionDao.getAllSessions()
            .map { entities ->
                EditingSessionMapper.toDomainList(entities)
            }
    }
    
    override fun getSessionById(id: Long): Flow<EditingSession?> {
        return editingSessionDao.getSessionByIdFlow(id)
            .map { entity ->
                entity?.let { EditingSessionMapper.toDomain(it) }
            }
    }
    
    override suspend fun createSession(session: EditingSession): Long = withContext(Dispatchers.IO) {
        val entity = EditingSessionMapper.toEntity(session)
        editingSessionDao.insert(entity)
    }
    
    override suspend fun updateSession(session: EditingSession) = withContext(Dispatchers.IO) {
        val entity = EditingSessionMapper.toEntity(session)
        editingSessionDao.update(entity)
    }
    
    override suspend fun deleteSession(session: EditingSession) = withContext(Dispatchers.IO) {
        val entity = EditingSessionMapper.toEntity(session)
        editingSessionDao.delete(entity)
    }
    
    override suspend fun deleteSessionById(id: Long) = withContext(Dispatchers.IO) {
        editingSessionDao.deleteById(id)
    }
}

