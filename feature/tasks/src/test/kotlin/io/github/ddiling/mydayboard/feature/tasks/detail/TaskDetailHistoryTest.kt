package io.github.ddiling.mydayboard.feature.tasks.detail

import io.github.ddiling.mydayboard.core.domain.task.Repeat
import io.github.ddiling.mydayboard.core.domain.task.Task
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class TaskDetailHistoryTest {
    private val fixture = TaskDetailFixture()
    private val yesterday = fixture.today.minusDays(1)
    private val nineDaysAgo = fixture.today.minusDays(9)

    @Test
    fun `history lists planned days newest first and skips days the task is not planned`() =
        runTest {
            val monday = LocalDate.of(2026, 11, 9)
            val wednesday = LocalDate.of(2026, 11, 11)
            val nextMonday = LocalDate.of(2026, 11, 16)
            val task =
                fixture.addTask(
                    "English",
                    Repeat.Weekdays(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)),
                    startDate = monday,
                )
            fixture.tickOn(task, monday)
            fixture.tickOn(task, nextMonday)

            val history = fixture.content(task).history

            assertEquals(
                listOf(
                    HistoryRow(nextMonday, HistoryResult.Done),
                    HistoryRow(wednesday, HistoryResult.Missed),
                    HistoryRow(monday, HistoryResult.Done),
                ),
                history,
            )
        }

    @Test
    fun `a one-time task that expired undone shows as missed`() =
        runTest {
            val task = fixture.addTask("Send CVs", Repeat.OneTime(yesterday), startDate = yesterday)

            assertEquals(listOf(HistoryRow(yesterday, HistoryResult.Missed)), fixture.content(task).history)
        }

    @Test
    fun `a half day of a several-times task is settled as a partial row`() =
        runTest {
            val task =
                fixture.addTask(
                    "Pills",
                    Repeat.TimesPerDay(listOf(LocalTime.of(8, 0), LocalTime.of(20, 0))),
                    startDate = yesterday,
                )
            fixture.tickOn(task, yesterday, LocalTime.of(8, 0))

            val row = fixture.content(task).history.single()

            assertEquals(HistoryRow(yesterday, HistoryResult.Partial(1, 2, severalTimes = true, settled = true)), row)
        }

    @Test
    fun `an unfinished count goal day is settled as a partial row`() =
        runTest {
            val task = fixture.addTask("Send CVs", goal = 3, startDate = yesterday)
            fixture.tickOn(task, yesterday)

            val row = fixture.content(task).history.single()

            assertEquals(HistoryRow(yesterday, HistoryResult.Partial(1, 3, severalTimes = false, settled = true)), row)
        }

    @Test
    fun `today with progress is listed as not settled`() =
        runTest {
            val task = fixture.addTask("Send CVs", goal = 3)
            fixture.tickOn(task, fixture.today)

            val partial = HistoryResult.Partial(1, 3, severalTimes = false, settled = false)
            assertEquals(listOf(HistoryRow(fixture.today, partial)), fixture.content(task).history)
        }

    @Test
    fun `an untouched today is not listed`() =
        runTest {
            val task = fixture.addTask("Send CVs", goal = 3)

            assertTrue(fixture.content(task).history.isEmpty())
        }

    private suspend fun nineDayTask(): Task {
        val task = fixture.addTask("Water filter", startDate = nineDaysAgo)
        (0L..8L).filter { it != 6L }.forEach { fixture.tickOn(task, nineDaysAgo.plusDays(it)) }
        return task
    }

    @Test
    fun `stats show the known history of a daily task`() =
        runTest {
            val stats = fixture.content(nineDayTask()).stats as DetailStats.Recurring

            assertEquals(nineDaysAgo, stats.since)
            assertEquals(2, stats.streak)
            assertEquals(8.0, stats.doneOfPlanned, 0.0)
            assertEquals(9, stats.plannedDays)
            assertEquals(8.0 / 9, stats.perDay, 1e-9)
            assertEquals(1, stats.skipped)
            assertNull(stats.halfDays)
        }

    @Test
    fun `unticking today takes the streak back in the same state flow`() =
        runTest {
            val task = nineDayTask()
            val today = fixture.tickOn(task, fixture.today)
            val states = fixture.run { collectStates(task.id) }
            assertEquals(3, ((states.last() as TaskDetailUiState.Content).stats as DetailStats.Recurring).streak)

            fixture.ticks.deleteTick(checkNotNull(today).id)
            runCurrent()

            assertEquals(2, ((states.last() as TaskDetailUiState.Content).stats as DetailStats.Recurring).streak)
        }

    @Test
    fun `a one-time task shows facts and no streak`() =
        runTest {
            val task = fixture.addTask("Send CVs", Repeat.OneTime(yesterday), startDate = nineDaysAgo)

            val stats = fixture.content(task).stats

            assertEquals(DetailStats.Facts(nineDaysAgo, daysLate = 1), stats)
        }
}
