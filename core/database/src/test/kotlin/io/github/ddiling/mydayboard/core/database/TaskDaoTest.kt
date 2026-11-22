package io.github.ddiling.mydayboard.core.database

import io.github.ddiling.mydayboard.core.database.model.TaskEntity
import io.github.ddiling.mydayboard.core.database.model.TaskEntityIntervalMode
import io.github.ddiling.mydayboard.core.database.model.TaskEntityRepeatType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TaskDaoTest {
    private val database = inMemoryDatabase()
    private val categoryDao = database.taskCategoryDao()
    private val dao = database.taskDao()

    @After
    fun closeDatabase() = database.close()

    private suspend fun roundTrip(entity: TaskEntity) {
        dao.insert(entity)

        assertEquals(entity, dao.observeLiveById(entity.id).first())
    }

    @Test
    fun `insert stores every column of a several times task`() =
        runTest {
            categoryDao.insert(category("home"))
            roundTrip(
                task("pills", repeatType = TaskEntityRepeatType.TIMES_PER_DAY, categoryId = "home").copy(
                    times = "08:00,20:00",
                    timeOfDay = "08:00",
                    note = "With water",
                    goal = 2,
                    archivedAt = T0 + 3,
                ),
            )
        }

    @Test
    fun `insert stores the weekday bit mask of a chosen weekdays task`() =
        runTest {
            val mondayAndSunday = 0b1000001
            roundTrip(task("english", repeatType = TaskEntityRepeatType.WEEKDAYS).copy(weekdays = mondayAndSunday))
        }

    @Test
    fun `insert stores mode and anchor date of an every N days task`() =
        runTest {
            roundTrip(
                task("vacuum", repeatType = TaskEntityRepeatType.EVERY_N_DAYS).copy(
                    intervalDays = 3,
                    intervalMode = TaskEntityIntervalMode.FIXED_CALENDAR,
                    anchorDate = DAY0 + 2,
                ),
            )
        }

    @Test
    fun `observeLive hides soft-deleted tasks but keeps archived ones`() =
        runTest {
            dao.insert(task("live"))
            dao.insert(task("archived").copy(archivedAt = T0 + 1))
            dao.insert(task("gone", deletedAt = T0 + 1))

            assertEquals(listOf("archived", "live"), dao.observeLive().first().map { it.id })
        }

    @Test
    fun `observeLiveById returns null for a deleted task`() =
        runTest {
            dao.insert(task("gone", deletedAt = T0 + 1))

            assertNull(dao.observeLiveById("gone").first())
        }

    @Test
    fun `updateIfLive changes the fields and updated_at and returns 1`() =
        runTest {
            dao.insert(task("t", name = "Old"))

            val changed = dao.updateIfLive(task("t", name = "New").copy(note = "Changed"), T0 + 5)

            val stored = dao.observeLiveById("t").first()
            assertEquals(1, changed)
            assertEquals(listOf("New", "Changed", T0 + 5), listOf(stored?.name, stored?.note, stored?.updatedAt))
        }

    @Test
    fun `updateIfLive keeps the stored creation and archive times`() =
        runTest {
            dao.insert(task("t").copy(createdAt = T0, archivedAt = T0 + 1))

            dao.updateIfLive(task("t").copy(createdAt = T0 + 8, archivedAt = null), T0 + 5)

            val stored = dao.observeLiveById("t").first()
            assertEquals(T0 to (T0 + 1), stored?.createdAt to stored?.archivedAt)
        }

    @Test
    fun `updateIfLive returns 0 for a deleted task and changes nothing`() =
        runTest {
            dao.insert(task("t", name = "Old", deletedAt = T0 + 1))

            val changed = dao.updateIfLive(task("t", name = "New"), T0 + 5)

            assertEquals(0, changed)
            assertEquals(T0 to (T0 + 1), database.updatedAndDeletedAt("tasks", "t"))
        }

    @Test
    fun `setArchivedAt stores and clears the value`() =
        runTest {
            dao.insert(task("t"))

            dao.setArchivedAt("t", T0 + 5, T0 + 6)
            val archived = dao.observeLiveById("t").first()?.archivedAt
            dao.setArchivedAt("t", null, T0 + 7)
            val cleared = dao.observeLiveById("t").first()?.archivedAt

            assertEquals(T0 + 5, archived)
            assertNull(cleared)
        }

    @Test
    fun `setArchivedAt returns 0 for a deleted task`() =
        runTest {
            dao.insert(task("t", deletedAt = T0 + 1))

            assertEquals(0, dao.setArchivedAt("t", T0 + 5, T0 + 6))
        }

    @Test
    fun `softDelete returns 1 and then 0 the second time`() =
        runTest {
            dao.insert(task("t"))

            val results = listOf(dao.softDeleteIfLive("t", T0 + 5), dao.softDeleteIfLive("t", T0 + 6))

            assertEquals(listOf(1, 0), results)
        }

    @Test
    fun `a task keeps its category_id when the category is soft-deleted`() =
        runTest {
            categoryDao.insert(category("home"))
            dao.insert(task("t", categoryId = "home"))

            categoryDao.softDeleteIfLive("home", T0 + 5)

            assertEquals("home", dao.observeLiveById("t").first()?.categoryId)
            assertEquals(T0 to null, database.updatedAndDeletedAt("tasks", "t"))
        }
}
