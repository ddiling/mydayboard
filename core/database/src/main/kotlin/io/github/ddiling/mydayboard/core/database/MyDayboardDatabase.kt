package io.github.ddiling.mydayboard.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.ddiling.mydayboard.core.database.dao.TrackerDao
import io.github.ddiling.mydayboard.core.database.dao.TrackerEventDao
import io.github.ddiling.mydayboard.core.database.model.TrackerEntity
import io.github.ddiling.mydayboard.core.database.model.TrackerEventEntity

/**
 * The app's local database.
 *
 * `exportSchema` writes every version's schema to `core/database/schemas/`; those files are
 * committed, because a migration can only be tested against the exact old schema.
 * Changing an entity means bumping [version] and adding a migration.
 */
@Database(
    entities = [TrackerEntity::class, TrackerEventEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class MyDayboardDatabase : RoomDatabase() {
    abstract fun trackerDao(): TrackerDao

    abstract fun trackerEventDao(): TrackerEventDao

    companion object {
        const val NAME = "mydayboard.db"
    }
}
