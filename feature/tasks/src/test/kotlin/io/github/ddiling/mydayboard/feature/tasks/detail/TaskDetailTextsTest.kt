package io.github.ddiling.mydayboard.feature.tasks.detail

import androidx.test.core.app.ApplicationProvider
import io.github.ddiling.mydayboard.core.domain.task.Repeat
import io.github.ddiling.mydayboard.feature.tasks.common.taskOf
import io.github.ddiling.mydayboard.feature.tasks.common.textsToday
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
class TaskDetailTextsTest {
    private val resources = ApplicationProvider.getApplicationContext<android.content.Context>().resources
    private val today = textsToday
    private val since = LocalDate.of(2026, 10, 3)

    private fun hero(hero: Hero) = heroText(resources, hero, today)

    @Test
    fun `the status word reads as in the design for every hero`() {
        val words =
            listOf(
                Hero.NotDone,
                Hero.DoneToday,
                Hero.TimesProgress(1, 2),
                Hero.CountProgress(2, 3),
                Hero.LateSince(today.minusDays(2)),
                Hero.NotDueToday,
                Hero.Finished(reached = true),
                Hero.Finished(reached = false),
            ).map(::hero)

        assertEquals(
            listOf(
                "Not done yet",
                "Done today",
                "1 of 2 today",
                "2/3 today",
                "Late since Wed",
                "Not due today",
                "Done",
                "Missed",
            ),
            words,
        )
    }

    @Test
    fun `a task late for six days still says the weekday`() {
        assertEquals("Late since Sat", hero(Hero.LateSince(today.minusDays(6))))
    }

    @Test
    fun `a task late for exactly seven days says the date because the weekday would read as today`() {
        assertEquals("Late since 13 Nov", hero(Hero.LateSince(today.minusDays(7))))
    }

    @Test
    fun `the subtitle joins repeat, note and category and leaves out what is missing`() {
        val task = taskOf(Repeat.Weekdays(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)))

        val noted = task.copy(note = "with food")

        assertEquals("Mon, Wed · with food · Learning", subtitleText(resources, noted, "Learning"))
        assertEquals("Mon, Wed", subtitleText(resources, task, null))
    }

    @Test
    fun `the next due day reads as weekday, day and month`() {
        assertEquals("Next: Sat 28.11", nextDueText(resources, LocalDate.of(2026, 11, 28)))
    }

    @Test
    fun `big buttons say what a tap does and None has no text`() {
        val buttons =
            listOf(
                BigButton.MarkDone,
                BigButton.TickNext(LocalTime.of(20, 0)),
                BigButton.PlusOne,
                BigButton.UntickToday,
                BigButton.None,
            )
        val texts = buttons.map { bigButtonText(resources, it) }

        assertEquals(listOf("Mark as done", "Tick 20:00", "+1", "Untick today", null), texts)
    }

    @Test
    fun `history results read as in the design`() {
        val results =
            listOf(
                HistoryResult.Done,
                HistoryResult.Missed,
                HistoryResult.Late,
                HistoryResult.Partial(1, 2, severalTimes = true, settled = true),
                HistoryResult.Partial(1, 2, severalTimes = true, settled = false),
                HistoryResult.Partial(1, 3, severalTimes = false, settled = true),
                HistoryResult.Partial(2, 3, severalTimes = false, settled = false),
            ).map { historyResultText(resources, it) }

        assertEquals(
            listOf(
                "Done",
                "Missed",
                "Late · not done yet",
                "1 of 2 · half a day",
                "1 of 2",
                "1/3 · not reached",
                "2/3",
            ),
            results,
        )
    }

    @Test
    fun `history days say Today and Yesterday and otherwise the weekday and date`() {
        val older = LocalDate.of(2026, 11, 13)
        val days = listOf(today, today.minusDays(1), older).map { historyDayText(resources, it, today) }

        assertEquals(listOf("Today", "Yesterday", "Fri 13 Nov"), days)
    }

    @Test
    fun `the stats of a recurring task fill the grid`() {
        val stats = DetailStats.Recurring(since, 4, 7.5, 9, 0.85, countsTicks = false, skipped = 1, halfDays = null)

        val expected =
            listOf(
                "3 Oct" to "since",
                "4 days" to "streak",
                "7.5 of 9" to "last 30 days",
                "0.9" to "per day",
                "1" to "skipped",
            )
        assertEquals(expected, statCells(resources, stats).map { it.value to it.label })
    }

    @Test
    fun `a several-times task adds its half days and a whole credit has no decimal`() {
        val stats = DetailStats.Recurring(since, 0, 8.0, 9, 1.0, countsTicks = false, skipped = 0, halfDays = 2)

        val cells = statCells(resources, stats).map { it.value to it.label }

        assertEquals("8 of 9" to "last 30 days", cells[2])
        assertEquals("2" to "half days", cells.last())
    }

    @Test
    fun `a one-time task shows only when it started and how late it is`() {
        val cells = statCells(resources, DetailStats.Facts(since, daysLate = 2)).map { it.value to it.label }

        assertEquals(listOf("3 Oct" to "since", "2 days" to "late"), cells)
    }

    @Test
    fun `a one-time task that is not late shows only when it started`() {
        assertEquals(1, statCells(resources, DetailStats.Facts(since, daysLate = null)).size)
    }
}
