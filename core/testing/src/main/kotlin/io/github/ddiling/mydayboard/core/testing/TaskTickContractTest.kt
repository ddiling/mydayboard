package io.github.ddiling.mydayboard.core.testing

import io.github.ddiling.mydayboard.core.domain.task.TaskTick
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Contract for adding, deleting and restoring single ticks. */
abstract class TaskTickContractTest : TaskTickRepositoryContractBase() {
    @Test
    fun `adding returns the tick with the injected id, day, slot and clock times`() =
        runTest {
            val task = givenTask()

            val tick = ticks.addTick(task.id, day, morning)

            assertEquals(TaskTick("id-2", task.id, day, morning, now, now), tick)
        }

    @Test
    fun `adding to an unknown task returns null`() =
        runTest {
            assertNull(ticks.addTick("missing", day, null))
        }

    @Test
    fun `adding to a deleted task returns null`() =
        runTest {
            val task = givenDeletedTask()

            assertNull(ticks.addTick(task.id, day, null))
        }

    @Test
    fun `adding a timed slot that already has a live tick returns that tick and adds none`() =
        runTest {
            val task = givenTask()
            val first = givenTick(task.id, slot = morning)

            val second = ticks.addTick(task.id, day, morning)

            assertEquals(first, second)
            assertEquals(listOf(first), liveTicksOf(task.id))
        }

    @Test
    fun `deleting hides the tick`() =
        runTest {
            val task = givenTask()
            val tick = givenTick(task.id)

            ticks.deleteTick(tick.id)

            assertEquals(emptyList<TaskTick>(), liveTicksOf(task.id))
        }

    @Test
    fun `restoring brings the tick back and bumps updatedAt`() =
        runTest {
            val task = givenTask()
            val tick = givenTick(task.id)
            ticks.deleteTick(tick.id)
            clock.advanceBy(clockAdvance)

            ticks.restoreTick(tick.id)

            assertEquals(listOf(tick.copy(updatedAt = nowAfterAdvance)), liveTicksOf(task.id))
        }

    @Test
    fun `restoring does nothing when the task is deleted`() =
        runTest {
            val task = givenTask()
            val tick = givenTick(task.id)
            ticks.deleteTick(tick.id)
            tasks.deleteTask(task.id)

            ticks.restoreTick(tick.id)

            assertEquals(emptyList<TaskTick>(), liveTicksOf(task.id))
        }
}
