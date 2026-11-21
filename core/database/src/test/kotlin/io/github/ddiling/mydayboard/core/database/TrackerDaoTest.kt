package io.github.ddiling.mydayboard.core.database

import app.cash.turbine.test
import io.github.ddiling.mydayboard.core.database.model.TrackerEntity
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

                assertEquals(emptyList<TrackerEntity>(), awaitItem())
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

    @Test
    fun `rename changes the name and updatedAt of an active tracker`() =
        runTest {
            dao.insert(tracker("a"))

            val changed = dao.rename("a", "Renamed", T0 + 1)

            assertEquals(1, changed)
            assertEquals(tracker("a").copy(name = "Renamed", updatedAt = T0 + 1), dao.getActiveById("a"))
        }

    @Test
    fun `rename does not bring back a soft-deleted tracker`() =
        runTest {
            dao.insert(tracker("a", deletedAt = T0 + 1))

            val changed = dao.rename("a", "Renamed", T0 + 2)

            assertEquals(0, changed)
            assertNull(dao.getActiveById("a"))
        }

    @Test
    fun `softDelete hides the tracker`() =
        runTest {
            dao.insert(tracker("a"))

            val changed = dao.softDelete("a", T0 + 1)

            assertEquals(1, changed)
            assertNull(dao.getActiveById("a"))
        }

    @Test
    fun `softDelete changes nothing for an unknown id`() =
        runTest {
            assertEquals(0, dao.softDelete("missing", T0 + 1))
        }

    @Test
    fun `softDelete keeps the first delete's timestamps on an already deleted tracker`() =
        runTest {
            val firstDelete = T0 + 1
            dao.insert(tracker("a"))
            dao.softDelete("a", firstDelete)

            dao.softDelete("a", T0 + 2)

            assertEquals(firstDelete to firstDelete, database.updatedAndDeletedAt("trackers", "a"))
        }

    @Test
    fun `setAskForReason stores the flag and bumps updatedAt`() =
        runTest {
            val now = T0 + 1
            dao.insert(tracker("a"))

            val changed = dao.setAskForReason("a", enabled = true, now = now)

            assertEquals(1, changed)
            assertEquals(tracker("a").copy(askForReason = true, updatedAt = now), dao.getActiveById("a"))
        }

    @Test
    fun `setAskForReason turns the flag off again`() =
        runTest {
            dao.insert(tracker("a").copy(askForReason = true))

            dao.setAskForReason("a", enabled = false, now = T0 + 1)

            assertEquals(false, dao.getActiveById("a")?.askForReason)
        }

    @Test
    fun `setAskForReason changes nothing for a soft-deleted tracker`() =
        runTest {
            val deletedAt = T0 + 1
            dao.insert(tracker("a", deletedAt = deletedAt))

            val changed = dao.setAskForReason("a", enabled = true, now = T0 + 2)

            assertEquals(0, changed)
            assertEquals(T0 to deletedAt, database.updatedAndDeletedAt("trackers", "a"))
        }

    @Test
    fun `setAskForReason changes nothing for an unknown id`() =
        runTest {
            assertEquals(0, dao.setAskForReason("missing", enabled = true, now = T0 + 1))
        }
}
