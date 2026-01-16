package com.example.neuralphotoredactor.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.example.neuralphotoredactor.R

/**
 * Компонент для отображения сообщения об ошибке.
 *
 * @param message Сообщение об ошибке (может быть null)
 * @param defaultMessageId ID строкового ресурса для дефолтного сообщения
 * @param modifier Modifier для компонента
 */
@Composable
fun ErrorMessage(
    message: String?,
    defaultMessageId: Int = R.string.error_load_images,
    modifier: Modifier = Modifier,
) {
    Text(
        text = message ?: stringResource(defaultMessageId),
        modifier = modifier,
        textAlign = TextAlign.Center,
    )
}
