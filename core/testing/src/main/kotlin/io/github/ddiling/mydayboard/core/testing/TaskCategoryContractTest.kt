package io.github.ddiling.mydayboard.core.testing

import io.github.ddiling.mydayboard.core.domain.task.TaskCategory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration

/** Contract for the categories themselves. */
abstract class TaskCategoryContractTest : TaskRepositoryContractBase() {
    /** More than nine, so `id-10` sorts before `id-2` and a missing tie-break shows. */
    private val sameInstantCount = 11
    private val subMillisecondNanos = 789_123L

    @Test
    fun `created category has the injected id, name and clock times`() =
        runTest {
            val category = categories.createCategory("Home")

            assertEquals(TaskCategory("id-1", "Home", now, now), category)
        }

    @Test
    fun `categories are listed oldest first and deleted ones are hidden`() =
        runTest {
            val first = givenCategory("Home")
            clock.advanceBy(clockAdvance)
            val second = givenCategory("Work")
            val deleted = givenCategory("Garden")
            categories.deleteCategory(deleted.id)

            assertEquals(listOf(first, second), categories.observeCategories().first())
        }

    @Test
    fun `renaming changes the name and updatedAt`() =
        runTest {
            val category = givenCategory()
            clock.advanceBy(clockAdvance)

            categories.renameCategory(category.id, "Garden")

            assertEquals(
                listOf(category.copy(name = "Garden", updatedAt = nowAfterAdvance)),
                categories.observeCategories().first(),
            )
        }

    @Test
    fun `a restored category returns at its old place`() =
        runTest {
            val first = givenCategory("Home")
            clock.advanceBy(clockAdvance)
            val second = givenCategory("Work")
            categories.deleteCategory(first.id)

            categories.restoreCategory(first.id)

            assertEquals(
                listOf(first.copy(updatedAt = nowAfterAdvance), second),
                categories.observeCategories().first(),
            )
        }

    @Test
    fun `restoring a category that is not deleted changes nothing`() =
        runTest {
            val category = givenCategory()
            clock.advanceBy(clockAdvance)

            categories.restoreCategory(category.id)

            assertEquals(listOf(category), categories.observeCategories().first())
        }

    @Test
    fun `categories created at the same instant are listed by id`() =
        runTest {
            val created = List(sameInstantCount) { givenCategory("Category $it") }

            assertEquals(created.sortedBy { it.id }, categories.observeCategories().first())
        }

    @Test
    fun `a created category equals what is read back when the clock has sub-millisecond precision`() =
        runTest {
            clock.advanceBy(Duration.ofNanos(subMillisecondNanos))

            val category = categories.createCategory("Home")

            assertEquals(listOf(category), categories.observeCategories().first())
        }
}

/** Contract for ids that are unknown or deleted: such calls change nothing. */
abstract class TaskCategoryNoOpContractTest : TaskRepositoryContractBase() {
    @Test
    fun `renaming an unknown id changes nothing`() =
        runTest {
            categories.renameCategory("unknown", "Garden")

            assertEquals(emptyList<TaskCategory>(), categories.observeCategories().first())
        }

    @Test
    fun `renaming a deleted category changes nothing`() =
        runTest {
            val category = givenDeletedCategory()
            clock.advanceBy(clockAdvance)
            categories.renameCategory(category.id, "Garden")
            clock.advanceBy(clockAdvance)

            categories.restoreCategory(category.id)

            assertEquals(
                listOf(category.copy(updatedAt = nowAfterAdvance.plus(clockAdvance))),
                categories.observeCategories().first(),
            )
        }

    @Test
    fun `deleting an unknown id keeps the other categories`() =
        runTest {
            val category = givenCategory()

            categories.deleteCategory("unknown")

            assertEquals(listOf(category), categories.observeCategories().first())
        }

    @Test
    fun `deleting a deleted category again keeps it deleted`() =
        runTest {
            val category = givenDeletedCategory()

            categories.deleteCategory(category.id)

            assertEquals(emptyList<TaskCategory>(), categories.observeCategories().first())
        }

    @Test
    fun `restoring an unknown id changes nothing`() =
        runTest {
            categories.restoreCategory("unknown")

            assertEquals(emptyList<TaskCategory>(), categories.observeCategories().first())
        }
}
