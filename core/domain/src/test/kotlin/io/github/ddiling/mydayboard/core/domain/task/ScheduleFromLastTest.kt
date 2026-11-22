package io.github.ddiling.mydayboard.core.domain.task

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ScheduleFromLastTest {
    private val everyThreeDays = Repeat.EveryNDays(3, EveryNDaysMode.FROM_LAST_DONE, wednesday)

    @Test
    fun `a tick on the due day of an expiring task makes the next due day due plus N`() {
        val ticks = listOf(tick(wednesday))

        val result = occurrencesOf(task(everyThreeDays), ticks, wednesday.plusDays(3), wednesday.plusDays(3))

        assertEquals(listOf(wednesday, wednesday.plusDays(3)), result.dues())
    }

    @Test
    fun `a late tick on a staying task makes the next due day the tick day plus N`() {
        val thursday = wednesday.plusDays(1)
        val sunday = wednesday.plusDays(4)

        val result = occurrencesOf(task(everyThreeDays, onMiss = OnMiss.STAYS), listOf(tick(thursday)), sunday)

        assertEquals(listOf(wednesday, sunday), result.dues())
    }

    @Test
    fun `a staying task never ticked has one open occurrence that is late once its due day is past`() {
        val result = occurrencesOf(task(everyThreeDays, onMiss = OnMiss.STAYS), emptyList(), wednesday.plusDays(10))

        assertEquals(listOf(OccurrenceStatus.LATE), result.map { it.status })
        assertNull(result.single().lastDay)
    }

    @Test
    fun `an expiring task that was missed has ended and the next due day is the missed day plus N`() {
        val result = occurrencesOf(task(everyThreeDays), emptyList(), wednesday.plusDays(3))

        assertEquals(listOf(wednesday, wednesday.plusDays(3)), result.dues())
        assertEquals(OccurrenceStatus.ENDED, result.first().status)
    }

    @Test
    fun `next due date of a staying task never ticked is its late due day`() {
        val result = nextDueDate(task(everyThreeDays, onMiss = OnMiss.STAYS), emptyList(), wednesday.plusDays(10))

        assertEquals(wednesday, result)
    }

    @Test
    fun `next due date after a completion is the completion day plus N`() {
        val result = nextDueDate(task(everyThreeDays, onMiss = OnMiss.STAYS), listOf(tick(wednesday)), wednesday)

        assertEquals(LocalDate.of(2026, 11, 21), result)
    }

    @Test
    fun `a tick before the due day does not complete the occurrence`() {
        val dueLater = Repeat.EveryNDays(3, EveryNDaysMode.FROM_LAST_DONE, wednesday)
        val earlyTick = tick(wednesday.minusDays(1))

        val result = occurrencesOf(task(dueLater, onMiss = OnMiss.STAYS), listOf(earlyTick), wednesday).single()

        assertEquals(OccurrenceStatus.DUE_TODAY, result.status)
    }
}
