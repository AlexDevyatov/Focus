package com.example.neuralphotoredactor.di

import android.content.Context
import com.example.neuralphotoredactor.domain.usecase.InitializeNeuralModelsUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * EntryPoint для доступа к зависимостям в Application классе.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppEntryPoint {
    fun initializeNeuralModelsUseCase(): InitializeNeuralModelsUseCase
    
    companion object {
        fun get(context: Context): AppEntryPoint {
            return EntryPointAccessors.fromApplication(
                context.applicationContext,
                AppEntryPoint::class.java
            )
        }
    }
}

