package io.github.ddiling.mydayboard.feature.tasks.common

import androidx.test.core.app.ApplicationProvider
import io.github.ddiling.mydayboard.core.domain.task.EveryNDaysMode
import io.github.ddiling.mydayboard.core.domain.task.Repeat
import io.github.ddiling.mydayboard.core.domain.task.RowState
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class DetailsTextTest {
    private val resources = ApplicationProvider.getApplicationContext<android.content.Context>().resources
    private val oneTime = taskOf(Repeat.OneTime(LocalDate.of(2026, 11, 10)))
    private val everyThreeDays =
        taskOf(Repeat.EveryNDays(3, EveryNDaysMode.FROM_LAST_DONE, LocalDate.of(2026, 1, 1)))

    @Test
    fun `a late task says the weekday it was due`() {
        val row = rowOf(oneTime, RowState.LATE, lateSince = LocalDate.of(2026, 11, 20).minusDays(2))

        assertEquals("late · was due Wed", detailsText(resources, row, textsToday))
    }

    @Test
    fun `a task late for more than a week says the date`() {
        val row = rowOf(oneTime, RowState.LATE, lateSince = LocalDate.of(2026, 11, 10))

        assertEquals("late · was due 10 Nov", detailsText(resources, row, textsToday))
    }

    @Test
    fun `a finished one-time task with every tick done says Done`() {
        val row = rowOf(oneTime, RowState.UPCOMING).copy(done = 1, target = 1)

        assertEquals("Done", detailsText(resources, row, textsToday))
    }

    @Test
    fun `a finished one-time task with ticks missing says Missed`() {
        val row = rowOf(oneTime, RowState.UPCOMING).copy(done = 1, target = 3)

        assertEquals("Missed", detailsText(resources, row, textsToday))
    }

    @Test
    fun `an upcoming every-N-days task adds the weekday of its next due day`() {
        val row = rowOf(everyThreeDays, RowState.UPCOMING, nextDue = LocalDate.of(2026, 11, 21))

        assertEquals("every 3 days · Sat", detailsText(resources, row, textsToday))
    }

    @Test
    fun `an upcoming monthly task adds the weekday of its next due day`() {
        val row = rowOf(taskOf(Repeat.Monthly(5)), RowState.UPCOMING, nextDue = LocalDate.of(2026, 11, 21))

        assertEquals("every month on the 5th · Sat", detailsText(resources, row, textsToday))
    }

    @Test
    fun `a pending task shows its schedule only`() {
        val row = rowOf(everyThreeDays, RowState.PENDING, nextDue = textsToday)

        assertEquals("every 3 days", detailsText(resources, row, textsToday))
    }
}
