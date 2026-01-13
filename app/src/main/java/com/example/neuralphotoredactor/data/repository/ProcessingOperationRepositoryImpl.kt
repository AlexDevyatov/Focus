package com.example.neuralphotoredactor.data.repository

import com.example.neuralphotoredactor.data.local.dao.ProcessingOperationDao
import com.example.neuralphotoredactor.data.mapper.ProcessingOperationMapper
import com.example.neuralphotoredactor.domain.model.ProcessingOperation
import com.example.neuralphotoredactor.domain.repository.ProcessingOperationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Реализация репозитория для работы с операциями обработки.
 * 
 * Использует Room Database для хранения операций.
 * Вся работа с БД происходит через DAO, обеспечивая изоляцию слоев.
 */
class ProcessingOperationRepositoryImpl @Inject constructor(
    private val processingOperationDao: ProcessingOperationDao
) : ProcessingOperationRepository {
    
    override fun getOperationsBySessionId(sessionId: Long): Flow<List<ProcessingOperation>> {
        return processingOperationDao.getOperationsBySessionId(sessionId)
            .map { entities ->
                ProcessingOperationMapper.toDomainList(entities)
            }
    }
    
    override fun getOperationsByHistoryId(historyId: Long): Flow<List<ProcessingOperation>> {
        return processingOperationDao.getOperationsByHistoryId(historyId)
            .map { entities ->
                ProcessingOperationMapper.toDomainList(entities)
            }
    }
    
    override suspend fun getOperationById(id: Long): ProcessingOperation? = withContext(Dispatchers.IO) {
        val entity = processingOperationDao.getOperationById(id)
        entity?.let { ProcessingOperationMapper.toDomain(it) }
    }
    
    override suspend fun getLastOperationBySessionId(sessionId: Long): ProcessingOperation? = withContext(Dispatchers.IO) {
        val entity = processingOperationDao.getLastOperationBySessionId(sessionId)
        entity?.let { ProcessingOperationMapper.toDomain(it) }
    }
    
    override suspend fun addOperation(operation: ProcessingOperation): Long = withContext(Dispatchers.IO) {
        val entity = ProcessingOperationMapper.toEntity(operation)
        processingOperationDao.insert(entity)
    }
    
    override suspend fun deleteOperation(operation: ProcessingOperation) = withContext(Dispatchers.IO) {
        val entity = ProcessingOperationMapper.toEntity(operation)
        processingOperationDao.delete(entity)
    }
    
    override suspend fun deleteOperationsBySessionId(sessionId: Long) = withContext(Dispatchers.IO) {
        processingOperationDao.deleteBySessionId(sessionId)
    }
}

