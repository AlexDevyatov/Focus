package com.example.neuralphotoredactor.presentation.ui.screen.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.neuralphotoredactor.domain.model.ImageData
import com.example.neuralphotoredactor.presentation.state.gallery.GalleryUiState
import com.example.neuralphotoredactor.ui.theme.NeuralPhotoRedactorTheme

@Composable
fun GalleryScreen(
    uiState: GalleryUiState,
    onImageClick: (ImageData) -> Unit,
    onImportClick: () -> Unit,
    onCaptureClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = "Gallery") },
                actions = {
                    TextButton(onClick = onCaptureClick) {
                        Text(text = "Camera")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onImportClick,
                text = { Text(text = "Import") }
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingState(modifier = Modifier.padding(innerPadding))
            uiState.images.isEmpty() -> EmptyGalleryState(modifier = Modifier.padding(innerPadding))
            else -> GalleryGrid(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                images = uiState.images,
                onImageClick = onImageClick
            )
        }

        if (uiState.errorMessage != null) {
            ErrorMessage(
                message = uiState.errorMessage,
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun GalleryGrid(
    images: List<ImageData>,
    onImageClick: (ImageData) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        modifier = modifier,
        columns = GridCells.Adaptive(minSize = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(images, key = { it.id }) { image ->
            GalleryGridItem(
                image = image,
                onClick = { onImageClick(image) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyGalleryState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Your gallery is empty",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Tap Import to add photos from your device or use the camera to capture a new one.",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorMessage(
    message: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = message,
        modifier = modifier,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
private fun GalleryGridItem(
    image: ImageData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = image.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GalleryScreenPreview() {
    NeuralPhotoRedactorTheme {
        GalleryScreen(
            uiState = GalleryUiState(
                images = sampleImages()
            ),
            onImageClick = {},
            onImportClick = {},
            onCaptureClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyGalleryScreenPreview() {
    NeuralPhotoRedactorTheme {
        GalleryScreen(
            uiState = GalleryUiState(),
            onImageClick = {},
            onImportClick = {},
            onCaptureClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadingGalleryScreenPreview() {
    NeuralPhotoRedactorTheme {
        GalleryScreen(
            uiState = GalleryUiState(isLoading = true),
            onImageClick = {},
            onImportClick = {},
            onCaptureClick = {}
        )
    }
}

private fun sampleImages(): List<ImageData> = List(6) { index ->
    ImageData(
        id = "preview-$index",
        name = "Preview ${index + 1}"
    )
}

