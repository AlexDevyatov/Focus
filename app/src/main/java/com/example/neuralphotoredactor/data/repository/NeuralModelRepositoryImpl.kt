package com.example.neuralphotoredactor.data.repository

import com.example.neuralphotoredactor.data.local.dao.NeuralModelDao
import com.example.neuralphotoredactor.data.mapper.NeuralModelMapper
import com.example.neuralphotoredactor.domain.model.ModelType
import com.example.neuralphotoredactor.domain.model.NeuralModel
import com.example.neuralphotoredactor.domain.repository.NeuralModelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Реализация репозитория для работы с нейросетевыми моделями.
 *
 * Использует Room Database для хранения моделей.
 * Вся работа с БД происходит через DAO, обеспечивая изоляцию слоев.
 */
class NeuralModelRepositoryImpl
    @Inject
    constructor(
        private val neuralModelDao: NeuralModelDao,
    ) : NeuralModelRepository {
        override fun getAllActiveModels(): Flow<List<NeuralModel>> {
            return neuralModelDao.getAllActiveModels()
                .map { entities ->
                    NeuralModelMapper.toDomainList(entities)
                }
        }

        override fun getAllModels(): Flow<List<NeuralModel>> {
            return neuralModelDao.getAllModels()
                .map { entities ->
                    NeuralModelMapper.toDomainList(entities)
                }
        }

        override fun getModelsByType(type: ModelType): Flow<List<NeuralModel>> {
            return neuralModelDao.getModelsByType(type.name)
                .map { entities ->
                    NeuralModelMapper.toDomainList(entities)
                }
        }

        override suspend fun getModelById(id: Long): NeuralModel? =
            withContext(Dispatchers.IO) {
                val entity = neuralModelDao.getModelById(id)
                entity?.let { NeuralModelMapper.toDomain(it) }
            }

        override suspend fun getModelByName(name: String): NeuralModel? =
            withContext(Dispatchers.IO) {
                val entity = neuralModelDao.getModelByName(name)
                entity?.let { NeuralModelMapper.toDomain(it) }
            }

        override suspend fun addModel(model: NeuralModel): Long =
            withContext(Dispatchers.IO) {
                val entity = NeuralModelMapper.toEntity(model)
                neuralModelDao.insert(entity)
            }

        override suspend fun updateModel(model: NeuralModel) =
            withContext(Dispatchers.IO) {
                val entity = NeuralModelMapper.toEntity(model)
                neuralModelDao.update(entity)
            }

        override suspend fun deleteModel(model: NeuralModel) =
            withContext(Dispatchers.IO) {
                val entity = NeuralModelMapper.toEntity(model)
                neuralModelDao.delete(entity)
            }

        override suspend fun setModelActive(
            id: Long,
            isActive: Boolean,
        ) = withContext(Dispatchers.IO) {
            neuralModelDao.setActive(id, isActive)
        }
    }
