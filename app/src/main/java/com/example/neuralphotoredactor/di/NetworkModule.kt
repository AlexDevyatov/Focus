package com.example.neuralphotoredactor.di

import com.example.neuralphotoredactor.data.remote.api.AIServiceApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt модуль для предоставления зависимостей, связанных с сетевыми запросами.
 * 
 * Создает и предоставляет OkHttpClient, Retrofit и API интерфейсы для взаимодействия
 * с облачными сервисами обработки изображений. Все зависимости являются Singleton.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    /**
     * Создает и предоставляет настроенный OkHttpClient.
     * 
     * Включает логирование HTTP запросов и ответов для отладки, а также
     * устанавливает таймауты для соединения и чтения.
     * 
     * @return Настроенный экземпляр OkHttpClient
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Создает и предоставляет настроенный Retrofit клиент.
     * 
     * @param okHttpClient OkHttpClient для выполнения HTTP запросов
     * @return Настроенный экземпляр Retrofit с базовым URL и Gson конвертером
     */
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.example.com/") // TODO: Replace with actual API URL
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Создает и предоставляет API интерфейс для AI сервиса.
     * 
     * @param retrofit Экземпляр Retrofit для создания API интерфейса
     * @return Реализация AIServiceApi интерфейса
     */
    @Provides
    @Singleton
    fun provideAIServiceApi(retrofit: Retrofit): AIServiceApi {
        return retrofit.create(AIServiceApi::class.java)
    }
}

