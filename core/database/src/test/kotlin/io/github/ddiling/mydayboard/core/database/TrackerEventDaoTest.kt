package io.github.ddiling.mydayboard.core.database

import android.database.sqlite.SQLiteConstraintException
import app.cash.turbine.test
import io.github.ddiling.mydayboard.core.database.model.TrackerEventEntity
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

                assertEquals(emptyList<TrackerEventEntity>(), awaitItem())
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

    @Test
    fun `insertForActiveTracker stores the event under an active tracker`() =
        runTest {
            trackerDao.insert(tracker("t"))

            dao.insertForActiveTracker("e", "t", occurredAt = T0 + 5, now = T0 + 6)

            assertEquals(
                TrackerEventEntity("e", "t", occurredAt = T0 + 5, createdAt = T0 + 6, updatedAt = T0 + 6),
                dao.getActiveById("e"),
            )
        }

    @Test
    fun `insertForActiveTracker stores nothing for a soft-deleted tracker`() =
        runTest {
            trackerDao.insert(tracker("t", deletedAt = T0 + 1))

            val rowId = dao.insertForActiveTracker("e", "t", occurredAt = T0, now = T0)

            assertEquals(-1L, rowId)
            assertNull(dao.getActiveById("e"))
        }

    @Test
    fun `insertForActiveTracker stores nothing for an unknown tracker without breaking the foreign key`() =
        runTest {
            val rowId = dao.insertForActiveTracker("e", "missing", occurredAt = T0, now = T0)

            assertEquals(-1L, rowId)
        }

    @Test
    fun `updateOccurredAtIfActive changes the time and updatedAt`() =
        runTest {
            trackerDao.insert(tracker("t"))
            dao.insert(event("e", "t"))

            val changed = dao.updateOccurredAtIfActive("e", occurredAt = T0 + 5, now = T0 + 6)

            assertEquals(1, changed)
            assertEquals(event("e", "t").copy(occurredAt = T0 + 5, updatedAt = T0 + 6), dao.getActiveById("e"))
        }

    @Test
    fun `updateOccurredAtIfActive changes nothing under a soft-deleted tracker`() =
        runTest {
            trackerDao.insert(tracker("t", deletedAt = T0 + 1))
            dao.insert(event("e", "t"))

            assertEquals(0, dao.updateOccurredAtIfActive("e", occurredAt = T0 + 5, now = T0 + 6))
        }

    @Test
    fun `softDeleteIfActive hides the event`() =
        runTest {
            trackerDao.insert(tracker("t"))
            dao.insert(event("e", "t"))

            val changed = dao.softDeleteIfActive("e", now = T0 + 1)

            assertEquals(1, changed)
            assertNull(dao.getActiveById("e"))
        }

    @Test
    fun `softDeleteIfActive changes nothing under a soft-deleted tracker`() =
        runTest {
            trackerDao.insert(tracker("t", deletedAt = T0 + 1))
            dao.insert(event("e", "t"))

            assertEquals(0, dao.softDeleteIfActive("e", now = T0 + 2))
        }

    @Test
    fun `softDeleteIfActive keeps the first delete's timestamps on an already deleted event`() =
        runTest {
            val firstDelete = T0 + 1
            trackerDao.insert(tracker("t"))
            dao.insert(event("e", "t", deletedAt = firstDelete))

            dao.softDeleteIfActive("e", now = T0 + 2)

            assertEquals(T0 to firstDelete, database.updatedAndDeletedAt("tracker_events", "e"))
        }

    @Test
    fun `updateOccurredAtIfActive leaves an already deleted event unchanged`() =
        runTest {
            val deletedAt = T0 + 1
            trackerDao.insert(tracker("t"))
            dao.insert(event("e", "t", deletedAt = deletedAt))

            val changed = dao.updateOccurredAtIfActive("e", occurredAt = T0 + 5, now = T0 + 6)

            assertEquals(0, changed)
            assertEquals(T0 to deletedAt, database.updatedAndDeletedAt("tracker_events", "e"))
        }

    @Test
    fun `restoreIfDeleted shows a deleted event again and bumps updatedAt`() =
        runTest {
            trackerDao.insert(tracker("t"))
            dao.insert(event("e", "t", deletedAt = T0 + 1))

            val changed = dao.restoreIfDeleted("e", now = T0 + 2)

            assertEquals(1, changed)
            assertEquals(T0 + 2 to null, database.updatedAndDeletedAt("tracker_events", "e"))
        }

    @Test
    fun `restoreIfDeleted changes nothing for an event that is not deleted`() =
        runTest {
            trackerDao.insert(tracker("t"))
            dao.insert(event("e", "t"))

            assertEquals(0, dao.restoreIfDeleted("e", now = T0 + 2))
        }

    @Test
    fun `restoreIfDeleted changes nothing under a soft-deleted tracker`() =
        runTest {
            trackerDao.insert(tracker("t", deletedAt = T0 + 1))
            dao.insert(event("e", "t", deletedAt = T0 + 1))

            assertEquals(0, dao.restoreIfDeleted("e", now = T0 + 2))
        }
}
