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
}
