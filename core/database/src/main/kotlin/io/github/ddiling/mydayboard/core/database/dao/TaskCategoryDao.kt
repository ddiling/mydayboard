package io.github.ddiling.mydayboard.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import io.github.ddiling.mydayboard.core.database.model.TaskCategoryEntity
import kotlinx.coroutines.flow.Flow

/** Every write is one conditional statement and returns 0 when its condition does not hold. */
@Dao
interface TaskCategoryDao {
    /** Oldest first; `id` breaks ties so the order is stable. */
    @Query("SELECT * FROM task_categories WHERE deleted_at IS NULL ORDER BY created_at, id")
    fun observeLive(): Flow<List<TaskCategoryEntity>>

    /** Fails if the id already exists. */
    @Insert
    suspend fun insert(entity: TaskCategoryEntity)

    @Query("UPDATE task_categories SET name = :name, updated_at = :now WHERE id = :id AND deleted_at IS NULL")
    suspend fun renameIfLive(
        id: String,
        name: String,
        now: Long,
    ): Int

    @Query("UPDATE task_categories SET updated_at = :now, deleted_at = :now WHERE id = :id AND deleted_at IS NULL")
    suspend fun softDeleteIfLive(
        id: String,
        now: Long,
    ): Int

    @Query("UPDATE task_categories SET updated_at = :now, deleted_at = NULL WHERE id = :id AND deleted_at IS NOT NULL")
    suspend fun restoreIfDeleted(
        id: String,
        now: Long,
    ): Int
}
