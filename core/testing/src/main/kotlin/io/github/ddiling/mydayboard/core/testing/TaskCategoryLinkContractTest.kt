package io.github.ddiling.mydayboard.core.testing

import io.github.ddiling.mydayboard.core.domain.task.Task
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** Contract for how tasks read while their category is deleted or restored. */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class TaskCategoryLinkContractTest : TaskRepositoryContractBase() {
    @Test
    fun `a task of a deleted category shows no category`() =
        runTest {
            val category = givenCategory()
            val task = givenTask(draft.copy(categoryId = category.id))

            categories.deleteCategory(category.id)

            assertEquals(
                null,
                tasks
                    .observeTasks()
                    .first()
                    .single()
                    .categoryId,
            )
            assertEquals(null, tasks.observeTask(task.id).first()?.categoryId)
        }

    @Test
    fun `restoring the category gives the task its category back`() =
        runTest {
            val category = givenDeletedCategory()
            val task = givenTask(draft.copy(categoryId = category.id))

            categories.restoreCategory(category.id)

            assertEquals(category.id, tasks.observeTask(task.id).first()?.categoryId)
        }

    @Test
    fun `the task list does not emit again after a category write that changes no task`() =
        runTest {
            val task = givenTask()
            val other = givenCategory()
            val emissions = MutableStateFlow(emptyList<List<Task>>())
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                tasks.observeTasks().collect { list -> emissions.update { it + listOf(list) } }
            }
            emissions.first { it.isNotEmpty() }

            categories.renameCategory(other.id, "Garden")
            tasks.updateTask(task.id, draft.copy(name = "Water filter"))
            emissions.first { it.size >= 2 }

            // The second emission is the task edit; a repeat of the first list would come from the category write.
            assertEquals("Water filter", emissions.value[1].single().name)
        }
}
