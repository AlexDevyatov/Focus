package com.example.neuralphotoredactor.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.example.neuralphotoredactor.domain.model.ImageData
import com.example.neuralphotoredactor.presentation.state.gallery.GalleryUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GalleryViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(GalleryUiState(isLoading = true))
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    init {
        loadInitialGallery()
    }

    fun onImportRequested() {
        // TODO: Connect to gallery import data source
    }

    fun onCaptureRequested() {
        // TODO: Connect to camera capture data source
    }

    fun onImageSelected(image: ImageData) {
        // TODO: Route to editor screen with selected image
    }

    private fun loadInitialGallery() {
        val sampleImages = List(9) { index ->
            ImageData(
                id = "sample-$index",
                name = "Sample ${index + 1}"
            )
        }
        _uiState.value = GalleryUiState(images = sampleImages)
    }
}

