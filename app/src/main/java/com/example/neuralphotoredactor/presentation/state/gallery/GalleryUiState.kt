package com.example.neuralphotoredactor.presentation.state.gallery

import com.example.neuralphotoredactor.domain.model.ImageData

data class GalleryUiState(
    val images: List<ImageData> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

