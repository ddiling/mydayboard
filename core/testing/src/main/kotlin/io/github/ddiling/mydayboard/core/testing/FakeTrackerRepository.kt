package io.github.ddiling.mydayboard.core.testing

import io.github.ddiling.mydayboard.core.domain.id.IdGenerator
import io.github.ddiling.mydayboard.core.domain.tracker.Tracker
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerEvent
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerRepository
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.Instant

/**
 * In-memory [TrackerRepository] for ViewModel tests. It passes the same contract tests as the Room
 * implementation, so a test using it sees the behaviour production will have. Deleted rows are simply
 * dropped, because the domain cannot tell them from missing ones.
 */
class FakeTrackerRepository(
    private val clock: Clock,
    private val ids: IdGenerator,
) : TrackerRepository {
    private data class State(
        val trackers: List<Tracker> = emptyList(),
        val events: List<TrackerEvent> = emptyList(),
    ) {
        fun hasTracker(id: String) = trackers.any { it.id == id }
    }

    private val state = MutableStateFlow(State())

    // The same orders as the Room queries, ties included, so both implementations list identically.
    private val trackerOrder = compareBy<Tracker> { it.createdAt }.thenBy { it.id }
    private val eventOrder = compareByDescending<TrackerEvent> { it.occurredAt }.thenByDescending { it.id }

    override fun observeTrackers(): Flow<List<Tracker>> =
        state.map { s -> s.trackers.sortedWith(trackerOrder) }.distinctUntilChanged()

    override fun observeTracker(id: String): Flow<Tracker?> =
        state.map { s -> s.trackers.firstOrNull { it.id == id } }.distinctUntilChanged()

    override fun observeEvents(trackerId: String): Flow<List<TrackerEvent>> =
        state
            .map { s -> s.events.filter { it.trackerId == trackerId }.sortedWith(eventOrder) }
            .distinctUntilChanged()

    override fun observeAllEvents(): Flow<List<TrackerEvent>> =
        state.map { s -> s.events.sortedWith(eventOrder) }.distinctUntilChanged()

    override suspend fun createTracker(
        name: String,
        type: TrackerType,
        startedAt: Instant,
    ): Tracker {
        val now = clock.instant()
        val tracker = Tracker(ids.newId(), name, type, startedAt, createdAt = now, updatedAt = now)
        state.update { it.copy(trackers = it.trackers + tracker) }
        return tracker
    }

    override suspend fun renameTracker(
        id: String,
        name: String,
    ) {
        val now = clock.instant()
        state.update { s ->
            s.copy(trackers = s.trackers.map { if (it.id == id) it.copy(name = name, updatedAt = now) else it })
        }
    }

    override suspend fun deleteTracker(id: String) {
        state.update { s ->
            s.copy(trackers = s.trackers.filter { it.id != id }, events = s.events.filter { it.trackerId != id })
        }
    }

    override suspend fun addEvent(
        trackerId: String,
        occurredAt: Instant,
    ): TrackerEvent? {
        val now = clock.instant()
        // Built outside `update`, whose lambda may run again when another writer races it.
        val event = TrackerEvent(ids.newId(), trackerId, occurredAt, createdAt = now, updatedAt = now)
        // The state the successful update started from decides whether the tracker existed.
        val before =
            state.getAndUpdate { s ->
                if (s.hasTracker(trackerId)) s.copy(events = s.events + event) else s
            }
        return event.takeIf { before.hasTracker(trackerId) }
    }

    override suspend fun updateEventTime(
        eventId: String,
        occurredAt: Instant,
    ) {
        val now = clock.instant()
        state.update { s ->
            val moved = { event: TrackerEvent -> event.copy(occurredAt = occurredAt, updatedAt = now) }
            s.copy(events = s.events.map { event -> if (event.id == eventId) moved(event) else event })
        }
    }

    override suspend fun deleteEvent(eventId: String) {
        state.update { s -> s.copy(events = s.events.filter { it.id != eventId }) }
    }
}
