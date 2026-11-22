package io.github.ddiling.mydayboard.core.testing

import io.github.ddiling.mydayboard.core.domain.task.TaskTick
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalTime
import java.util.concurrent.Executors

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
    fun `adding the same timed slot from parallel coroutines leaves one live tick`() =
        runTest {
            val task = givenTask()

            Executors.newFixedThreadPool(PARALLEL_ADDS).asCoroutineDispatcher().use { threads ->
                withContext(threads) {
                    (1..PARALLEL_ADDS).map { async { ticks.addTick(task.id, day, morning) } }.awaitAll()
                }
            }

            assertEquals(1, liveTicksOf(task.id).size)
        }

    @Test
    fun `adding a slot with seconds keeps whole minutes and matches the tick of that minute`() =
        runTest {
            val task = givenTask()
            val first = givenTick(task.id, slot = morning)

            val second = ticks.addTick(task.id, day, LocalTime.of(8, 0, 30))

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

    @Test
    fun `restoring a timed tick is skipped when a live tick now holds the same slot`() =
        runTest {
            val task = givenTask()
            val deleted = givenTick(task.id, slot = morning)
            ticks.deleteTick(deleted.id)
            val replacement = givenTick(task.id, slot = morning)

            ticks.restoreTick(deleted.id)

            assertEquals(listOf(replacement), liveTicksOf(task.id))
        }

    private companion object {
        const val PARALLEL_ADDS = 8
    }
}
