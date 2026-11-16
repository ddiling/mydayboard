package io.github.ddiling.mydayboard.core.designsystem.preview

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import io.github.ddiling.mydayboard.core.designsystem.theme.AppTheme

/** Wraps preview content in the app theme on a surface, so every preview looks like the real app. */
@Composable
fun PreviewSurface(content: @Composable () -> Unit) {
    AppTheme {
        Surface(content = content)
    }
}
