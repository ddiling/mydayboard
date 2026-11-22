package io.github.ddiling.mydayboard.core.domain.task

import io.github.ddiling.mydayboard.core.domain.time.today
import io.github.ddiling.mydayboard.core.testing.MutableClock
import io.github.ddiling.mydayboard.core.testing.TestZones
import io.github.ddiling.mydayboard.core.testing.warsaw
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class CategoryStatsTest {
    private val today: LocalDate = wednesday.plusDays(40)
    private val warsaw: ZoneId = ZoneId.of("Europe/Warsaw")

    /** Just after local midnight in Warsaw, which is still the previous day in UTC. */
    private fun shortlyAfterMidnight(day: LocalDate): Instant =
        day.atTime(LocalTime.of(0, 30)).atZone(warsaw).toInstant()

    private fun meal(
        id: String,
        start: LocalDate,
        archivedAt: Instant? = null,
    ): Task = task(startDate = start).copy(id = id, archivedAt = archivedAt)

    private fun tickOf(
        task: Task,
        day: LocalDate,
    ): TaskTick = tick(day).copy(id = "${task.id}-$day", taskId = task.id)

    @Test
    fun `the average per day is the credit of all tasks over calendar days`() {
        val start = today.minusDays(10)
        val tasks = listOf(meal("breakfast", start), meal("lunch", start), meal("dinner", start))
        val ticks =
            tasks.flatMap { t -> (0 until 8).map { tickOf(t, start.plusDays(it.toLong())) } }

        val stats = computeCategoryStats(tasks, ticks, today, warsaw)

        assertEquals(2.4, stats.creditPerDay, 1e-9)
        assertEquals(24.0, stats.doneOfPlanned, 0.0)
        assertEquals(30, stats.plannedDays)
        assertEquals(6, stats.skipped)
        assertEquals(3, stats.taskCount)
    }

    @Test
    fun `archived tasks are included`() {
        val start = today.minusDays(2)
        val archivedAt = shortlyAfterMidnight(today.minusDays(1))
        val tasks = listOf(meal("active", start), meal("archived", start, archivedAt))

        assertEquals(4, computeCategoryStats(tasks, emptyList(), today, warsaw).plannedDays)
    }

    @Test
    fun `days after the archive date are not counted as missed`() {
        val archiveDay = today.minusDays(3)
        val archived = meal("archived", today.minusDays(5), shortlyAfterMidnight(archiveDay))

        val stats = computeCategoryStats(listOf(archived), emptyList(), today, warsaw)

        assertEquals(3, stats.plannedDays)
    }

    @Test
    fun `no tasks give zeros`() {
        assertEquals(CategoryStats(0, 0.0, 0.0, 0, 0), computeCategoryStats(emptyList(), emptyList(), today, warsaw))
    }

    @Test
    fun `a task started later does not lower the average of the days before it existed`() {
        val first = meal("first", today.minusDays(10))
        val later = meal("later", today.minusDays(5))
        val ticks =
            (0 until 10).map { tickOf(first, today.minusDays(10).plusDays(it.toLong())) } +
                (0 until 5).map { tickOf(later, today.minusDays(5).plusDays(it.toLong())) }

        val stats = computeCategoryStats(listOf(first, later), ticks, today, warsaw)

        assertEquals(1.5, stats.creditPerDay, 1e-9)
    }

    @Test
    fun `the window is the earliest start when that is less than 30 days ago`() {
        val young = meal("young", today.minusDays(5))

        assertEquals(5, computeCategoryStats(listOf(young), emptyList(), today, warsaw).plannedDays)
    }

    @Test
    fun `the window is the earliest start, but at most 30 days`() {
        val old = meal("old", today.minusDays(100))

        assertEquals(30, computeCategoryStats(listOf(old), emptyList(), today, warsaw).plannedDays)
    }

    @Test
    fun `a task archived 60 days ago adds none of its old history to the window`() {
        val active = meal("active", today.minusDays(100))
        val archived = meal("archived", today.minusDays(100), shortlyAfterMidnight(today.minusDays(60)))

        val stats = computeCategoryStats(listOf(active, archived), emptyList(), today, warsaw)

        assertEquals(30, stats.plannedDays)
        assertEquals(30, stats.skipped)
    }

    @Test
    fun `the average of an old archived task and an active one is per day of the 30 day window`() {
        val active = meal("active", today.minusDays(100))
        val archived = meal("archived", today.minusDays(100), shortlyAfterMidnight(today.minusDays(60)))
        val ticks = (1..30).map { tickOf(active, today.minusDays(it.toLong())) }

        val stats = computeCategoryStats(listOf(active, archived), ticks, today, warsaw)

        assertEquals(1.0, stats.creditPerDay, 1e-9)
    }

    @Test
    fun `the window ends today when the only task due today is done`() {
        val task = meal("daily", today.minusDays(2))
        val ticks = (0..2).map { tickOf(task, today.minusDays(it.toLong())) }

        assertEquals(3, computeCategoryStats(listOf(task), ticks, today, warsaw).plannedDays)
    }

    @Test
    fun `the window ends yesterday when a task due today is not done yet`() {
        val done = meal("done", today.minusDays(2))
        val notYet = meal("not-yet", today.minusDays(2))
        val ticks = (0..2).map { tickOf(done, today.minusDays(it.toLong())) }

        assertEquals(4, computeCategoryStats(listOf(done, notYet), ticks, today, warsaw).plannedDays)
    }

    private val taskStart: LocalDate = LocalDate.of(2026, 11, 10)

    private fun plannedDaysAt(
        clock: MutableClock,
        vararg tasks: Task,
    ): Int = computeCategoryStats(tasks.toList(), emptyList(), clock.today(), clock.zone).plannedDays

    @Test
    fun `the window still ends the day before yesterday at 23 59 59 999`() {
        val lastMomentOfDay = warsaw(2026, 11, 22, 23, 59).plusSeconds(59).plusMillis(999)
        val clock = MutableClock(lastMomentOfDay, TestZones.WARSAW)

        assertEquals(12, plannedDaysAt(clock, meal("daily", taskStart)))
    }

    @Test
    fun `the window grows by one day at midnight`() {
        val clock = MutableClock(warsaw(2026, 11, 23), TestZones.WARSAW)

        assertEquals(13, plannedDaysAt(clock, meal("daily", taskStart)))
    }

    @Test
    fun `the 30 day window holds 30 calendar days across the spring forward day`() {
        val clock = MutableClock(warsaw(2026, 4, 5, 12), TestZones.WARSAW)

        assertEquals(30, plannedDaysAt(clock, meal("daily", LocalDate.of(2026, 1, 1))))
    }

    @Test
    fun `the 30 day window holds 30 calendar days across the fall back day`() {
        val clock = MutableClock(warsaw(2026, 11, 3, 12), TestZones.WARSAW)

        assertEquals(30, plannedDaysAt(clock, meal("daily", LocalDate.of(2026, 1, 1))))
    }

    @Test
    fun `the window is the same at 00 30 on the spring forward day`() {
        val clock = MutableClock(warsaw(2026, 3, 29, 0, 30), TestZones.WARSAW)

        assertEquals(10, plannedDaysAt(clock, meal("daily", LocalDate.of(2026, 3, 19))))
    }

    @Test
    fun `the window is the same at 00 30 on the fall back day`() {
        val clock = MutableClock(warsaw(2026, 10, 25, 0, 30), TestZones.WARSAW)

        assertEquals(10, plannedDaysAt(clock, meal("daily", LocalDate.of(2026, 10, 15))))
    }

    @Test
    fun `an archive instant just after midnight in Warsaw caps at the Warsaw date, one day after the UTC date`() {
        val instant = Instant.parse("2026-11-09T23:30:00Z")
        val clock = MutableClock(warsaw(2026, 11, 22, 12), TestZones.WARSAW)

        assertEquals(10, plannedDaysAt(clock, meal("a", LocalDate.of(2026, 11, 1), instant)))
    }

    @Test
    fun `an archive instant late in the evening in New York caps at the New York date, one day before the UTC date`() {
        val instant = Instant.parse("2026-11-10T02:00:00Z")
        val newYork = ZoneId.of("America/New_York")
        val clock = MutableClock(warsaw(2026, 11, 22, 12), newYork)

        assertEquals(9, plannedDaysAt(clock, meal("a", LocalDate.of(2026, 11, 1), instant)))
    }

    @Test
    fun `a zone change with the same archive instant moves the archive day and the planned days`() {
        val instant = Instant.parse("2026-11-10T02:00:00Z")
        val clock = MutableClock(warsaw(2026, 11, 22, 12), TestZones.WARSAW)
        val task = meal("a", LocalDate.of(2026, 11, 1), instant)

        val inWarsaw = plannedDaysAt(clock, task)
        clock.zone = ZoneId.of("America/New_York")
        val inNewYork = plannedDaysAt(clock, task)

        assertEquals(10, inWarsaw)
        assertEquals(9, inNewYork)
    }

    @Test
    fun `an archive instant just after midnight on the spring forward day caps at that day`() {
        val clock = MutableClock(warsaw(2026, 4, 10, 12), TestZones.WARSAW)

        assertEquals(10, plannedDaysAt(clock, meal("a", LocalDate.of(2026, 3, 20), warsaw(2026, 3, 29, 0, 30))))
    }
}
