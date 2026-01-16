package com.example.neuralphotoredactor.data.repository

import com.example.neuralphotoredactor.data.local.dao.FilterDao
import com.example.neuralphotoredactor.domain.repository.FilterRepository
import javax.inject.Inject

/**
 * Реализация репозитория для работы с фильтрами.
 *
 * Использует Room Database для хранения фильтров.
 * Вся работа с БД происходит через DAO, обеспечивая изоляцию слоев.
 */
class FilterRepositoryImpl
    @Inject
    constructor(
        private val filterDao: FilterDao,
    ) : FilterRepository {
        override suspend fun getFilterNameById(filterId: Long): String? {
            return filterDao.getFilterById(filterId)?.name
        }
    }
