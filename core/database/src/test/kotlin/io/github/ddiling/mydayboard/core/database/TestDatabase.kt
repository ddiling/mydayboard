package io.github.ddiling.mydayboard.core.database

import androidx.room.Room
import io.github.ddiling.mydayboard.core.database.model.TaskCategoryEntity
import io.github.ddiling.mydayboard.core.database.model.TaskEntity
import io.github.ddiling.mydayboard.core.database.model.TaskEntityOnMiss
import io.github.ddiling.mydayboard.core.database.model.TaskEntityRepeatType
import io.github.ddiling.mydayboard.core.database.model.TaskTickEntity
import io.github.ddiling.mydayboard.core.database.model.TrackerEntity
import io.github.ddiling.mydayboard.core.database.model.TrackerEntityType
import io.github.ddiling.mydayboard.core.database.model.TrackerEventEntity
import io.github.ddiling.mydayboard.core.database.model.TrackerReasonEntity
import org.robolectric.RuntimeEnvironment

/** A fresh in-memory database, so tests share no state and need no clean-up of files. */
internal fun inMemoryDatabase(): MyDayboardDatabase =
    Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), MyDayboardDatabase::class.java).build()

/** Fixed instants instead of a clock: tests depend only on their order. */
internal const val T0 = 1_700_000_000_000L

internal fun tracker(
    id: String,
    createdAt: Long = T0,
    deletedAt: Long? = null,
) = TrackerEntity(
    id = id,
    name = "Tracker $id",
    type = TrackerEntityType.EVENT_COUNTER,
    startedAt = createdAt,
    createdAt = createdAt,
    updatedAt = createdAt,
    deletedAt = deletedAt,
)

internal fun event(
    id: String,
    trackerId: String,
    occurredAt: Long = T0,
    deletedAt: Long? = null,
) = TrackerEventEntity(
    id = id,
    trackerId = trackerId,
    occurredAt = occurredAt,
    createdAt = occurredAt,
    updatedAt = occurredAt,
    deletedAt = deletedAt,
)

internal fun reason(
    id: String,
    trackerId: String,
    text: String = "Reason $id",
    createdAt: Long = T0,
    deletedAt: Long? = null,
) = TrackerReasonEntity(
    id = id,
    trackerId = trackerId,
    text = text,
    createdAt = createdAt,
    updatedAt = createdAt,
    deletedAt = deletedAt,
)

/** A fixed epoch day (2026-01-01), so task tests never read a clock. */
internal const val DAY0 = 20_454L

internal fun category(
    id: String,
    name: String = "Category $id",
    createdAt: Long = T0,
    deletedAt: Long? = null,
) = TaskCategoryEntity(id = id, name = name, createdAt = createdAt, updatedAt = createdAt, deletedAt = deletedAt)

internal fun task(
    id: String,
    name: String = "Task $id",
    repeatType: TaskEntityRepeatType = TaskEntityRepeatType.EVERY_DAY,
    categoryId: String? = null,
    deletedAt: Long? = null,
) = TaskEntity(
    id = id,
    name = name,
    categoryId = categoryId,
    repeatType = repeatType,
    times = null,
    weekdays = null,
    intervalDays = null,
    intervalMode = null,
    anchorDate = null,
    dayOfMonth = null,
    timeOfDay = null,
    note = null,
    goal = null,
    onMiss = TaskEntityOnMiss.STAYS,
    startDate = DAY0,
    archivedAt = null,
    createdAt = T0,
    updatedAt = T0,
    deletedAt = deletedAt,
)

internal fun tick(
    id: String,
    taskId: String,
    day: Long = DAY0,
    slotMinute: Int? = null,
    deletedAt: Long? = null,
) = TaskTickEntity(
    id = id,
    taskId = taskId,
    day = day,
    slotMinute = slotMinute,
    createdAt = T0,
    updatedAt = T0,
    deletedAt = deletedAt,
)

/** Reads `updated_at` and `deleted_at` of any row, deleted or not: the DAO reads hide deleted rows. */
internal fun MyDayboardDatabase.updatedAndDeletedAt(
    table: String,
    id: String,
): Pair<Long, Long?> =
    openHelper.readableDatabase.query("SELECT updated_at, deleted_at FROM $table WHERE id = '$id'").use { cursor ->
        check(cursor.moveToFirst()) { "no row $id in $table" }
        cursor.getLong(0) to if (cursor.isNull(1)) null else cursor.getLong(1)
    }
