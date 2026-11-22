package io.github.ddiling.mydayboard.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A row of `task_ticks`: one "done" mark. `day` is an epoch day and `slot_minute` the minute of the day of a
 * "several times" slot (null when the task has no times). One table serves plain, several-times and count-goal
 * tasks: the number of live rows for a day is the count. No cascade: a soft-deleted task hides its ticks by join.
 */
@Entity(
    tableName = "task_ticks",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
        ),
    ],
    indices = [Index("task_id", "day")],
)
data class TaskTickEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "task_id") val taskId: String,
    val day: Long,
    @ColumnInfo(name = "slot_minute") val slotMinute: Int?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
)
