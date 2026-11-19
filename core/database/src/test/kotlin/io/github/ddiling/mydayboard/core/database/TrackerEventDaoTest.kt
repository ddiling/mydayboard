package io.github.ddiling.mydayboard.core.database

import android.database.sqlite.SQLiteConstraintException
import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TrackerEventDaoTest {
    private val database = inMemoryDatabase()
    private val trackerDao = database.trackerDao()
    private val dao = database.trackerEventDao()

    @After
    fun closeDatabase() = database.close()

    @Test
    fun `observeActiveForTracker hides soft-deleted events`() =
        runTest {
            trackerDao.insert(tracker("t"))
            dao.insert(event("active", "t"))
            dao.insert(event("deleted", "t", deletedAt = T0 + 1))

            val ids = dao.observeActiveForTracker("t").first().map { it.id }

            assertEquals(listOf("active"), ids)
        }

    @Test
    fun `observeActiveForTracker lists only that tracker's events, newest first`() =
        runTest {
            trackerDao.insert(tracker("t"))
            trackerDao.insert(tracker("other"))
            dao.insert(event("older", "t", occurredAt = T0))
            dao.insert(event("newer", "t", occurredAt = T0 + 1))
            dao.insert(event("foreign", "other", occurredAt = T0 + 2))

            val ids = dao.observeActiveForTracker("t").first().map { it.id }

            assertEquals(listOf("newer", "older"), ids)
        }

    @Test
    fun `observeActiveForActiveTrackers hides events of soft-deleted trackers`() =
        runTest {
            trackerDao.insert(tracker("active"))
            trackerDao.insert(tracker("deleted", deletedAt = T0 + 1))
            dao.insert(event("kept", "active"))
            dao.insert(event("orphaned", "deleted"))

            val ids = dao.observeActiveForActiveTrackers().first().map { it.id }

            assertEquals(listOf("kept"), ids)
        }

    @Test
    fun `observeActiveForActiveTrackers hides soft-deleted events, newest first`() =
        runTest {
            trackerDao.insert(tracker("a"))
            trackerDao.insert(tracker("b"))
            dao.insert(event("older", "a", occurredAt = T0))
            dao.insert(event("newer", "b", occurredAt = T0 + 1))
            dao.insert(event("deleted", "a", occurredAt = T0 + 2, deletedAt = T0 + 3))

            val ids = dao.observeActiveForActiveTrackers().first().map { it.id }

            assertEquals(listOf("newer", "older"), ids)
        }

    @Test
    fun `observeActiveForActiveTrackers emits again when the tracker is soft-deleted`() =
        runTest {
            val tracker = tracker("t")
            trackerDao.insert(tracker)
            dao.insert(event("e", "t"))

            dao.observeActiveForActiveTrackers().test {
                assertEquals(listOf("e"), awaitItem().map { it.id })

                trackerDao.update(tracker.copy(deletedAt = T0 + 1))

                assertEquals(emptyList<Any>(), awaitItem())
            }
        }

    @Test
    fun `getActiveById returns null for a soft-deleted event`() =
        runTest {
            trackerDao.insert(tracker("t"))
            dao.insert(event("e", "t", deletedAt = T0 + 1))

            assertNull(dao.getActiveById("e"))
        }

    @Test
    fun `update stores the changed fields`() =
        runTest {
            trackerDao.insert(tracker("t"))
            val event = event("e", "t")
            dao.insert(event)
            val moved = event.copy(occurredAt = T0 - 1, updatedAt = T0 + 1)

            dao.update(moved)

            assertEquals(moved, dao.getActiveById("e"))
        }

    @Test
    fun `insert fails for an unknown tracker id`() =
        runTest {
            val failure = runCatching { dao.insert(event("e", "missing")) }.exceptionOrNull()

            assertTrue("Expected a constraint failure, got $failure", failure is SQLiteConstraintException)
        }
}
