package io.github.ddiling.mydayboard.core.domain.task

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ValidateTaskDraftTest {
    private fun draft(
        repeat: Repeat = Repeat.EveryDay,
        name: String = "Pills",
        goal: Int? = null,
        note: String? = null,
    ) = TaskDraft(
        name = name,
        categoryId = null,
        repeat = repeat,
        time = null,
        note = note,
        goal = goal,
        onMiss = OnMiss.STAYS,
    )

    private fun intervalDraft(days: Int) =
        draft(Repeat.EveryNDays(days, EveryNDaysMode.FIXED_CALENDAR, LocalDate.of(2026, 5, 1)))

    @Test
    fun `a valid every day draft has no errors`() {
        assertEquals(emptyList<TaskFormError>(), validateTaskDraft(draft()))
    }

    @Test
    fun `several times with no times is NoTimes`() {
        val result = validateTaskDraft(draft(Repeat.TimesPerDay(emptyList())))

        assertEquals(listOf(TaskFormError.NoTimes), result)
    }

    @Test
    fun `several times with a time has no errors`() {
        val result = validateTaskDraft(draft(Repeat.TimesPerDay(listOf(LocalTime.of(8, 0)))))

        assertEquals(emptyList<TaskFormError>(), result)
    }

    @Test
    fun `weekdays with none chosen is NoWeekdays`() {
        val result = validateTaskDraft(draft(Repeat.Weekdays(emptySet())))

        assertEquals(listOf(TaskFormError.NoWeekdays), result)
    }

    @Test
    fun `an interval of 0 or 366 is out of range and 1 and 365 are valid`() {
        val outOfRange = TaskFormError.IntervalOutOfRange(1, 365)

        assertEquals(listOf(outOfRange), validateTaskDraft(intervalDraft(0)))
        assertEquals(listOf(outOfRange), validateTaskDraft(intervalDraft(366)))
        assertEquals(emptyList<TaskFormError>(), validateTaskDraft(intervalDraft(1)))
        assertEquals(emptyList<TaskFormError>(), validateTaskDraft(intervalDraft(365)))
    }

    @Test
    fun `a day of month of 0 or 32 is out of range and 31 is valid`() {
        val outOfRange = listOf(TaskFormError.DayOfMonthOutOfRange)

        assertEquals(outOfRange, validateTaskDraft(draft(Repeat.Monthly(0))))
        assertEquals(outOfRange, validateTaskDraft(draft(Repeat.Monthly(32))))
        assertEquals(emptyList<TaskFormError>(), validateTaskDraft(draft(Repeat.Monthly(31))))
    }

    @Test
    fun `a goal of 1 or 100 is out of range and 2 and 99 are valid`() {
        val outOfRange = listOf(TaskFormError.GoalOutOfRange(2, 99))

        assertEquals(outOfRange, validateTaskDraft(draft(goal = 1)))
        assertEquals(outOfRange, validateTaskDraft(draft(goal = 100)))
        assertEquals(emptyList<TaskFormError>(), validateTaskDraft(draft(goal = 2)))
        assertEquals(emptyList<TaskFormError>(), validateTaskDraft(draft(goal = 99)))
    }

    @Test
    fun `a note of 81 code points is too long and 80 is valid`() {
        val tooLong = listOf(TaskFormError.NoteTooLong(80))

        assertEquals(tooLong, validateTaskDraft(draft(note = "a".repeat(81))))
        assertEquals(emptyList<TaskFormError>(), validateTaskDraft(draft(note = "a".repeat(80))))
    }

    @Test
    fun `several errors come back together in form order`() {
        val result = validateTaskDraft(draft(Repeat.Weekdays(emptySet()), name = " ", goal = 1, note = "a".repeat(81)))

        assertEquals(
            listOf(
                TaskFormError.BlankName,
                TaskFormError.NoWeekdays,
                TaskFormError.GoalOutOfRange(2, 99),
                TaskFormError.NoteTooLong(80),
            ),
            result,
        )
    }

    @Test
    fun `a name that is too long is reported with the limit`() {
        val result = validateTaskDraft(draft(name = "a".repeat(61)))

        assertEquals(listOf(TaskFormError.NameTooLong(60)), result)
    }
}
