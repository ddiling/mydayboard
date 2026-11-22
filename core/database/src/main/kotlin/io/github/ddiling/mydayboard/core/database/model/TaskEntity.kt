package io.github.ddiling.mydayboard.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** How often a task is due. Stored by name, so reordering the entries never changes saved data. */
enum class TaskEntityRepeatType { TIMES_PER_DAY, EVERY_DAY, WEEKDAYS, EVERY_N_DAYS, MONTHLY, ONE_TIME }

/** Whether the next "every N days" due date counts from the last tick or from the fixed calendar. */
enum class TaskEntityIntervalMode { FROM_LAST_DONE, FIXED_CALENDAR }

/** What happens to a due task nobody ticked: it stays due, or it expires with its day. */
enum class TaskEntityOnMiss { STAYS, EXPIRES }

/**
 * A row of `tasks`. Times (`created_at`, `updated_at`, `deleted_at`, `archived_at`) are epoch milliseconds; the
 * calendar fields (`start_date`, `anchor_date`) are epoch days, so a change of time zone never moves a due date.
 *
 * The repeat rule is flat nullable columns, not a child table: each repeat type reads only its own columns,
 * and one row is then one atomic write. `weekdays` is a bit mask (bit 0 = Monday ... bit 6 = Sunday).
 * `times` and `time_of_day` hold "HH:mm" text. No cascade on the category: deleting a category never touches tasks.
 */
@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = TaskCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
        ),
    ],
    indices = [Index("category_id")],
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "category_id") val categoryId: String?,
    @ColumnInfo(name = "repeat_type") val repeatType: TaskEntityRepeatType,
    val times: String?,
    val weekdays: Int?,
    @ColumnInfo(name = "interval_days") val intervalDays: Int?,
    @ColumnInfo(name = "interval_mode") val intervalMode: TaskEntityIntervalMode?,
    @ColumnInfo(name = "anchor_date") val anchorDate: Long?,
    @ColumnInfo(name = "day_of_month") val dayOfMonth: Int?,
    @ColumnInfo(name = "time_of_day") val timeOfDay: String?,
    val note: String?,
    val goal: Int?,
    @ColumnInfo(name = "on_miss") val onMiss: TaskEntityOnMiss,
    @ColumnInfo(name = "start_date") val startDate: Long,
    @ColumnInfo(name = "archived_at") val archivedAt: Long?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
)
