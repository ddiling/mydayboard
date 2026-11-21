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
 * The data behind [FakeTrackerRepository] and [FakeTrackerReasonRepository]. They share one store, like the one
 * Room database behind the real ones: a deleted tracker hides its reasons, and the flag shows when observed.
 */
class FakeTrackerStore {
    internal val state = MutableStateFlow(FakeTrackerState())
}
