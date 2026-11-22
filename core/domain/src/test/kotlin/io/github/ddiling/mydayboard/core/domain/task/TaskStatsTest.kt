package io.github.ddiling.mydayboard.core.domain.task

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class TaskStatsTest {
    private val today: LocalDate = wednesday.plusDays(40)

    private fun everyDayTicks(
        from: LocalDate,
        to: LocalDate,
        skip: Set<LocalDate> = emptySet(),
    ): List<TaskTick> =
        generateSequence(from) { it.plusDays(1) }
            .takeWhile { !it.isAfter(to) }
            .filter { it !in skip }
            .map { tick(it) }
            .toList()

    @Test
    fun `since is the start date`() {
        val start = wednesday.plusDays(3)

        assertEquals(start, computeTaskStats(task(startDate = start), emptyList(), today).since)
    }

    @Test
    fun `a daily task done 18 of the last 21 days counts 18 of 21 and a streak up to today`() {
        val start = today.minusDays(21)
        val missed = setOf(start.plusDays(2), start.plusDays(5), start.plusDays(9))
        val ticks = everyDayTicks(start, today.minusDays(1), missed)

        val stats = computeTaskStats(task(startDate = start), ticks, today)

        assertEquals(18.0, stats.doneOfPlanned, 0.0)
        assertEquals(21, stats.plannedDays)
        assertEquals(11, stats.streak)
    }

    @Test
    fun `today's undone occurrence is not planned`() {
        val stats = computeTaskStats(task(startDate = today.minusDays(2)), emptyList(), today)

        assertEquals(2, stats.plannedDays)
    }

    @Test
    fun `today's done occurrence is planned and in the streak`() {
        val stats = computeTaskStats(task(startDate = today), listOf(tick(today)), today)

        assertEquals(1, stats.plannedDays)
        assertEquals(1, stats.streak)
    }

    @Test
    fun `an upcoming occurrence is not counted anywhere`() {
        val future = today.plusDays(1)

        val stats = computeTaskStats(task(Repeat.OneTime(future)), listOf(tick(future)), today)

        assertEquals(0, stats.plannedDays)
        assertEquals(0, stats.skipped)
        assertEquals(0.0, stats.doneOfPlanned, 0.0)
    }

    @Test
    fun `a day that is not planned does not break the streak`() {
        val monday = LocalDate.of(2026, 11, 16)
        val weekdays = Repeat.Weekdays(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
        val ticks = listOf(tick(monday), tick(monday.plusDays(2)))

        val stats = computeTaskStats(task(weekdays, startDate = monday), ticks, monday.plusDays(3))

        assertEquals(2, stats.streak)
    }

    @Test
    fun `a missed planned day breaks the streak`() {
        val ticks = everyDayTicks(today.minusDays(5), today.minusDays(2))

        val stats = computeTaskStats(task(startDate = today.minusDays(5)), ticks, today)

        assertEquals(0, stats.streak)
    }

    @Test
    fun `a half day counts half, is a half day and breaks the streak`() {
        val times = Repeat.TimesPerDay(listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)))
        val ticks =
            listOf(tick(today.minusDays(2), LocalTime.of(8, 0))) +
                listOf(tick(today.minusDays(1), LocalTime.of(8, 0)), tick(today.minusDays(1), LocalTime.of(20, 0)))

        val stats = computeTaskStats(task(times, today.minusDays(2)), ticks, today)

        assertEquals(1.5, stats.doneOfPlanned, 0.0)
        assertEquals(1, stats.halfDays)
        assertEquals(1, stats.streak)
        assertEquals(0, stats.skipped)
    }

    @Test
    fun `skipped counts occurrences with no credit`() {
        val stats = computeTaskStats(task(startDate = today.minusDays(3)), listOf(tick(today.minusDays(3))), today)

        assertEquals(2, stats.skipped)
    }

    @Test
    fun `an older missed day is not skipped in the window but still ends the streak`() {
        val start = today.minusDays(45)
        val ticks = everyDayTicks(start.plusDays(1), today.minusDays(1))

        val stats = computeTaskStats(task(startDate = start), ticks, today)

        assertEquals(0, stats.skipped)
        assertEquals(44, stats.streak)
        assertEquals(30, stats.plannedDays)
    }

    @Test
    fun `a task started 5 days ago uses 5 calendar days`() {
        val stats = computeTaskStats(task(startDate = today.minusDays(5)), emptyList(), today)

        assertEquals(0.0, stats.creditPerDay, 0.0)
        assertEquals(5, stats.plannedDays)
    }

    @Test
    fun `credit per day is the credit sum over calendar days`() {
        val start = today.minusDays(4)
        val ticks = listOf(tick(start), tick(start.plusDays(1)), tick(start.plusDays(2)))

        val stats = computeTaskStats(task(startDate = start), ticks, today)

        assertEquals(0.75, stats.creditPerDay, 0.0)
    }

    @Test
    fun `ticks per day counts raw ticks above the goal`() {
        val start = today.minusDays(1)
        val ticks = List(4) { tick(start, LocalTime.of(it, 0)) }

        val stats = computeTaskStats(task(startDate = start, goal = 3), ticks, today)

        assertEquals(4.0, stats.ticksPerDay, 0.0)
        assertEquals(1.0, stats.creditPerDay, 0.0)
    }

    @Test
    fun `an undone one time task has no streak and counts the days past its due day`() {
        val due = today.minusDays(3)

        val stats = computeTaskStats(task(Repeat.OneTime(due), due, OnMiss.STAYS), emptyList(), today)

        assertEquals(0, stats.streak)
        assertEquals(3, stats.daysLate)
    }

    @Test
    fun `a done one time task has no days late`() {
        val due = today.minusDays(3)

        val stats = computeTaskStats(task(Repeat.OneTime(due), due, OnMiss.STAYS), listOf(tick(today)), today)

        assertNull(stats.daysLate)
    }

    @Test
    fun `a one time task not yet due has no days late`() {
        val due = today.plusDays(3)

        assertNull(computeTaskStats(task(Repeat.OneTime(due), due), emptyList(), today).daysLate)
    }

    @Test
    fun `an old late staying occurrence counts as settled and missed`() {
        val monday = LocalDate.of(2026, 11, 16)
        val weekly = Repeat.Weekdays(setOf(DayOfWeek.MONDAY))

        val stats = computeTaskStats(task(weekly, monday, OnMiss.STAYS), emptyList(), monday.plusDays(3))

        assertEquals(1, stats.plannedDays)
        assertEquals(1, stats.skipped)
    }

    @Test
    fun `a task started today with nothing due has all zeros`() {
        val stats = computeTaskStats(task(startDate = today), emptyList(), today)

        assertEquals(TaskStats(today, 0, 0.0, 0, 0.0, 0.0, 0, 0, null), stats)
    }

    @Test
    fun `18 days of a long break give streak 0 and a low count`() {
        val start = today.minusDays(30)
        val ticks = everyDayTicks(start, today.minusDays(19))

        val stats = computeTaskStats(task(startDate = start), ticks, today)

        assertEquals(0, stats.streak)
        assertEquals(12.0, stats.doneOfPlanned, 0.0)
        assertEquals(18, stats.skipped)
    }

    @Test
    fun `stats stop at the until day`() {
        val start = today.minusDays(10)
        val until = today.minusDays(6)

        val stats = computeTaskStats(task(startDate = start), emptyList(), today, until)

        assertEquals(5, stats.plannedDays)
    }

    @Test
    fun `the window moves by one day at midnight`() {
        val start = LocalDate.of(2026, 10, 1)
        val oldestDay = LocalDate.of(2026, 10, 23)
        val ticks = listOf(tick(oldestDay))

        val before = computeTaskStats(task(startDate = start), ticks, LocalDate.of(2026, 11, 22))
        val after = computeTaskStats(task(startDate = start), ticks, LocalDate.of(2026, 11, 23))

        assertEquals(1.0, before.doneOfPlanned, 0.0)
        assertEquals(0.0, after.doneOfPlanned, 0.0)
    }

    @Test
    fun `a zone change with the same instant moves today and the window together`() {
        val instant = Instant.parse("2026-11-22T23:30:00Z")
        val start = LocalDate.of(2026, 10, 1)
        val oldestDay = LocalDate.of(2026, 10, 23)
        val ticks = listOf(tick(oldestDay))

        fun doneIn(zone: String): Double {
            val day =
                Clock
                    .fixed(instant, ZoneId.of(zone))
                    .instant()
                    .atZone(ZoneId.of(zone))
                    .toLocalDate()
            return computeTaskStats(task(startDate = start), ticks, day).doneOfPlanned
        }

        assertEquals(1.0, doneIn("UTC"), 0.0)
        assertEquals(0.0, doneIn("Europe/Warsaw"), 0.0)
    }

    @Test
    fun `the window is 30 calendar days across the spring forward day`() {
        val springForward = LocalDate.of(2026, 3, 29)
        val start = springForward.minusDays(60)

        val stats = computeTaskStats(task(startDate = start), emptyList(), springForward.plusDays(5))

        assertEquals(STATS_WINDOW_DAYS, stats.plannedDays)
    }

    @Test
    fun `the window is 30 calendar days across the fall back day`() {
        val fallBack = LocalDate.of(2026, 10, 25)
        val start = fallBack.minusDays(60)

        val stats = computeTaskStats(task(startDate = start), emptyList(), fallBack.plusDays(5))

        assertEquals(STATS_WINDOW_DAYS, stats.plannedDays)
    }

    @Test
    fun `the window is 30 calendar days across a leap day`() {
        val leapDay = LocalDate.of(2028, 2, 29)

        val stats = computeTaskStats(task(startDate = leapDay.minusDays(60)), emptyList(), leapDay.plusDays(10))

        assertEquals(STATS_WINDOW_DAYS, stats.plannedDays)
    }
}
