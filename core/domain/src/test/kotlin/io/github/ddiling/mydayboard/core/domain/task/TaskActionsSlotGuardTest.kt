package io.github.ddiling.mydayboard.core.domain.task

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class TaskActionsSlotGuardTest {
    private val fixture = TasksOverviewFixture()
    private val actions = TaskActions(fixture.tasks, fixture.ticks, fixture.clock)
    private val today = fixture.today
    private val yesterday = today.minusDays(1)
    private val morning = LocalTime.of(8, 0)
    private val evening = LocalTime.of(20, 0)
    private val morningWithSeconds = LocalTime.of(8, 0, 30)

    private suspend fun twiceADay(startDate: LocalDate = today) =
        fixture.addTask("Pills", Repeat.TimesPerDay(listOf(morning, evening)), startDate = startDate)

    private suspend fun ticksOf(task: Task) = fixture.ticks.observeTicks(task.id).first()

    @Test
    fun `ticking a time of a task that starts tomorrow returns null and stores nothing`() =
        runTest {
            val task = twiceADay(startDate = today.plusDays(1))

            assertNull(actions.tickSlotToday(task.id, morning))
            assertEquals(emptyList<TaskTick>(), ticksOf(task))
        }

    @Test
    fun `ticking a time of a deleted task returns null`() =
        runTest {
            val task = twiceADay()
            fixture.tasks.deleteTask(task.id)

            assertNull(actions.tickSlotToday(task.id, morning))
        }

    @Test
    fun `unticking a time of an archived task returns null and keeps its tick`() =
        runTest {
            val task = twiceADay()
            actions.tickSlotToday(task.id, morning)
            fixture.tasks.archiveTask(task.id)

            assertNull(actions.untickSlotToday(task.id, morning))
            assertEquals(1, ticksOf(task).size)
        }

    @Test
    fun `unticking a time of a deleted task returns null`() =
        runTest {
            val task = twiceADay()
            actions.tickSlotToday(task.id, morning)
            fixture.tasks.deleteTask(task.id)

            assertNull(actions.untickSlotToday(task.id, morning))
        }

    @Test
    fun `a time with seconds is cut to the whole minute before it is checked against the task times`() =
        runTest {
            val task = twiceADay()

            val outcome = actions.tickSlotToday(task.id, morningWithSeconds)

            assertEquals(morning, outcome?.slot)
            assertEquals(listOf<LocalTime?>(morning), ticksOf(task).map { it.slot })
        }

    @Test
    fun `unticking a time with seconds removes the tick of that whole minute`() =
        runTest {
            val task = twiceADay()
            actions.tickSlotToday(task.id, morning)

            actions.untickSlotToday(task.id, morningWithSeconds)

            assertEquals(emptyList<TaskTick>(), ticksOf(task))
        }

    @Test
    fun `undo of a ticked time removes that tick again`() =
        runTest {
            val task = twiceADay()
            val outcome = checkNotNull(actions.tickSlotToday(task.id, evening))

            actions.undo(outcome)

            assertEquals(emptyList<TaskTick>(), ticksOf(task))
        }

    @Test
    fun `unticking a time never removes the tick of the same time from another day`() =
        runTest {
            val task = twiceADay(startDate = yesterday)
            actions.markDay(task.id, yesterday, listOf(morning))

            assertNull(actions.untickSlotToday(task.id, morning))
            assertEquals(listOf(yesterday), ticksOf(task).map { it.day })
        }
}
