package io.github.ddiling.mydayboard.core.testing

import io.github.ddiling.mydayboard.core.domain.task.TaskTick
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

/** Contract for the tick flows. */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class TaskTickObserveContractTest : TaskTickRepositoryContractBase() {
    @Test
    fun `ticks are ordered by day, slot and id`() =
        runTest {
            val task = givenTask()
            val laterDay = givenTick(task.id, otherDay, morning)
            val lateSlot = givenTick(task.id, day, evening)
            val noSlot = givenTick(task.id, day, null)
            val earlySlot = givenTick(task.id, day, morning)

            assertEquals(listOf(noSlot, earlySlot, lateSlot, laterDay), liveTicksOf(task.id))
        }

    @Test
    fun `ticks of a deleted task are empty`() =
        runTest {
            val task = givenTask()
            givenTick(task.id)
            tasks.deleteTask(task.id)

            assertEquals(emptyList<TaskTick>(), liveTicksOf(task.id))
        }

    @Test
    fun `all ticks list those of live tasks and none of deleted ones`() =
        runTest {
            val live = givenTask()
            val deleted = givenTask()
            val kept = givenTick(live.id)
            givenTick(deleted.id)
            tasks.deleteTask(deleted.id)

            assertEquals(listOf(kept), ticks.observeAllTicks().first())
        }

    @Test
    fun `all ticks do not emit again after an unrelated task write`() =
        runTest {
            val task = givenTask()
            givenTick(task.id)
            val emissions = MutableStateFlow(emptyList<List<TaskTick>>())
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                ticks.observeAllTicks().collect { list -> emissions.update { it + listOf(list) } }
            }
            emissions.first { it.isNotEmpty() }

            tasks.updateTask(task.id, draft.copy(name = "Water filter"))
            advanceUntilIdle()

            assertEquals(1, emissions.value.size)
        }
}
