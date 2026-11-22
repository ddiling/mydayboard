package io.github.ddiling.mydayboard.feature.tasks.categories

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import io.github.ddiling.mydayboard.core.designsystem.preview.PreviewSurface

@Composable
private fun PreviewStats(state: CategoryStatsUiState) {
    PreviewSurface { CategoryStatsScreen(state, CategoryStatsActions()) }
}

@PreviewLightDark
@Composable
private fun CategoryStatsPreview() = PreviewStats(previewStatsContent)

@PreviewLightDark
@Composable
private fun CategoryStatsEmptyPreview() = PreviewStats(CategoryStatsUiState.Empty("Meals"))

@PreviewLightDark
@Composable
private fun CategoryStatsLoadingPreview() = PreviewStats(CategoryStatsUiState.Loading)

@Preview(name = "Large font light", fontScale = 2f)
@Preview(name = "Large font dark", fontScale = 2f, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun CategoryStatsLargeFontPreview() = PreviewStats(previewStatsContent)
