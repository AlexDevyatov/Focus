package com.example.neuralphotoredactor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.neuralphotoredactor.domain.model.ImageData
import com.example.neuralphotoredactor.presentation.state.gallery.GalleryUiState
import com.example.neuralphotoredactor.presentation.ui.screen.gallery.GalleryScreen
import com.example.neuralphotoredactor.presentation.viewmodel.GalleryViewModel
import com.example.neuralphotoredactor.ui.theme.NeuralPhotoRedactorTheme

class MainActivity : ComponentActivity() {

    private val galleryViewModel: GalleryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NeuralPhotoRedactorTheme {
                val uiState by galleryViewModel.uiState.collectAsState()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GalleryScreen(
                        modifier = Modifier.padding(innerPadding),
                        uiState = uiState,
                        onImageClick = galleryViewModel::onImageSelected,
                        onImportClick = galleryViewModel::onImportRequested,
                        onCaptureClick = galleryViewModel::onCaptureRequested
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GalleryScreenPreview() {
    NeuralPhotoRedactorTheme {
        GalleryScreen(
            uiState = GalleryUiState(images = previewGalleryItems),
            onImageClick = {},
            onImportClick = {},
            onCaptureClick = {}
        )
    }
}

private val previewGalleryItems: List<ImageData> = List(6) { index ->
    ImageData(
        id = "preview-$index",
        name = "Sample ${index + 1}"
    )
}