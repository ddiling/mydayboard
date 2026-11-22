package io.github.ddiling.mydayboard.core.testing

import io.github.ddiling.mydayboard.core.domain.task.OnMiss
import io.github.ddiling.mydayboard.core.domain.task.Repeat
import io.github.ddiling.mydayboard.core.domain.task.Task
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/** Contract for updating, archiving and restoring tasks. */
abstract class TaskChangeContractTest : TaskRepositoryContractBase() {
    private val editedDraft =
        draft.copy(name = "Water filter", note = "Monthly", goal = 2, onMiss = OnMiss.EXPIRES, time = LocalTime.NOON)

    @Test
    fun `updating changes the fields, keeps startDate and createdAt and bumps updatedAt`() =
        runTest {
            val task = givenTask()
            clock.advanceBy(clockAdvance)

            tasks.updateTask(task.id, editedDraft)

            val expected =
                task.copy(
                    name = "Water filter",
                    note = "Monthly",
                    goal = 2,
                    onMiss = OnMiss.EXPIRES,
                    time = LocalTime.NOON,
                    updatedAt = nowAfterAdvance,
                )
            assertEquals(expected, tasks.observeTask(task.id).first())
        }

    @Test
    fun `updating the repeat type keeps the same id`() =
        runTest {
            val task = givenTask()
            val oneTime = Repeat.OneTime(LocalDate.of(2025, 4, 1))

            tasks.updateTask(task.id, draft.copy(repeat = oneTime))

            assertEquals(listOf(task.id to oneTime), tasks.observeTasks().first().map { it.id to it.repeat })
        }

    @Test
    fun `updating to several times a day stores them sorted and without repeats`() =
        runTest {
            val task = givenTask()
            val evening = LocalTime.of(20, 0)
            val morning = LocalTime.of(8, 0)

            tasks.updateTask(task.id, draft.copy(repeat = Repeat.TimesPerDay(listOf(evening, morning, morning))))

            val stored = tasks.observeTask(task.id).first()?.repeat
            assertEquals(Repeat.TimesPerDay(listOf(morning, evening)), stored)
        }

    @Test
    fun `updating a deleted task does not bring it back`() =
        runTest {
            val task = givenDeletedTask()

            tasks.updateTask(task.id, editedDraft)

            assertNull(tasks.observeTask(task.id).first())
        }

    @Test
    fun `updating an unknown id changes nothing`() =
        runTest {
            tasks.updateTask("unknown", editedDraft)

            assertEquals(emptyList<Task>(), tasks.observeTasks().first())
        }

    @Test
    fun `archiving sets archivedAt from the clock and keeps the task listed`() =
        runTest {
            val task = givenTask()
            clock.advanceBy(clockAdvance)

            tasks.archiveTask(task.id)

            assertEquals(
                listOf(task.copy(archivedAt = nowAfterAdvance, updatedAt = nowAfterAdvance)),
                tasks.observeTasks().first(),
            )
        }

    @Test
    fun `restoring clears archivedAt`() =
        runTest {
            val task = givenTask()
            tasks.archiveTask(task.id)

            tasks.restoreTask(task.id)

            assertNull(tasks.observeTask(task.id).first()?.archivedAt)
        }
}
