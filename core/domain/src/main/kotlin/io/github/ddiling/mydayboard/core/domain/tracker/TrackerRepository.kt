package io.github.ddiling.mydayboard.core.domain.tracker

import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Storage for trackers and their events. The domain never sees deleted rows: deletes are soft in storage
 * (so sync can propagate them) but invisible here.
 *
 * Contract shared by every implementation (the in-memory fake and the Room one run the same contract tests):
 * - The repository does NOT validate. Callers validate names and times with the domain functions first.
 * - It sets `id`, `createdAt` and `updatedAt` itself, from the injected `IdGenerator` and `Clock`.
 * - Operations on unknown or deleted ids are no-ops: they change nothing and do not throw. A screen may
 *   still show an item that another device just deleted, and that must not crash it.
 * - Observed flows apply `distinctUntilChanged()`, because Room may emit an identical list again after an
 *   unrelated write to the same table. Collectors are therefore only woken by changes to what they observe.
 */
interface TrackerRepository {
    /** All trackers, ordered by `createdAt` ascending. */
    fun observeTrackers(): Flow<List<Tracker>>

    /** The tracker with [id], or null when it is missing or deleted. */
    fun observeTracker(id: String): Flow<Tracker?>

    /** Events of [trackerId] ordered by `occurredAt` descending; empty when the tracker is missing or deleted. */
    fun observeEvents(trackerId: String): Flow<List<TrackerEvent>>

    /** Events of all non-deleted trackers, for statistics across trackers. */
    fun observeAllEvents(): Flow<List<TrackerEvent>>

    /** Creates a tracker; [startedAt] is stored as given. */
    suspend fun createTracker(
        name: String,
        type: TrackerType,
        startedAt: Instant,
    ): Tracker

    /** Renames the tracker and bumps its `updatedAt`. */
    suspend fun renameTracker(
        id: String,
        name: String,
    )

    /** Soft-deletes the tracker; its events disappear with it. */
    suspend fun deleteTracker(id: String)

    /** Records an event, or returns null (and stores nothing) when the tracker is unknown or deleted. */
    suspend fun addEvent(
        trackerId: String,
        occurredAt: Instant,
    ): TrackerEvent?

    /** Corrects when the event happened and bumps its `updatedAt`. */
    suspend fun updateEventTime(
        eventId: String,
        occurredAt: Instant,
    )

    /** Soft-deletes the event. Also what Undo of an added event uses. */
    suspend fun deleteEvent(eventId: String)
}
