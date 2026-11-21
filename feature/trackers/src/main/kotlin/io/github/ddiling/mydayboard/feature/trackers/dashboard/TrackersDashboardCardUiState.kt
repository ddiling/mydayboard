package io.github.ddiling.mydayboard.feature.trackers.dashboard

import io.github.ddiling.mydayboard.feature.trackers.detail.Undo
import java.time.Duration

/** What the dashboard card shows. [Loading] lasts until the first emission, so "empty" never flashes on start. */
internal sealed interface TrackersDashboardCardUiState {
    data object Loading : TrackersDashboardCardUiState

    /**
     * [rows] are the trackers that get a place on the card (at most [MAX_PLACES]; empty when there are none yet),
     * [totalCount] is how many trackers exist, and [pendingUndo] is the action the snackbar offers to undo.
     */
    data class Content(
        val rows: List<TrackersDashboardRow>,
        val totalCount: Int,
        val pendingUndo: Undo?,
    ) : TrackersDashboardCardUiState
}

/** How many places the card's grid has. */
internal const val MAX_PLACES = 4

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
