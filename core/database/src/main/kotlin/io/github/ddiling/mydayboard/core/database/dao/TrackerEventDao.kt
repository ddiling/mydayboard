package io.github.ddiling.mydayboard.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.github.ddiling.mydayboard.core.database.model.TrackerEventEntity
import kotlinx.coroutines.flow.Flow

/**
 * Tracker events that are not soft-deleted. Deleting is an [update] that sets `deletedAt`.
 * Lists are newest first; `id` breaks ties so the order is stable.
 */
@Dao
interface TrackerEventDao {
    @Query(
        """
        SELECT * FROM tracker_events
        WHERE tracker_id = :trackerId AND deleted_at IS NULL
        ORDER BY occurred_at DESC, id DESC
        """,
    )
    fun observeActiveForTracker(trackerId: String): Flow<List<TrackerEventEntity>>

    /** Also hides the events of soft-deleted trackers, which keep their own `deleted_at` unset. */
    @Query(
        """
        SELECT e.* FROM tracker_events AS e
        INNER JOIN trackers AS t ON t.id = e.tracker_id
        WHERE e.deleted_at IS NULL AND t.deleted_at IS NULL
        ORDER BY e.occurred_at DESC, e.id DESC
        """,
    )
    fun observeActiveForActiveTrackers(): Flow<List<TrackerEventEntity>>

    @Query("SELECT * FROM tracker_events WHERE id = :id AND deleted_at IS NULL")
    suspend fun getActiveById(id: String): TrackerEventEntity?

    /** Fails if the id already exists or `trackerId` names no tracker (foreign key). */
    @Insert
    suspend fun insert(entity: TrackerEventEntity)

    @Update
    suspend fun update(entity: TrackerEventEntity)

    /**
     * Inserts only while the tracker exists and is not deleted, checked in the same statement so a concurrent
     * tracker delete cannot slip in between. Returns the new row id, or -1 when nothing was inserted.
     */
    @Query(
        """
        INSERT INTO tracker_events (id, tracker_id, occurred_at, created_at, updated_at)
        SELECT :id, :trackerId, :occurredAt, :now, :now
        WHERE EXISTS (SELECT 1 FROM trackers WHERE id = :trackerId AND deleted_at IS NULL)
        """,
    )
    suspend fun insertForActiveTracker(
        id: String,
        trackerId: String,
        occurredAt: Long,
        now: Long,
    ): Long

    /** Changes nothing (returns 0) when the event or its tracker is missing or deleted. */
    @Query(
        """
        UPDATE tracker_events SET occurred_at = :occurredAt, updated_at = :now
        WHERE id = :id AND deleted_at IS NULL
        AND tracker_id IN (SELECT id FROM trackers WHERE deleted_at IS NULL)
        """,
    )
    suspend fun updateOccurredAtIfActive(
        id: String,
        occurredAt: Long,
        now: Long,
    ): Int

    /** Changes nothing (returns 0) when the event or its tracker is missing or deleted. */
    @Query(
        """
        UPDATE tracker_events SET updated_at = :now, deleted_at = :now
        WHERE id = :id AND deleted_at IS NULL
        AND tracker_id IN (SELECT id FROM trackers WHERE deleted_at IS NULL)
        """,
    )
    suspend fun softDeleteIfActive(
        id: String,
        now: Long,
    ): Int
}
