package io.github.ddiling.mydayboard.core.testing

import io.github.ddiling.mydayboard.core.domain.task.OnMiss
import io.github.ddiling.mydayboard.core.domain.task.Repeat
import io.github.ddiling.mydayboard.core.domain.task.Task
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.LocalTime

/** Contract for creating and observing tasks. */
abstract class TaskContractTest : TaskRepositoryContractBase() {
    /** More than 9, so that ids compared as text (`id-10` before `id-2`) differ from creation order. */
    private val tiedCount = 10

    private val subMillisecondNanos = 789_123L

    @Test
    fun `created task has the injected id, clock times and the given start date`() =
        runTest {
            val task = tasks.createTask(draft, notToday)

            assertEquals(
                Task("id-1", "Vacuum", null, Repeat.EveryDay, null, null, null, OnMiss.STAYS, notToday, null, now, now),
                task,
            )
        }

    @Test
    fun `created task is the one observed`() =
        runTest {
            val task = givenTask()

            assertEquals(task, tasks.observeTask(task.id).first())
        }

    @Test
    fun `a created task equals what is read back when the clock has sub-millisecond precision`() =
        runTest {
            clock.advanceBy(Duration.ofNanos(subMillisecondNanos))

            val task = tasks.createTask(draft, notToday)

            assertEquals(task, tasks.observeTask(task.id).first())
        }

    @Test
    fun `an archived task has the same archivedAt and updatedAt even when the clock moves on every reading`() =
        runTest {
            val task = givenTask()
            clock.tickPerReading = clockAdvance

            tasks.archiveTask(task.id)

            val archived = tasks.observeTask(task.id).first()
            assertEquals(archived?.updatedAt, archived?.archivedAt)
        }

    @Test
    fun `tasks are listed oldest first`() =
        runTest {
            val first = givenTask()
            clock.advanceBy(clockAdvance)
            val second = givenTask()

            assertEquals(listOf(first, second), tasks.observeTasks().first())
        }

    @Test
    fun `tasks with the same createdAt are listed by id`() =
        runTest {
            repeat(tiedCount) { givenTask() }

            val listedIds = tasks.observeTasks().first().map { it.id }

            assertEquals(listedIds.sorted(), listedIds)
        }

    @Test
    fun `observing an unknown id gives null`() =
        runTest {
            assertNull(tasks.observeTask("unknown").first())
        }

    @Test
    fun `observing a deleted id gives null`() =
        runTest {
            assertNull(tasks.observeTask(givenDeletedTask().id).first())
        }

    @Test
    fun `deleted task is hidden from the list`() =
        runTest {
            givenDeletedTask()

            assertEquals(emptyList<Task>(), tasks.observeTasks().first())
        }

    @Test
    fun `deleting an unknown id does not throw`() =
        runTest {
            tasks.deleteTask("unknown")

            assertEquals(emptyList<Task>(), tasks.observeTasks().first())
        }

    @Test
    fun `several times a day are stored sorted and without repeats`() =
        runTest {
            val evening = LocalTime.of(20, 0)
            val morning = LocalTime.of(8, 0)
            val task = givenTask(draft.copy(repeat = Repeat.TimesPerDay(listOf(evening, morning, morning))))

            assertEquals(Repeat.TimesPerDay(listOf(morning, evening)), task.repeat)
            assertEquals(task.repeat, tasks.observeTask(task.id).first()?.repeat)
        }
}
