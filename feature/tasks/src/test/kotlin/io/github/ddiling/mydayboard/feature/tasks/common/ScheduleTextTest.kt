package io.github.ddiling.mydayboard.feature.tasks.common

import androidx.test.core.app.ApplicationProvider
import io.github.ddiling.mydayboard.core.domain.task.EveryNDaysMode
import io.github.ddiling.mydayboard.core.domain.task.Repeat
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
class ScheduleTextTest {
    private val resources = ApplicationProvider.getApplicationContext<android.content.Context>().resources
    private val morning = LocalTime.of(8, 30)

    private fun text(
        repeat: Repeat,
        time: LocalTime? = null,
    ) = scheduleText(resources, taskOf(repeat, time))

    private fun everyDays(days: Int) = Repeat.EveryNDays(days, EveryNDaysMode.FROM_LAST_DONE, LocalDate.of(2026, 1, 1))

    @Test
    fun `an every-day task with a time shows just the time`() {
        assertEquals("08:30", text(Repeat.EveryDay, morning))
    }

    @Test
    fun `an every-day task without a time says every day`() {
        assertEquals("every day", text(Repeat.EveryDay))
    }

    @Test
    fun `a several-times task lists its times`() {
        val times = listOf(LocalTime.of(8, 0), LocalTime.of(20, 0))

        assertEquals("08:00, 20:00", text(Repeat.TimesPerDay(times)))
    }

    @Test
    fun `chosen weekdays are listed in week order`() {
        val days = setOf(DayOfWeek.FRIDAY, DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)

        assertEquals("Mon, Wed, Fri", text(Repeat.Weekdays(days)))
    }

    @Test
    fun `monday to friday reads as every weekday`() {
        val days = DayOfWeek.entries.take(5).toSet()

        assertEquals("every weekday", text(Repeat.Weekdays(days)))
    }

    @Test
    fun `all seven weekdays read as every day`() {
        assertEquals("every day", text(Repeat.Weekdays(DayOfWeek.entries.toSet())))
    }

    @Test
    fun `all seven weekdays with a time show just the time like every day`() {
        assertEquals("08:30", text(Repeat.Weekdays(DayOfWeek.entries.toSet()), morning))
    }

    @Test
    fun `a time follows the weekdays after a dot`() {
        assertEquals("Mon · 08:30", text(Repeat.Weekdays(setOf(DayOfWeek.MONDAY)), morning))
    }

    @Test
    fun `every several days uses the plural`() {
        assertEquals("every 3 days", text(everyDays(3)))
    }

    @Test
    fun `monthly names the day with an English ordinal`() {
        val ordinals = listOf(1, 2, 3, 5, 11, 12, 13, 21, 22).map { text(Repeat.Monthly(it)) }

        assertEquals(
            listOf(
                "every month on the 1st",
                "every month on the 2nd",
                "every month on the 3rd",
                "every month on the 5th",
                "every month on the 11th",
                "every month on the 12th",
                "every month on the 13th",
                "every month on the 21st",
                "every month on the 22nd",
            ),
            ordinals,
        )
    }

    @Test
    fun `a one-time task shows its date`() {
        assertEquals("27 Nov", text(Repeat.OneTime(LocalDate.of(2026, 11, 27))))
    }
}
