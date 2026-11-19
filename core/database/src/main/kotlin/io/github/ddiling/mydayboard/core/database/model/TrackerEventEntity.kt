package io.github.ddiling.mydayboard.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A row of `tracker_events`: one occurrence counted by a tracker. All times are epoch milliseconds.
 *
 * The foreign key has no cascade because nothing is ever hard-deleted: a soft-deleted
 * tracker keeps its events, and the DAOs hide them through a join instead.
 * The `(tracker_id, occurred_at)` index serves the per-tracker query, which filters by
 * tracker and sorts by time, and the foreign-key check.
 */
@Entity(
    tableName = "tracker_events",
    foreignKeys = [
        ForeignKey(
            entity = TrackerEntity::class,
            parentColumns = ["id"],
            childColumns = ["tracker_id"],
        ),
    ],
    indices = [Index("tracker_id", "occurred_at")],
)
data class TrackerEventEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "tracker_id") val trackerId: String,
    @ColumnInfo(name = "occurred_at") val occurredAt: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
)
