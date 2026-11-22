package io.github.ddiling.mydayboard.feature.tasks.categories

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import io.github.ddiling.mydayboard.core.designsystem.preview.PreviewSurface

internal val previewStatsContent =
    CategoryStatsUiState.Content(
        categoryName = "Meals",
        perDay = 2.4,
        doneOfPlanned = 72.0,
        plannedDays = 90,
        skipped = 4,
        tasks =
            listOf(
                TaskLine("breakfast", "Breakfast", skipped = 1),
                TaskLine("lunch", "Lunch", skipped = 2),
                TaskLine("dinner", "Dinner", skipped = 1),
            ),
        archivedLines = listOf(TaskLine("snack", "Afternoon snack", skipped = 0)),
    )

@Composable
private fun PreviewBody() {
    PreviewSurface { CategoryStatsBody(previewStatsContent, onTaskClick = {}) }
}

@PreviewLightDark
@Composable
private fun CategoryStatsBodyPreview() = PreviewBody()

@Preview(name = "Large font light", fontScale = 2f)
@Preview(name = "Large font dark", fontScale = 2f, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun CategoryStatsBodyLargeFontPreview() = PreviewBody()
