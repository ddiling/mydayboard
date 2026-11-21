package io.github.ddiling.mydayboard.core.domain.tracker

import kotlinx.coroutines.flow.Flow

/**
 * Storage for the ask-for-reason flag of a tracker and for its reasons. It is separate from [TrackerRepository]
 * so a screen that only deals with reasons depends on reasons only, and neither interface grows past what one
 * type can reasonably do.
 *
 * It follows the contract of [TrackerRepository]: no validation inside (callers use [validateReasonText]),
 * `id`, `createdAt` and `updatedAt` come from the injected `IdGenerator` and `Clock`, operations on unknown or
 * deleted ids are no-ops that do not throw, and observed flows apply `distinctUntilChanged()`. Deletes are soft
 * (so sync can propagate them) but invisible here.
 */
interface TrackerReasonRepository {
    /**
     * Reasons of [trackerId] ordered by `createdAt` ascending, ties broken by `id` ascending so the order is
     * stable; empty when the tracker is missing or deleted.
     */
    fun observeReasons(trackerId: String): Flow<List<TrackerReason>>

    /** Turns the ask-for-reason flag of the tracker on or off and bumps its `updatedAt`. */
    suspend fun setAskForReason(
        id: String,
        enabled: Boolean,
    )

    /** Adds a reason, or returns null (and stores nothing) when the tracker is unknown or deleted. */
    suspend fun addReason(
        trackerId: String,
        text: String,
    ): TrackerReason?

    /** Changes the reason's text and bumps its `updatedAt`. */
    suspend fun updateReason(
        reasonId: String,
        text: String,
    )

    /** Soft-deletes the reason. */
    suspend fun deleteReason(reasonId: String)

    /**
     * Brings a deleted reason back (Undo): clears its deletion and bumps `updatedAt` from the injected clock.
     * Changes nothing for an unknown id, a reason that is not deleted, or a reason of a deleted tracker.
     */
    suspend fun restoreReason(reasonId: String)
}
