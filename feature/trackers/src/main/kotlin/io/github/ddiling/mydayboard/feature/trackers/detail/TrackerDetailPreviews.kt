package io.github.ddiling.mydayboard.feature.trackers.detail

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import io.github.ddiling.mydayboard.core.designsystem.preview.PreviewSurface
import io.github.ddiling.mydayboard.core.domain.tracker.DayCount
import io.github.ddiling.mydayboard.core.domain.tracker.Trend
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

private val previewToday = LocalDate.of(2026, 3, 2)

private val previewCounter =
    TrackerDetailUiState.Counter(
        name = "Water",
        todayCount = 3,
        sinceLast = Duration.ofMinutes(200),
        lastDays =
            listOf(2, 4, 1, 0, 5, 3, 3).mapIndexed {
                i,
                count,
                ->
                DayCount(previewToday.minusDays(6L - i), count)
            },
        trend = Trend.UP,
        history =
            listOf(
                HistoryDay(
                    previewToday,
                    listOf(HistoryEntry("e3", LocalTime.of(9, 30)), HistoryEntry("e2", LocalTime.of(8, 5))),
                ),
                HistoryDay(previewToday.minusDays(1), listOf(HistoryEntry("e1", LocalTime.of(20, 0)))),
            ),
        pendingUndo = null,
    )

private val previewEmptyCounter =
    previewCounter.copy(
        todayCount = 0,
        sinceLast = null,
        lastDays = previewCounter.lastDays.map { it.copy(count = 0) },
        trend = Trend.NOT_ENOUGH_DATA,
        history = emptyList(),
    )

@Composable
private fun Preview(state: TrackerDetailUiState) = PreviewSurface { TrackerDetailScreen(state, TrackerDetailActions()) }

@PreviewLightDark
@Composable
private fun TrackerDetailCounterPreview() = Preview(previewCounter)

@PreviewLightDark
@Composable
private fun TrackerDetailCounterEmptyPreview() = Preview(previewEmptyCounter)

@PreviewLightDark
@Composable
private fun TrackerDetailTrendDownPreview() = Preview(previewCounter.copy(trend = Trend.DOWN))

@PreviewLightDark
@Composable
private fun TrackerDetailTrendFlatPreview() = Preview(previewCounter.copy(trend = Trend.FLAT))

@PreviewLightDark
@Composable
private fun TrackerDetailTrendUpPreview() = Preview(previewCounter.copy(trend = Trend.UP))

@PreviewLightDark
@Composable
private fun TrackerDetailTrendNotEnoughDataPreview() = Preview(previewCounter.copy(trend = Trend.NOT_ENOUGH_DATA))

@PreviewLightDark
@Composable
private fun TrackerDetailNotFoundPreview() = Preview(TrackerDetailUiState.NotFound)

@Preview(name = "Large font light", fontScale = 2f)
@Preview(name = "Large font dark", fontScale = 2f, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun TrackerDetailLargeFontPreview() = Preview(previewCounter)
