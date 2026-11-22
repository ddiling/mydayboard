package io.github.ddiling.mydayboard.core.domain.task

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalTime

class TaskActionsSlotTest {
    private val fixture = TasksOverviewFixture()
    private val actions = TaskActions(fixture.tasks, fixture.ticks, fixture.clock)
    private val today = fixture.today
    private val morning = LocalTime.of(8, 0)
    private val evening = LocalTime.of(20, 0)

    private suspend fun twiceADay() = fixture.addTask("Pills", Repeat.TimesPerDay(listOf(morning, evening)))

    private suspend fun slotsTicked(task: Task) =
        fixture.ticks
            .observeTicks(task.id)
            .first()
            .filter { it.day == today }
            .map { it.slot }

    @Test
    fun `ticking the evening first leaves the morning free`() =
        runTest {
            val task = twiceADay()

            val outcome = actions.tickSlotToday(task.id, evening)

            assertEquals(evening, outcome?.slot)
            assertEquals(1, outcome?.done)
            assertEquals(2, outcome?.target)
            assertEquals(listOf<LocalTime?>(evening), slotsTicked(task))
        }

    @Test
    fun `ticking a time that is already ticked does nothing`() =
        runTest {
            val task = twiceADay()
            actions.tickSlotToday(task.id, morning)

            assertNull(actions.tickSlotToday(task.id, morning))
            assertEquals(listOf<LocalTime?>(morning), slotsTicked(task))
        }

    @Test
    fun `ticking a time the task does not have does nothing`() =
        runTest {
            val task = twiceADay()

            assertNull(actions.tickSlotToday(task.id, LocalTime.of(13, 0)))
            assertEquals(emptyList<LocalTime?>(), slotsTicked(task))
        }

    @Test
    fun `ticking a time of a task without several times does nothing`() =
        runTest {
            val task = fixture.addTask("Vacuum")

            assertNull(actions.tickSlotToday(task.id, morning))
        }

    @Test
    fun `ticking a time of an archived task does nothing`() =
        runTest {
            val task = twiceADay()
            fixture.tasks.archiveTask(task.id)

            assertNull(actions.tickSlotToday(task.id, morning))
        }

    @Test
    fun `unticking the morning keeps the evening and reports the progress`() =
        runTest {
            val task = twiceADay()
            actions.tickSlotToday(task.id, morning)
            actions.tickSlotToday(task.id, evening)

            val outcome = actions.untickSlotToday(task.id, morning)

            assertEquals(1, outcome?.done)
            assertEquals(2, outcome?.target)
            assertEquals(listOf<LocalTime?>(evening), slotsTicked(task))
        }

    @Test
    fun `unticking a time that is not ticked does nothing`() =
        runTest {
            val task = twiceADay()
            actions.tickSlotToday(task.id, evening)

            assertNull(actions.untickSlotToday(task.id, morning))
            assertEquals(listOf<LocalTime?>(evening), slotsTicked(task))
        }

    @Test
    fun `undo of an unticked time brings the tick back`() =
        runTest {
            val task = twiceADay()
            actions.tickSlotToday(task.id, morning)
            val outcome = checkNotNull(actions.untickSlotToday(task.id, morning))

            actions.undo(outcome)

            assertEquals(listOf<LocalTime?>(morning), slotsTicked(task))
        }
}
