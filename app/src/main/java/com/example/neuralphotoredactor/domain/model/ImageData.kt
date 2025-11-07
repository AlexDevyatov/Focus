package com.example.neuralphotoredactor.domain.model

data class ImageData(
    val id: String,
    val name: String,
    val thumbnailUri: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)

