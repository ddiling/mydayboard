package io.github.ddiling.mydayboard.core.database

import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TaskCategoryDaoTest {
    private val database = inMemoryDatabase()
    private val dao = database.taskCategoryDao()

    @After
    fun closeDatabase() = database.close()

    @Test
    fun `observeLive hides soft-deleted categories`() =
        runTest {
            dao.insert(category("live"))
            dao.insert(category("gone", deletedAt = T0 + 1))

            assertEquals(listOf("live"), dao.observeLive().first().map { it.id })
        }

    @Test
    fun `observeLive lists oldest first with ties broken by id`() =
        runTest {
            dao.insert(category("b", createdAt = T0))
            dao.insert(category("a", createdAt = T0))
            dao.insert(category("old", createdAt = T0 - 1))

            assertEquals(listOf("old", "a", "b"), dao.observeLive().first().map { it.id })
        }

    @Test
    fun `insert makes observeLive emit again`() =
        runTest {
            dao.observeLive().test {
                assertEquals(emptyList<String>(), awaitItem().map { it.id })

                dao.insert(category("new"))

                assertEquals(listOf("new"), awaitItem().map { it.id })
            }
        }

    @Test
    fun `rename changes the name and updated_at and returns 1`() =
        runTest {
            dao.insert(category("c", name = "Home"))

            val changed = dao.renameIfLive("c", "Garden", T0 + 5)

            assertEquals(1, changed)
            assertEquals(
                "Garden",
                dao
                    .observeLive()
                    .first()
                    .single()
                    .name,
            )
            assertEquals(T0 + 5, database.updatedAndDeletedAt("task_categories", "c").first)
        }

    @Test
    fun `rename returns 0 for a deleted category`() =
        runTest {
            dao.insert(category("c", name = "Home", deletedAt = T0 + 1))

            assertEquals(0, dao.renameIfLive("c", "Garden", T0 + 5))
        }

    @Test
    fun `softDelete sets deleted_at and updated_at and returns 1`() =
        runTest {
            dao.insert(category("c"))

            val changed = dao.softDeleteIfLive("c", T0 + 5)

            assertEquals(1, changed)
            assertEquals(T0 + 5 to (T0 + 5), database.updatedAndDeletedAt("task_categories", "c"))
        }

    @Test
    fun `softDelete returns 0 for an already deleted category`() =
        runTest {
            dao.insert(category("c", deletedAt = T0 + 1))

            assertEquals(0, dao.softDeleteIfLive("c", T0 + 5))
        }

    @Test
    fun `restore clears deleted_at and returns 1`() =
        runTest {
            dao.insert(category("c", deletedAt = T0 + 1))

            val changed = dao.restoreIfDeleted("c", T0 + 5)

            assertEquals(1, changed)
            assertEquals(T0 + 5 to null, database.updatedAndDeletedAt("task_categories", "c"))
        }

    @Test
    fun `restore returns 0 when the category is not deleted`() =
        runTest {
            dao.insert(category("c"))

            assertEquals(0, dao.restoreIfDeleted("c", T0 + 5))
        }
}
