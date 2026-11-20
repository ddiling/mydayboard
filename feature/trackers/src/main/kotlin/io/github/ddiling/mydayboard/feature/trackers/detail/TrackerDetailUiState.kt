package io.github.ddiling.mydayboard.feature.trackers.detail

import io.github.ddiling.mydayboard.core.domain.tracker.DayCount
import io.github.ddiling.mydayboard.core.domain.tracker.Trend
import java.time.Duration

/** What the detail screen shows. [Loading] lasts until the first emission; [NotFound] is e.g. a deleted tracker. */
internal sealed interface TrackerDetailUiState {
    data object Loading : TrackerDetailUiState

    data object NotFound : TrackerDetailUiState

    /** An event counter, as display-ready values so a preview needs no repository or clock. */
    data class Counter(
        val name: String,
        val todayCount: Int,
        val sinceLast: Duration?, // null: no event yet
        val lastDays: List<DayCount>, // oldest first, the last one is today
        val trend: Trend,
        val pendingUndo: Undo?,
    ) : TrackerDetailUiState

    /** Placeholder until the days-since detail arrives. */
    data class DaysSince(
        val name: String,
    ) : TrackerDetailUiState
}
