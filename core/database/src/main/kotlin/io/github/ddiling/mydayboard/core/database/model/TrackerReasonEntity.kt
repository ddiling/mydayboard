package io.github.ddiling.mydayboard.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A row of `tracker_reasons`: one reason the user wrote down for a tracker. All times are epoch milliseconds.
 *
 * A separate table, not a column on `trackers`, because a tracker has any number of reasons and each one is
 * edited, deleted and restored on its own. Like events, the foreign key has no cascade because nothing is ever
 * hard-deleted: a soft-deleted tracker keeps its reasons, and the DAO hides them through a join instead.
 * The `tracker_id` index serves the per-tracker query and the foreign-key check.
 */
@Entity(
    tableName = "tracker_reasons",
    foreignKeys = [
        ForeignKey(
            entity = TrackerEntity::class,
            parentColumns = ["id"],
            childColumns = ["tracker_id"],
        ),
    ],
    indices = [Index("tracker_id")],
)
data class TrackerReasonEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "tracker_id") val trackerId: String,
    val text: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
)
