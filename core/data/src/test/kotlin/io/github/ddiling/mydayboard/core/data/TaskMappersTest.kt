package io.github.ddiling.mydayboard.core.data

import io.github.ddiling.mydayboard.core.domain.task.EveryNDaysMode
import io.github.ddiling.mydayboard.core.domain.task.OnMiss
import io.github.ddiling.mydayboard.core.domain.task.Repeat
import io.github.ddiling.mydayboard.core.domain.task.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

private fun taskOf(
    repeat: Repeat,
    startDate: LocalDate = LocalDate.of(2025, 3, 10),
) = Task(
    "t1",
    "Vacuum",
    "c1",
    repeat,
    LocalTime.of(9, 30),
    "note",
    2,
    OnMiss.EXPIRES,
    startDate,
    Instant.ofEpochMilli(1_000),
    Instant.ofEpochMilli(2_000),
    Instant.ofEpochMilli(3_000),
)

@RunWith(Parameterized::class)
class TaskRepeatRoundTripTest(
    private val repeat: Repeat,
) {
    @Test
    fun `task survives a round trip through the entity`() {
        val task = taskOf(repeat)

        assertEquals(task, task.toEntity().toDomain())
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun repeats(): List<Repeat> =
            listOf(
                Repeat.TimesPerDay(listOf(LocalTime.of(8, 0), LocalTime.of(20, 0))),
                Repeat.EveryDay,
                Repeat.Weekdays(setOf(DayOfWeek.TUESDAY, DayOfWeek.SUNDAY)),
                Repeat.EveryNDays(3, EveryNDaysMode.FIXED_CALENDAR, LocalDate.of(2025, 3, 12)),
                Repeat.Monthly(31),
                Repeat.OneTime(LocalDate.of(2025, 4, 1)),
            )
    }
}

class TaskMappersTest {
    @Test
    fun `only Monday is bit 1`() {
        assertEquals(1, setOf(DayOfWeek.MONDAY).toMask())
    }

    @Test
    fun `only Sunday is bit 64`() {
        assertEquals(64, setOf(DayOfWeek.SUNDAY).toMask())
    }

    @Test
    fun `all seven days make 127 and read back as all seven`() {
        val all = DayOfWeek.entries.toSet()

        assertEquals(127, all.toMask())
        assertEquals(all, 127.toWeekdays())
    }

    @Test
    fun `times keep their order and minutes as comma separated text`() {
        val repeat = Repeat.TimesPerDay(listOf(LocalTime.of(8, 0), LocalTime.of(20, 5)))

        assertEquals("08:00,20:05", taskOf(repeat).toEntity().times)
    }

    @Test
    fun `start date is stored as epoch days at a leap day and a year change`() {
        val leapDay = LocalDate.of(2028, 2, 29)
        val newYear = LocalDate.of(2026, 1, 1)

        assertEquals(leapDay.toEpochDay(), taskOf(Repeat.EveryDay, leapDay).toEntity().startDate)
        assertEquals(newYear, taskOf(Repeat.EveryDay, newYear).toEntity().toDomain().startDate)
    }

    @Test
    fun `a weekdays row without its weekdays column fails naming the column`() {
        val corrupt = taskOf(Repeat.Weekdays(setOf(DayOfWeek.MONDAY))).toEntity().copy(weekdays = null)

        val error = assertThrows(IllegalStateException::class.java) { corrupt.toDomain() }

        assertTrue(error.message.orEmpty().contains("weekdays"))
    }
}
