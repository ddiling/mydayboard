package io.github.ddiling.mydayboard.core.database

import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TrackerDaoTest {
    private val database = inMemoryDatabase()
    private val dao = database.trackerDao()

    @After
    fun closeDatabase() = database.close()

    @Test
    fun `observeActive hides soft-deleted trackers`() =
        runTest {
            dao.insert(tracker("active"))
            dao.insert(tracker("deleted", deletedAt = T0 + 1))

            val ids = dao.observeActive().first().map { it.id }

            assertEquals(listOf("active"), ids)
        }

    @Test
    fun `observeActive lists trackers oldest first`() =
        runTest {
            dao.insert(tracker("newer", createdAt = T0 + 1))
            dao.insert(tracker("older", createdAt = T0))

            val ids = dao.observeActive().first().map { it.id }

            assertEquals(listOf("older", "newer"), ids)
        }

    @Test
    fun `observeActive emits again when a tracker is soft-deleted`() =
        runTest {
            val tracker = tracker("a")
            dao.insert(tracker)

            dao.observeActive().test {
                assertEquals(listOf(tracker), awaitItem())

                dao.update(tracker.copy(deletedAt = T0 + 1))

                assertEquals(emptyList<Any>(), awaitItem())
            }
        }

    @Test
    fun `observeActiveById emits null for a soft-deleted tracker`() =
        runTest {
            dao.insert(tracker("a", deletedAt = T0 + 1))

            assertNull(dao.observeActiveById("a").first())
        }

    @Test
    fun `getActiveById returns null for a soft-deleted tracker`() =
        runTest {
            dao.insert(tracker("a", deletedAt = T0 + 1))

            assertNull(dao.getActiveById("a"))
        }

    @Test
    fun `update stores the changed fields`() =
        runTest {
            val tracker = tracker("a")
            dao.insert(tracker)
            val renamed = tracker.copy(name = "Renamed", updatedAt = T0 + 1)

            dao.update(renamed)

            assertEquals(renamed, dao.getActiveById("a"))
        }
}
