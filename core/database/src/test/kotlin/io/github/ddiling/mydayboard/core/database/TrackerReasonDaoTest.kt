package io.github.ddiling.mydayboard.core.database

import app.cash.turbine.test
import io.github.ddiling.mydayboard.core.database.model.TrackerReasonEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TrackerReasonDaoTest {
    private val database = inMemoryDatabase()
    private val trackerDao = database.trackerDao()
    private val dao = database.trackerReasonDao()

    /** Every test starts with one live tracker, `t`; tests about a deleted tracker soft-delete it. */
    @Before
    fun givenTracker() = runTest { trackerDao.insert(tracker("t")) }

    @After
    fun closeDatabase() = database.close()

    @Test
    fun `observeActiveForTracker hides soft-deleted reasons`() =
        runTest {
            dao.insert(reason("active", "t"))
            dao.insert(reason("deleted", "t", deletedAt = T0 + 1))

            val ids = dao.observeActiveForTracker("t").first().map { it.id }

            assertEquals(listOf("active"), ids)
        }

    @Test
    fun `observeActiveForTracker lists only that tracker's reasons`() =
        runTest {
            trackerDao.insert(tracker("other"))
            dao.insert(reason("own", "t"))
            dao.insert(reason("foreign", "other"))

            val ids = dao.observeActiveForTracker("t").first().map { it.id }

            assertEquals(listOf("own"), ids)
        }

    @Test
    fun `observeActiveForTracker lists the oldest reason first`() =
        runTest {
            dao.insert(reason("a-newer", "t", createdAt = T0 + 1))
            dao.insert(reason("b-older", "t", createdAt = T0))

            val ids = dao.observeActiveForTracker("t").first().map { it.id }

            assertEquals(listOf("b-older", "a-newer"), ids)
        }

    @Test
    fun `observeActiveForTracker breaks a createdAt tie by id`() =
        runTest {
            dao.insert(reason("b", "t"))
            dao.insert(reason("a", "t"))

            val ids = dao.observeActiveForTracker("t").first().map { it.id }

            assertEquals(listOf("a", "b"), ids)
        }

    @Test
    fun `observeActiveForTracker is empty for a soft-deleted tracker`() =
        runTest {
            dao.insert(reason("r", "t"))
            trackerDao.softDelete("t", T0 + 1)

            val reasons = dao.observeActiveForTracker("t").first()

            assertEquals(emptyList<TrackerReasonEntity>(), reasons)
        }

    @Test
    fun `observeActiveForTracker emits again after an insert`() =
        runTest {
            dao.observeActiveForTracker("t").test {
                assertEquals(emptyList<TrackerReasonEntity>(), awaitItem())

                dao.insertForActiveTracker("r", "t", "Stay hydrated", T0 + 1)

                assertEquals(listOf("r"), awaitItem().map { it.id })
            }
        }

    @Test
    fun `insertForActiveTracker stores the text with both timestamps set to now`() =
        runTest {
            val now = T0 + 1

            val rowId = dao.insertForActiveTracker("r", "t", "Stay hydrated", now)

            assertTrue(rowId > 0)
            val stored = dao.observeActiveForTracker("t").first()
            assertEquals(listOf(reason("r", "t", text = "Stay hydrated", createdAt = now)), stored)
        }

    @Test
    fun `insertForActiveTracker inserts nothing for a soft-deleted tracker`() =
        runTest {
            trackerDao.softDelete("t", T0 + 1)

            assertEquals(-1L, dao.insertForActiveTracker("r", "t", "Stay hydrated", T0 + 2))
        }

    @Test
    fun `insertForActiveTracker inserts nothing for an unknown tracker`() =
        runTest {
            assertEquals(-1L, dao.insertForActiveTracker("r", "missing", "Stay hydrated", T0 + 1))
        }

    @Test
    fun `updateTextIfActive changes the text and updatedAt`() =
        runTest {
            val now = T0 + 1
            dao.insert(reason("r", "t"))

            val changed = dao.updateTextIfActive("r", "Sleep better", now)

            assertEquals(1, changed)
            val stored = dao.observeActiveForTracker("t").first()
            assertEquals(listOf(reason("r", "t", text = "Sleep better").copy(updatedAt = now)), stored)
        }

    @Test
    fun `updateTextIfActive changes nothing for a deleted reason`() =
        runTest {
            dao.insert(reason("r", "t", deletedAt = T0 + 1))

            assertEquals(0, dao.updateTextIfActive("r", "Sleep better", T0 + 2))
        }

    @Test
    fun `updateTextIfActive changes nothing for a reason of a deleted tracker`() =
        runTest {
            dao.insert(reason("r", "t"))
            trackerDao.softDelete("t", T0 + 1)

            assertEquals(0, dao.updateTextIfActive("r", "Sleep better", T0 + 2))
        }

    @Test
    fun `softDeleteIfActive sets deletedAt and updatedAt`() =
        runTest {
            val now = T0 + 1
            dao.insert(reason("r", "t"))

            val changed = dao.softDeleteIfActive("r", now)

            assertEquals(1, changed)
            assertEquals(now to now, database.updatedAndDeletedAt("tracker_reasons", "r"))
        }

    @Test
    fun `softDeleteIfActive changes nothing for an already deleted reason`() =
        runTest {
            dao.insert(reason("r", "t", deletedAt = T0 + 1))

            assertEquals(0, dao.softDeleteIfActive("r", T0 + 2))
        }

    @Test
    fun `softDeleteIfActive changes nothing for a reason of a deleted tracker`() =
        runTest {
            dao.insert(reason("r", "t"))
            trackerDao.softDelete("t", T0 + 1)

            assertEquals(0, dao.softDeleteIfActive("r", T0 + 2))
        }

    @Test
    fun `restoreIfDeleted clears deletedAt and bumps updatedAt`() =
        runTest {
            val now = T0 + 2
            dao.insert(reason("r", "t", deletedAt = T0 + 1))

            val changed = dao.restoreIfDeleted("r", now)

            assertEquals(1, changed)
            assertEquals(now to null, database.updatedAndDeletedAt("tracker_reasons", "r"))
        }

    @Test
    fun `restoreIfDeleted changes nothing for a reason that is not deleted`() =
        runTest {
            dao.insert(reason("r", "t"))

            assertEquals(0, dao.restoreIfDeleted("r", T0 + 1))
        }

    @Test
    fun `restoreIfDeleted changes nothing for a reason of a deleted tracker`() =
        runTest {
            dao.insert(reason("r", "t", deletedAt = T0 + 1))
            trackerDao.softDelete("t", T0 + 2)

            assertEquals(0, dao.restoreIfDeleted("r", T0 + 3))
        }
}
