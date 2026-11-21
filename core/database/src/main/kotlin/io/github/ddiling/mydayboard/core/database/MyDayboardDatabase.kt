package io.github.ddiling.mydayboard.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.ddiling.mydayboard.core.database.dao.TrackerDao
import io.github.ddiling.mydayboard.core.database.dao.TrackerEventDao
import io.github.ddiling.mydayboard.core.database.dao.TrackerReasonDao
import io.github.ddiling.mydayboard.core.database.model.TrackerEntity
import io.github.ddiling.mydayboard.core.database.model.TrackerEventEntity
import io.github.ddiling.mydayboard.core.database.model.TrackerReasonEntity

/**
 * The app's local database.
 *
 * `exportSchema` writes every version's schema to `core/database/schemas/`; those files are
 * committed, because a migration can only be tested against the exact old schema.
 * Changing an entity means bumping [version] and adding a migration.
 *
 * Version 2 adds `tracker_reasons` and `trackers.ask_for_reason`. An [AutoMigration] is enough: both are pure
 * additions, and the new NOT NULL column has a default, so Room can generate the SQL from the two schemas.
 */
@Database(
    entities = [TrackerEntity::class, TrackerEventEntity::class, TrackerReasonEntity::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class MyDayboardDatabase : RoomDatabase() {
    abstract fun trackerDao(): TrackerDao

    abstract fun trackerEventDao(): TrackerEventDao

    abstract fun trackerReasonDao(): TrackerReasonDao

    companion object {
        const val NAME = "mydayboard.db"
    }
}
