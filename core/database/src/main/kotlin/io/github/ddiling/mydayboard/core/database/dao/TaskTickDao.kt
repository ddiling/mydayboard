package io.github.ddiling.mydayboard.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import io.github.ddiling.mydayboard.core.database.model.TaskTickEntity
import kotlinx.coroutines.flow.Flow

/**
 * Ticks that are not soft-deleted, of tasks that are not soft-deleted. Every write is one statement that checks
 * its own conditions; a write changes nothing (returns 0, or -1 for an insert) when the tick or its task is
 * missing or deleted.
 */
@Dao
interface TaskTickDao {
    /** Empty while the task is soft-deleted. Ordered by day, then slot, then `id` so the order is stable. */
    @Query(
        """
        SELECT k.* FROM task_ticks AS k
        INNER JOIN tasks AS t ON t.id = k.task_id
        WHERE k.task_id = :taskId AND k.deleted_at IS NULL AND t.deleted_at IS NULL
        ORDER BY k.day, k.slot_minute, k.id
        """,
    )
    fun observeForTask(taskId: String): Flow<List<TaskTickEntity>>

    @Query(
        """
        SELECT k.* FROM task_ticks AS k
        INNER JOIN tasks AS t ON t.id = k.task_id
        WHERE k.deleted_at IS NULL AND t.deleted_at IS NULL
        ORDER BY k.day, k.slot_minute, k.id
        """,
    )
    fun observeAllLive(): Flow<List<TaskTickEntity>>

    /** Live ticks of one day, empty while the task is soft-deleted; ordered by slot, then `id`. */
    @Query(
        """
        SELECT k.* FROM task_ticks AS k
        INNER JOIN tasks AS t ON t.id = k.task_id
        WHERE k.task_id = :taskId AND k.day = :day AND k.deleted_at IS NULL AND t.deleted_at IS NULL
        ORDER BY k.slot_minute, k.id
        """,
    )
    suspend fun findLiveForDay(
        taskId: String,
        day: Long,
    ): List<TaskTickEntity>

    /**
     * Inserts only while the task is live and, for a timed slot, no live tick holds it, all in this one
     * statement so parallel calls cannot both pass the check. Returns the new row id, or -1 when it inserts
     * nothing. A NULL [slotMinute] is a counted tick, which may repeat.
     */
    @Query(
        """
        INSERT INTO task_ticks (id, task_id, day, slot_minute, created_at, updated_at)
        SELECT :id, :taskId, :day, :slotMinute, :now, :now
        WHERE EXISTS (SELECT 1 FROM tasks WHERE id = :taskId AND deleted_at IS NULL)
        AND (:slotMinute IS NULL OR NOT EXISTS (
            SELECT 1 FROM task_ticks AS twin
            WHERE twin.task_id = :taskId AND twin.day = :day
            AND twin.slot_minute = :slotMinute AND twin.deleted_at IS NULL
        ))
        """,
    )
    suspend fun insertForLiveTask(
        id: String,
        taskId: String,
        day: Long,
        slotMinute: Int?,
        now: Long,
    ): Long

    @Query(
        """
        UPDATE task_ticks SET updated_at = :now, deleted_at = :now
        WHERE id = :id AND deleted_at IS NULL
        AND task_id IN (SELECT id FROM tasks WHERE deleted_at IS NULL)
        """,
    )
    suspend fun softDeleteIfLive(
        id: String,
        now: Long,
    ): Int

    /**
     * Brings a soft-deleted tick back; a deleted task keeps hiding its ticks. A timed tick stays deleted when a
     * live tick already holds its slot (`=` is never true for a NULL slot, so counted ticks always come back).
     */
    @Query(
        """
        UPDATE task_ticks SET updated_at = :now, deleted_at = NULL
        WHERE id = :id AND deleted_at IS NOT NULL
        AND task_id IN (SELECT id FROM tasks WHERE deleted_at IS NULL)
        AND NOT EXISTS (
            SELECT 1 FROM task_ticks AS twin
            WHERE twin.task_id = task_ticks.task_id AND twin.day = task_ticks.day
            AND twin.slot_minute = task_ticks.slot_minute AND twin.deleted_at IS NULL
        )
        """,
    )
    suspend fun restoreIfDeleted(
        id: String,
        now: Long,
    ): Int
}
