package io.github.ddiling.mydayboard.feature.trackers.detail

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import io.github.ddiling.mydayboard.core.designsystem.preview.PreviewSurface
import java.time.LocalDate

private val previewToday = LocalDate.of(2026, 3, 2)

private val previewNoResets =
    TrackerDetailUiState.DaysSince(
        name = "No sugar",
        currentDays = 12,
        currentStart = previewToday.minusDays(12),
        longestDays = 12,
        brokenStreaks = emptyList<BrokenStreak>(),
        pendingUndo = null,
    )

private val previewWithHistory =
    previewNoResets.copy(
        longestDays = 30,
        brokenStreaks =
            listOf(
                BrokenStreak(previewToday.minusDays(40), previewToday.minusDays(12), 28),
                BrokenStreak(previewToday.minusDays(70), previewToday.minusDays(40), 30),
                BrokenStreak(previewToday.minusDays(71), previewToday.minusDays(70), 1),
            ),
    )

@Composable
private fun DaysSincePreview(state: TrackerDetailUiState.DaysSince) =
    PreviewSurface { TrackerDetailScreen(state, TrackerDetailActions()) }

@PreviewLightDark
@Composable
private fun TrackerDetailDaysSinceNoResetsPreview() = DaysSincePreview(previewNoResets)

@PreviewLightDark
@Composable
private fun TrackerDetailDaysSinceHistoryPreview() = DaysSincePreview(previewWithHistory)

@Preview(name = "Days since large font light", fontScale = 2f)
@Preview(name = "Days since large font dark", fontScale = 2f, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun TrackerDetailDaysSinceLargeFontPreview() = DaysSincePreview(previewWithHistory)
