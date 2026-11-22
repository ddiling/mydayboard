package io.github.ddiling.mydayboard.core.database

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
class TaskTickDaoTest {
    private val database = inMemoryDatabase()
    private val taskDao = database.taskDao()
    private val dao = database.taskTickDao()

    /** Every test starts with one live task, `t`; tests about a deleted task soft-delete it. */
    @Before
    fun givenTask() = runTest { taskDao.insert(task("t")) }

    @After
    fun closeDatabase() = database.close()

    @Test
    fun `observeForTask hides deleted ticks`() =
        runTest {
            dao.insertForLiveTask("live", "t", DAY0, null, T0)
            dao.insertForLiveTask("gone", "t", DAY0, null, T0)
            dao.softDeleteIfLive("gone", T0 + 1)

            assertEquals(listOf("live"), dao.observeForTask("t").first().map { it.id })
        }

    @Test
    fun `observeForTask is empty for a deleted task`() =
        runTest {
            dao.insertForLiveTask("k", "t", DAY0, null, T0)
            taskDao.softDeleteIfLive("t", T0 + 1)

            assertTrue(dao.observeForTask("t").first().isEmpty())
        }

    @Test
    fun `observeForTask orders by day then slot then id, counted ticks first`() =
        runTest {
            dao.insertForLiveTask("c", "t", DAY0 + 1, 60, T0)
            dao.insertForLiveTask("b", "t", DAY0, 120, T0)
            dao.insertForLiveTask("z", "t", DAY0, null, T0)
            dao.insertForLiveTask("a", "t", DAY0, null, T0)

            assertEquals(listOf("a", "z", "b", "c"), dao.observeForTask("t").first().map { it.id })
        }

    @Test
    fun `observeAllLive lists ticks of live tasks only`() =
        runTest {
            taskDao.insert(task("other"))
            dao.insertForLiveTask("own", "t", DAY0, null, T0)
            dao.insertForLiveTask("foreign", "other", DAY0, null, T0)
            taskDao.softDeleteIfLive("other", T0 + 1)

            assertEquals(listOf("own"), dao.observeAllLive().first().map { it.id })
        }

    @Test
    fun `findLiveForDay returns only live ticks of that day`() =
        runTest {
            dao.insertForLiveTask("today", "t", DAY0, null, T0)
            dao.insertForLiveTask("gone", "t", DAY0, null, T0)
            dao.insertForLiveTask("tomorrow", "t", DAY0 + 1, null, T0)
            dao.softDeleteIfLive("gone", T0 + 1)

            assertEquals(listOf("today"), dao.findLiveForDay("t", DAY0).map { it.id })
        }

    @Test
    fun `findLiveForDay is empty for a deleted task`() =
        runTest {
            dao.insertForLiveTask("k", "t", DAY0, null, T0)
            taskDao.softDeleteIfLive("t", T0 + 1)

            assertTrue(dao.findLiveForDay("t", DAY0).isEmpty())
        }

    @Test
    fun `insertForLiveTask stores day and slot and returns a row id`() =
        runTest {
            val rowId = dao.insertForLiveTask("k", "t", DAY0 + 2, 480, T0)

            val stored = dao.observeForTask("t").first().single()
            assertTrue(rowId > 0)
            assertEquals(listOf(DAY0 + 2, 480), listOf(stored.day, stored.slotMinute?.toLong()))
        }

    @Test
    fun `insertForLiveTask returns -1 for a deleted task`() =
        runTest {
            taskDao.softDeleteIfLive("t", T0 + 1)

            assertEquals(-1L, dao.insertForLiveTask("k", "t", DAY0, null, T0 + 2))
        }

    @Test
    fun `insertForLiveTask returns -1 for an unknown task`() =
        runTest {
            assertEquals(-1L, dao.insertForLiveTask("k", "missing", DAY0, null, T0))
        }

    @Test
    fun `softDelete returns 1 once and 0 when repeated`() =
        runTest {
            dao.insertForLiveTask("k", "t", DAY0, null, T0)

            val results = listOf(dao.softDeleteIfLive("k", T0 + 1), dao.softDeleteIfLive("k", T0 + 2))

            assertEquals(listOf(1, 0), results)
        }

    @Test
    fun `restore returns 1 once and 0 when repeated`() =
        runTest {
            dao.insertForLiveTask("k", "t", DAY0, null, T0)
            dao.softDeleteIfLive("k", T0 + 1)

            val results = listOf(dao.restoreIfDeleted("k", T0 + 2), dao.restoreIfDeleted("k", T0 + 3))

            assertEquals(listOf(1, 0), results)
        }

    @Test
    fun `restore returns 0 for a tick of a deleted task`() =
        runTest {
            dao.insertForLiveTask("k", "t", DAY0, null, T0)
            dao.softDeleteIfLive("k", T0 + 1)
            taskDao.softDeleteIfLive("t", T0 + 2)

            assertEquals(0, dao.restoreIfDeleted("k", T0 + 3))
        }
}
