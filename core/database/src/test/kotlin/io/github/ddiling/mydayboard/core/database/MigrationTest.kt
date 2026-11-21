package io.github.ddiling.mydayboard.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Opens a real version-1 database built from the committed `1.json`, fills it with raw SQL (the v1 entities
 * no longer exist in code) and runs the migration to version 2, which also validates the result against `2.json`.
 */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), MyDayboardDatabase::class.java)

    @Test
    fun `trackers keep all column values after migrating from 1 to 2`() {
        givenVersion1 { insertWaterTracker(deletedAt = DELETED_AT) }

        val row =
            migrateTo2().queryRow(
                "SELECT id, name, type, started_at, created_at, updated_at, deleted_at FROM trackers",
            )

        assertEquals(
            listOf("water", "Water", "DAYS_SINCE", "$STARTED_AT", "$CREATED_AT", "$UPDATED_AT", "$DELETED_AT"),
            row,
        )
    }

    @Test
    fun `events keep all column values after migrating from 1 to 2`() {
        givenVersion1 {
            insertWaterTracker(deletedAt = null)
            execSQL(
                "INSERT INTO tracker_events (id, tracker_id, occurred_at, created_at, updated_at, deleted_at) " +
                    "VALUES ('sip', 'water', $OCCURRED_AT, $CREATED_AT, $UPDATED_AT, $DELETED_AT)",
            )
        }

        val row =
            migrateTo2().queryRow(
                "SELECT id, tracker_id, occurred_at, created_at, updated_at, deleted_at FROM tracker_events",
            )

        assertEquals(listOf("sip", "water", "$OCCURRED_AT", "$CREATED_AT", "$UPDATED_AT", "$DELETED_AT"), row)
    }

    @Test
    fun `existing trackers do not ask for a reason after migrating from 1 to 2`() {
        givenVersion1 { insertWaterTracker(deletedAt = null) }

        val row = migrateTo2().queryRow("SELECT ask_for_reason FROM trackers")

        assertEquals(listOf("0"), row)
    }

    @Test
    fun `the reasons table exists and is empty after migrating from 1 to 2`() {
        givenVersion1 { insertWaterTracker(deletedAt = null) }

        val row = migrateTo2().queryRow("SELECT COUNT(*) FROM tracker_reasons")

        assertEquals(listOf("0"), row)
    }

    private fun givenVersion1(fill: SupportSQLiteDatabase.() -> Unit) =
        helper.createDatabase(TEST_DB, 1).use { it.fill() }

    private fun migrateTo2(): SupportSQLiteDatabase = helper.runMigrationsAndValidate(TEST_DB, 2, true)

    private fun SupportSQLiteDatabase.insertWaterTracker(deletedAt: Long?) =
        execSQL(
            "INSERT INTO trackers (id, name, type, started_at, created_at, updated_at, deleted_at) " +
                "VALUES ('water', 'Water', 'DAYS_SINCE', $STARTED_AT, $CREATED_AT, $UPDATED_AT, $deletedAt)",
        )

    /** The single row of [sql] as strings, so values of every column type compare the same way. */
    private fun SupportSQLiteDatabase.queryRow(sql: String): List<String?> =
        use { database ->
            database.query(sql).use { cursor ->
                check(cursor.count == 1 && cursor.moveToFirst()) { "expected one row, got ${cursor.count}" }
                (0 until cursor.columnCount).map { cursor.getString(it) }
            }
        }

    private companion object {
        const val TEST_DB = "migration-test.db"

        // Distinct values, so a column copied into the wrong place would show.
        const val STARTED_AT = T0
        const val CREATED_AT = T0 + 1
        const val UPDATED_AT = T0 + 2
        const val DELETED_AT = T0 + 3
        const val OCCURRED_AT = T0 + 4
    }
}
