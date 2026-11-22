package io.github.ddiling.mydayboard.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.github.ddiling.mydayboard.core.database.model.TaskEntity
import kotlinx.coroutines.flow.Flow

/** Every write is one conditional statement and returns 0 when the task is missing or soft-deleted. */
@Dao
interface TaskDao {
    /** Archived tasks stay in the list: archiving hides a task from "today", not from the app. */
    @Query("SELECT * FROM tasks WHERE deleted_at IS NULL ORDER BY created_at, id")
    fun observeLive(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id AND deleted_at IS NULL")
    fun observeLiveById(id: String): Flow<TaskEntity?>

    /** Fails if the id already exists or `categoryId` names no category (foreign key). */
    @Insert
    suspend fun insert(entity: TaskEntity)

    /**
     * Rewrites every editable field of a live task from [entity] and stamps `updated_at` with [now]; returns 0 when
     * the task is missing or soft-deleted. `created_at`, `archived_at` and `deleted_at` keep their stored values.
     * Room cannot bind `:entity.field` in a query, so this is a transaction: the read and the write cannot be split
     * by a concurrent delete.
     */
    @Transaction
    suspend fun updateIfLive(
        entity: TaskEntity,
        now: Long,
    ): Int {
        val stored = findLive(entity.id) ?: return 0
        val kept = entity.copy(createdAt = stored.createdAt, archivedAt = stored.archivedAt, deletedAt = null)
        return update(kept.copy(updatedAt = now))
    }

    @Query("SELECT * FROM tasks WHERE id = :id AND deleted_at IS NULL")
    suspend fun findLive(id: String): TaskEntity?

    @Update
    suspend fun update(entity: TaskEntity): Int

    @Query("UPDATE tasks SET archived_at = :archivedAt, updated_at = :now WHERE id = :id AND deleted_at IS NULL")
    suspend fun setArchivedAt(
        id: String,
        archivedAt: Long?,
        now: Long,
    ): Int

    @Query("UPDATE tasks SET updated_at = :now, deleted_at = :now WHERE id = :id AND deleted_at IS NULL")
    suspend fun softDeleteIfLive(
        id: String,
        now: Long,
    ): Int
}
