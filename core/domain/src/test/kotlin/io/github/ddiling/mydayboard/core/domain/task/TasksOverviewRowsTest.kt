package io.github.ddiling.mydayboard.core.domain.task

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class TasksOverviewRowsTest {
    private val fixture = TasksOverviewFixture()
    private val today = fixture.today
    private val yesterday = today.minusDays(1)

    @Test
    fun `an every-day task due today is pending until it is ticked`() =
        runTest {
            val task = fixture.addTask("Vacuum")
            assertEquals(RowState.PENDING, fixture.rowOf(task).state)

            fixture.ticks.addTick(task.id, today, null)

            assertEquals(RowState.DONE, fixture.rowOf(task).state)
        }

    @Test
    fun `a task not planned today is upcoming with its next due date`() =
        runTest {
            val monday = Repeat.Weekdays(setOf(DayOfWeek.MONDAY))
            val task = fixture.addTask("Water filter", repeat = monday)

            val row = fixture.rowOf(task)

            assertEquals(RowState.UPCOMING, row.state)
            assertEquals(LocalDate.of(2026, 11, 23), row.nextDue)
        }

    @Test
    fun `a late bill that stays is late since its due day`() =
        runTest {
            val bill =
                fixture.addTask(
                    "Pay bills",
                    repeat = Repeat.Monthly(15),
                    startDate = LocalDate.of(2026, 11, 1),
                ) { copy(onMiss = OnMiss.STAYS) }

            val row = fixture.rowOf(bill)

            assertEquals(RowState.LATE, row.state)
            assertEquals(LocalDate.of(2026, 11, 15), row.lateSince)
        }

    @Test
    fun `an every-day task that expired yesterday is pending again, never late`() =
        runTest {
            val task = fixture.addTask("Vacuum", startDate = yesterday)

            val row = fixture.rowOf(task)

            assertEquals(RowState.PENDING, row.state)
            assertNull(row.lateSince)
        }

    @Test
    fun `a one-time task that expired yesterday is upcoming with nothing left`() =
        runTest {
            val task = fixture.addTask("CVs", repeat = Repeat.OneTime(yesterday), startDate = yesterday)

            val row = fixture.rowOf(task)

            assertEquals(RowState.UPCOMING, row.state)
            assertNull(row.nextDue)
        }

    @Test
    fun `an archived task is not a row but is counted`() =
        runTest {
            val task = fixture.addTask("Vacuum")
            fixture.tasks.archiveTask(task.id)

            val overview = fixture.overview()

            assertTrue(overview.rows.isEmpty())
            assertEquals(1, overview.archivedCount)
        }

    @Test
    fun `today's totals count only tasks due today`() =
        runTest {
            val done = fixture.addTask("Breakfast")
            fixture.addTask("Vacuum")
            fixture.addTask("Water filter", repeat = Repeat.Weekdays(setOf(DayOfWeek.MONDAY)))
            fixture.ticks.addTick(done.id, today, null)

            val overview = fixture.overview()

            assertEquals(2, overview.totalToday)
            assertEquals(1, overview.doneToday)
        }

    @Test
    fun `empty repositories give no rows and 0 of 0`() =
        runTest {
            val overview = fixture.overview()

            assertTrue(overview.rows.isEmpty())
            assertEquals(0, overview.totalToday)
            assertEquals(0, overview.doneToday)
        }

    @Test
    fun `when everything is done, done equals total`() =
        runTest {
            val first = fixture.addTask("Breakfast")
            val second = fixture.addTask("Vacuum")
            fixture.ticks.addTick(first.id, today, null)
            fixture.ticks.addTick(second.id, today, null)

            val overview = fixture.overview()

            assertEquals(overview.totalToday, overview.doneToday)
        }

    @Test
    fun `a count goal task shows how many are done of its goal`() =
        runTest {
            val task = fixture.addTask("Water") { copy(goal = 3) }
            fixture.ticks.addTick(task.id, today, null)
            fixture.ticks.addTick(task.id, today, null)

            val row = fixture.rowOf(task)

            assertEquals(2, row.done)
            assertEquals(3, row.target)
        }
}
