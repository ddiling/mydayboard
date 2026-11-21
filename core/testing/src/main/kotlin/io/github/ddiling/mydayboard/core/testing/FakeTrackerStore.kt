package io.github.ddiling.mydayboard.core.testing

import io.github.ddiling.mydayboard.core.domain.tracker.Tracker
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerEvent
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerReason
import kotlinx.coroutines.flow.MutableStateFlow

internal data class FakeTrackerState(
    val trackers: List<Tracker> = emptyList(),
    val events: List<TrackerEvent> = emptyList(),
    val deletedEvents: List<TrackerEvent> = emptyList(),
    val reasons: List<TrackerReason> = emptyList(),
    val deletedReasons: List<TrackerReason> = emptyList(),
) {
    fun hasTracker(id: String) = trackers.any { it.id == id }
}

/**
 * The data behind [FakeTrackerRepository] and [FakeTrackerReasonRepository]. The two share one store, so a
 * deleted tracker hides its reasons and a flag set through the reasons repository shows up when the tracker is
 * observed, as with the one Room database behind both real repositories.
 */
class FakeTrackerStore {
    internal val state = MutableStateFlow(FakeTrackerState())
}
