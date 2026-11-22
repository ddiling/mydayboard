package io.github.ddiling.mydayboard.core.testing

import io.github.ddiling.mydayboard.core.domain.task.TaskCategory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** Contract for the categories themselves. */
abstract class TaskCategoryContractTest : TaskRepositoryContractBase() {
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
    fun `renaming an unknown id changes nothing`() =
        runTest {
            categories.renameCategory("unknown", "Garden")

            assertEquals(emptyList<TaskCategory>(), categories.observeCategories().first())
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
}
