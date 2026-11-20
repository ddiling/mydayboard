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
    /** All trackers ordered by `createdAt` ascending, ties broken by `id` ascending so the order is stable. */
    fun observeTrackers(): Flow<List<Tracker>>

    /** The tracker with [id], or null when it is missing or deleted. */
    fun observeTracker(id: String): Flow<Tracker?>

    /**
     * Events of [trackerId] ordered by `occurredAt` descending, ties broken by `id` descending so the order is
     * stable; empty when the tracker is missing or deleted.
     */
    fun observeEvents(trackerId: String): Flow<List<TrackerEvent>>

    /**
     * Events of all non-deleted trackers, for statistics across trackers. Newest first by `occurredAt`;
     * events with the same time are ordered by id descending, so every implementation lists them alike.
     */
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

    /**
     * Brings a deleted event back: clears its deletion and bumps `updatedAt` from the injected clock.
     * It is what Undo of a deleted entry uses. Changes nothing, without throwing, for an unknown id,
     * for an event that is not deleted, and for an event of a deleted tracker (that tracker still
     * hides its events).
     */
    suspend fun restoreEvent(eventId: String)
}
