package io.github.ddiling.mydayboard.feature.trackers.list

import java.time.Duration

/** What the tracker list shows. [Loading] lasts until the first emission, so "empty" never flashes on start. */
internal sealed interface TrackersListUiState {
    data object Loading : TrackersListUiState

    data object Empty : TrackersListUiState

    data class Content(
        val items: List<TrackerListItem>,
    ) : TrackersListUiState
}

/** One row, as display-ready values rather than domain objects, so a preview needs no repository or clock. */
internal sealed interface TrackerListItem {
    val id: String
    val name: String

    data class Counter(
        override val id: String,
        override val name: String,
        val todayCount: Int,
        val sinceLast: Duration?, // null: no event yet
    ) : TrackerListItem

    data class DaysSince(
        override val id: String,
        override val name: String,
        val days: Int,
    ) : TrackerListItem
}
