package io.github.ddiling.mydayboard.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.github.ddiling.mydayboard.core.database.model.TrackerEntity
import kotlinx.coroutines.flow.Flow

/** Trackers that are not soft-deleted. Deleting is an [update] that sets `deletedAt`. */
@Dao
interface TrackerDao {
    /** Oldest first; `id` breaks ties so the order is stable. */
    @Query("SELECT * FROM trackers WHERE deleted_at IS NULL ORDER BY created_at, id")
    fun observeActive(): Flow<List<TrackerEntity>>

    /** Emits `null` while the tracker does not exist or is soft-deleted. */
    @Query("SELECT * FROM trackers WHERE id = :id AND deleted_at IS NULL")
    fun observeActiveById(id: String): Flow<TrackerEntity?>

    @Query("SELECT * FROM trackers WHERE id = :id AND deleted_at IS NULL")
    suspend fun getActiveById(id: String): TrackerEntity?

    /** Fails if the id already exists: ids are generated once and never reused. */
    @Insert
    suspend fun insert(entity: TrackerEntity)

    @Update
    suspend fun update(entity: TrackerEntity)
}
