package io.github.ddiling.mydayboard.core.domain.task

import app.cash.turbine.test
import io.github.ddiling.mydayboard.core.testing.TestZones
import io.github.ddiling.mydayboard.core.testing.warsaw
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class ObserveTasksOverviewUseCaseTest {
    private val beforeMidnight = warsaw(2026, 11, 19).minusMillis(1)
    private val midnight = warsaw(2026, 11, 19)
    private val thursday = LocalDate.of(2026, 11, 19)

    @Test
    fun `at 23-59-59-999 a task is due today`() =
        runTest {
            val fixture = TasksOverviewFixture(beforeMidnight)
            val task = fixture.addTask("Vacuum", repeat = Repeat.Weekdays(setOf(DayOfWeek.WEDNESDAY)))

            assertEquals(RowState.PENDING, fixture.rowOf(task).state)
        }

    @Test
    fun `at midnight the new day's tasks appear and yesterday's undone expiring task is gone`() =
        runTest {
            val fixture = TasksOverviewFixture(beforeMidnight)
            val yesterdays = fixture.addTask("CVs", repeat = Repeat.OneTime(fixture.today))
            val todays = fixture.addTask("Vacuum", repeat = Repeat.Weekdays(setOf(DayOfWeek.THURSDAY)))

            fixture.clock.now = midnight
            fixture.tick()

            assertEquals(RowState.UPCOMING, fixture.rowOf(yesterdays).state)
            assertEquals(RowState.PENDING, fixture.rowOf(todays).state)
        }

    @Test
    fun `the overview is recomputed at the minute tick without a data change`() =
        runTest {
            val fixture = TasksOverviewFixture(beforeMidnight)
            fixture.useCase().test {
                assertEquals(fixture.today, awaitItem().today)

                fixture.clock.now = midnight
                fixture.tick()

                assertEquals(thursday, awaitItem().today)
            }
        }

    @Test
    fun `on the spring-forward day the overview's day is the local date at 00-30 and 23-30`() =
        runTest {
            val fixture = TasksOverviewFixture(warsaw(2026, 3, 29, 0, 30))
            val atStart = fixture.overview().today
            fixture.clock.now = warsaw(2026, 3, 29, 23, 30)
            fixture.tick()
            val atEnd = fixture.overview().today

            assertEquals(listOf(LocalDate.of(2026, 3, 29), LocalDate.of(2026, 3, 29)), listOf(atStart, atEnd))
        }

    @Test
    fun `on the fall-back day the overview's day is the local date at 00-30 and 23-30`() =
        runTest {
            val fixture = TasksOverviewFixture(warsaw(2026, 10, 25, 0, 30))
            val atStart = fixture.overview().today
            fixture.clock.now = warsaw(2026, 10, 25, 23, 30)
            fixture.tick()
            val atEnd = fixture.overview().today

            assertEquals(listOf(LocalDate.of(2026, 10, 25), LocalDate.of(2026, 10, 25)), listOf(atStart, atEnd))
        }

    @Test
    fun `a zone change with the same instant moves the day and recomputes`() =
        runTest {
            val fixture = TasksOverviewFixture(warsaw(2026, 11, 18, 23, 30))
            fixture.useCase().test {
                assertEquals(fixture.today, awaitItem().today)

                fixture.clock.zone = TestZones.TOKYO
                fixture.tick()

                assertEquals(thursday, awaitItem().today)
            }
        }

    @Test
    fun `a tick added right after a ticker emission is counted at once`() =
        runTest {
            val fixture = TasksOverviewFixture(beforeMidnight)
            val task = fixture.addTask("Vacuum")
            fixture.useCase().test {
                assertEquals(RowState.PENDING, awaitItem().rows.single().state)

                // The ticker still holds the old minute; only the clock knows it is a new day.
                fixture.clock.now = midnight
                fixture.ticks.addTick(task.id, thursday, null)

                val overview = expectMostRecentItem()
                assertEquals(RowState.DONE, overview.rows.single().state)
            }
        }
}
