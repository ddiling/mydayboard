package io.github.ddiling.mydayboard.feature.trackers.dashboard

import io.github.ddiling.mydayboard.feature.trackers.detail.Undo
import java.time.Duration

/** What the dashboard card shows. [Loading] lasts until the first emission, so "empty" never flashes on start. */
internal sealed interface TrackersDashboardCardUiState {
    data object Loading : TrackersDashboardCardUiState

    /** [rows] is empty when there are no trackers yet; [pendingUndo] is the action the snackbar offers to undo. */
    data class Content(
        val rows: List<TrackersDashboardRow>,
        val pendingUndo: Undo?,
    ) : TrackersDashboardCardUiState
}

/** One row, as display-ready values rather than domain objects, so a preview needs no repository or clock. */
internal sealed interface TrackersDashboardRow {
    val id: String
    val name: String

    data class Counter(
        override val id: String,
        override val name: String,
        val todayCount: Int,
        val sinceLast: Duration?, // null: no event yet
    ) : TrackersDashboardRow

    data class DaysSince(
        override val id: String,
        override val name: String,
        val days: Int,
    ) : TrackersDashboardRow
}
