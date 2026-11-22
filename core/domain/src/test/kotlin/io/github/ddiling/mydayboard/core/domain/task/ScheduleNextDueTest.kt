package io.github.ddiling.mydayboard.core.domain.task

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek

class ScheduleNextDueTest {
    @Test
    fun `a one time task due in the future has its date as the next due date`() {
        val due = wednesday.plusDays(5)

        assertEquals(due, nextDueDate(task(Repeat.OneTime(due)), emptyList(), wednesday))
    }

    @Test
    fun `next due date of an every day task done today is tomorrow`() {
        val result = nextDueDate(task(), listOf(tick(wednesday)), wednesday)

        assertEquals(wednesday.plusDays(1), result)
    }

    @Test
    fun `next due date of an every day task not done today is today`() {
        assertEquals(wednesday, nextDueDate(task(), emptyList(), wednesday))
    }

    @Test
    fun `next due date of a late staying task is its late due day`() {
        val weekdays = Repeat.Weekdays(setOf(DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
        val lateFriday = wednesday.plusDays(2)

        val result =
            nextDueDate(task(weekdays, wednesday, OnMiss.STAYS), listOf(tick(wednesday)), lateFriday.plusDays(1))

        assertEquals(lateFriday, result)
    }

    @Test
    fun `a finished one time task has no next due date`() {
        val oneTime = task(Repeat.OneTime(wednesday.minusDays(3)), onMiss = OnMiss.EXPIRES)

        assertNull(nextDueDate(oneTime, emptyList(), wednesday))
    }
}
