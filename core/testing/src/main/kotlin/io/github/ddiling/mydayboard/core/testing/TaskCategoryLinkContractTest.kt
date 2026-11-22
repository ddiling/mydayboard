package io.github.ddiling.mydayboard.core.testing

import io.github.ddiling.mydayboard.core.domain.task.Task
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
            givenTask(draft.copy(categoryId = category.id))

            categories.deleteCategory(category.id)

            assertEquals(
                null,
                tasks
                    .observeTasks()
                    .first()
                    .single()
                    .categoryId,
            )
        }

    @Test
    fun `an observed task of a deleted category shows no category`() =
        runTest {
            val category = givenCategory()
            val task = givenTask(draft.copy(categoryId = category.id))

            categories.deleteCategory(category.id)

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
            val emissions = MutableStateFlow(emptyList<List<Task>>())
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                tasks.observeTasks().collect { list -> emissions.update { it + listOf(list) } }
            }
            val initial = emissions.first { it.isNotEmpty() }.single()

            givenCategory()
            tasks.updateTask(task.id, draft.copy(name = "Water filter"))
            emissions.first { it.size >= 2 && it.last().single().name == "Water filter" }
            advanceUntilIdle()

            // A repeat of the first list from the category write would make this three emissions.
            assertEquals(listOf(initial, emissions.value.last()), emissions.value)
            assertEquals("Water filter", emissions.value.last().single().name)
        }

    @Test
    fun `an observed task does not emit again after a category write that changes nothing for it`() =
        runTest {
            val task = givenTask()
            val emissions = MutableStateFlow(emptyList<Task?>())
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                tasks.observeTask(task.id).collect { value -> emissions.update { it + listOf(value) } }
            }
            val initial = emissions.first { it.isNotEmpty() }.single()

            givenCategory()
            tasks.updateTask(task.id, draft.copy(name = "Water filter"))
            emissions.first { it.size >= 2 && it.last()?.name == "Water filter" }
            advanceUntilIdle()

            assertEquals(listOf(initial, emissions.value.last()), emissions.value)
        }
}
