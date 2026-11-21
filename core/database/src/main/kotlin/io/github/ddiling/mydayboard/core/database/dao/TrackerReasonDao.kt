package io.github.ddiling.mydayboard.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import io.github.ddiling.mydayboard.core.database.model.TrackerReasonEntity
import kotlinx.coroutines.flow.Flow

/**
 * Reasons that are not soft-deleted, of trackers that are not soft-deleted. Every write is one statement that checks
 * its own conditions, so a concurrent delete cannot be undone by a stale read-modify-write; a write changes nothing
 * (returns 0, or -1 for an insert) when the reason or its tracker is missing or deleted; a duplicate id still fails.
 */
@Dao
interface TrackerReasonDao {
    /** Oldest first; `id` breaks ties so the order is stable. Empty while the tracker is soft-deleted. */
    @Query(
        """
        SELECT r.* FROM tracker_reasons AS r
        INNER JOIN trackers AS t ON t.id = r.tracker_id
        WHERE r.tracker_id = :trackerId AND r.deleted_at IS NULL AND t.deleted_at IS NULL
        ORDER BY r.created_at, r.id
        """,
    )
    fun observeActiveForTracker(trackerId: String): Flow<List<TrackerReasonEntity>>

    /** Fails if the id already exists or `trackerId` names no tracker (foreign key). */
    @Insert
    suspend fun insert(entity: TrackerReasonEntity)

    /**
     * Inserts only while the tracker is live; returns the new row id, or -1 when it inserts nothing.
     * Like [insert], it fails on a duplicate id.
     */
    @Query(
        """
        INSERT INTO tracker_reasons (id, tracker_id, text, created_at, updated_at)
        SELECT :id, :trackerId, :text, :now, :now
        WHERE EXISTS (SELECT 1 FROM trackers WHERE id = :trackerId AND deleted_at IS NULL)
        """,
    )
    suspend fun insertForActiveTracker(
        id: String,
        trackerId: String,
        text: String,
        now: Long,
    ): Long

    @Query(
        """
        UPDATE tracker_reasons SET text = :text, updated_at = :now
        WHERE id = :id AND deleted_at IS NULL
        AND tracker_id IN (SELECT id FROM trackers WHERE deleted_at IS NULL)
        """,
    )
    suspend fun updateTextIfActive(
        id: String,
        text: String,
        now: Long,
    ): Int

    @Query(
        """
        UPDATE tracker_reasons SET updated_at = :now, deleted_at = :now
        WHERE id = :id AND deleted_at IS NULL
        AND tracker_id IN (SELECT id FROM trackers WHERE deleted_at IS NULL)
        """,
    )
    suspend fun softDeleteIfActive(
        id: String,
        now: Long,
    ): Int

    /** Brings a soft-deleted reason back; a deleted tracker keeps hiding its reasons. */
    @Query(
        """
        UPDATE tracker_reasons SET updated_at = :now, deleted_at = NULL
        WHERE id = :id AND deleted_at IS NOT NULL
        AND tracker_id IN (SELECT id FROM trackers WHERE deleted_at IS NULL)
        """,
    )
    suspend fun restoreIfDeleted(
        id: String,
        now: Long,
    ): Int
}
