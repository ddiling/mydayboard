package io.github.ddiling.mydayboard.core.domain.task

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
}
