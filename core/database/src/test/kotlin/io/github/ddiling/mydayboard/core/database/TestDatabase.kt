package io.github.ddiling.mydayboard.core.database

import androidx.room.Room
import io.github.ddiling.mydayboard.core.database.model.TrackerEntity
import io.github.ddiling.mydayboard.core.database.model.TrackerEntityType
import io.github.ddiling.mydayboard.core.database.model.TrackerEventEntity
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

/** Reads `updated_at` and `deleted_at` of any row, deleted or not: the DAO reads hide deleted rows. */
internal fun MyDayboardDatabase.updatedAndDeletedAt(
    table: String,
    id: String,
): Pair<Long, Long?> =
    openHelper.readableDatabase.query("SELECT updated_at, deleted_at FROM $table WHERE id = '$id'").use { cursor ->
        check(cursor.moveToFirst()) { "no row $id in $table" }
        cursor.getLong(0) to if (cursor.isNull(1)) null else cursor.getLong(1)
    }
