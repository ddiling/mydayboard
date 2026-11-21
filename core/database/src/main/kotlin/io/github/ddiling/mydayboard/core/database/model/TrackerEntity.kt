package io.github.ddiling.mydayboard.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A row of `trackers`. All times are epoch milliseconds.
 *
 * Rows are never deleted: [deletedAt] marks a soft delete, so a tracker can be restored
 * and its events keep a valid parent. Every DAO read filters soft-deleted rows out,
 * so code above the database never sees them.
 */
@Entity(tableName = "trackers")
data class TrackerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: TrackerEntityType,
    /** When counting starts; a "days since" tracker counts from here. */
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    /**
     * Whether logging asks for a reason first. The SQL default is needed because the column is NOT NULL and
     * was added by a migration: existing rows get `0` (off), so they keep logging with one tap.
     */
    @ColumnInfo(name = "ask_for_reason", defaultValue = "0") val askForReason: Boolean = false,
)

/** Stored as the constant's name (`TEXT`), so never rename a constant without a migration. */
enum class TrackerEntityType {
    EVENT_COUNTER,
    DAYS_SINCE,
}
