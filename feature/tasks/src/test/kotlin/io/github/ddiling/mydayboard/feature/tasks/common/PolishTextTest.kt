package io.github.ddiling.mydayboard.feature.tasks.common

import androidx.test.core.app.ApplicationProvider
import io.github.ddiling.mydayboard.core.domain.task.EveryNDaysMode
import io.github.ddiling.mydayboard.core.domain.task.Repeat
import io.github.ddiling.mydayboard.core.domain.task.RowState
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "pl")
class PolishTextTest {
    private val resources = ApplicationProvider.getApplicationContext<android.content.Context>().resources

    private fun everyDays(days: Int) =
        taskOf(Repeat.EveryNDays(days, EveryNDaysMode.FROM_LAST_DONE, LocalDate.of(2026, 1, 1)))

    @Test
    fun `the plural of days follows the Polish rules`() {
        val texts = listOf(1, 3, 5).map { scheduleText(resources, everyDays(it)) }

        assertEquals(listOf("co dzień", "co 3 dni", "co 5 dni"), texts)
    }

    @Test
    fun `a monthly task keeps a plain full stop after the day`() {
        assertEquals("co miesiąc, 5.", scheduleText(resources, taskOf(Repeat.Monthly(5))))
    }

    @Test
    fun `a finished one-time task says Zrobione`() {
        val oneTime = taskOf(Repeat.OneTime(LocalDate.of(2026, 11, 10)))
        val row = rowOf(oneTime, RowState.UPCOMING).copy(done = 1, target = 1)

        assertEquals("Zrobione", detailsText(resources, row, textsToday))
    }
}
