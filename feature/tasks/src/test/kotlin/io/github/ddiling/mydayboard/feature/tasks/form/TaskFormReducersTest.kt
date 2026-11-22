package io.github.ddiling.mydayboard.feature.tasks.form

import io.github.ddiling.mydayboard.core.domain.task.MAX_DAY_OF_MONTH
import io.github.ddiling.mydayboard.core.domain.task.MAX_INTERVAL_DAYS
import io.github.ddiling.mydayboard.core.domain.task.MIN_INTERVAL_DAYS
import io.github.ddiling.mydayboard.core.domain.task.MIN_TASK_GOAL
import io.github.ddiling.mydayboard.core.domain.task.OnMiss
import io.github.ddiling.mydayboard.core.domain.task.TaskFormError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class TaskFormReducersTest {
    private val today = LocalDate.of(2026, 3, 2)
    private val initial = TaskFormUiState(today = today).revalidated()
    private val morning = LocalTime.of(8, 0)
    private val evening = LocalTime.of(20, 0)

    private fun givenSeveralTimesTask() = initial.withName("Water filter").withRepeatType(RepeatType.TIMES_PER_DAY)

    @Test
    fun `a new form is every day that expires with no goal, no category and nothing to save`() {
        assertEquals(RepeatType.EVERY_DAY, initial.repeatType)
        assertEquals(OnMiss.EXPIRES, initial.onMiss)
        assertFalse(initial.goalOn)
        assertNull(initial.categoryId)
        assertFalse(initial.canSave)
    }

    @Test
    fun `a name makes the new form savable and a blank name is an error`() {
        assertTrue(initial.withName("Pills").canSave)

        val blank = initial.withName("   ")

        assertTrue(TaskFormError.BlankName in blank.errors)
        assertFalse(blank.canSave)
    }

    @Test
    fun `a one-time task defaults to staying and switching to every day makes it expire`() {
        val oneTime = initial.withRepeatType(RepeatType.ONE_TIME)
        assertEquals(OnMiss.STAYS, oneTime.onMiss)

        assertEquals(OnMiss.EXPIRES, oneTime.withRepeatType(RepeatType.EVERY_DAY).onMiss)
    }

    @Test
    fun `a one-time task cannot be saved before it has a due date`() {
        val oneTime = initial.withName("Electricity bill").withRepeatType(RepeatType.ONE_TIME)
        assertFalse(oneTime.canSave)

        assertTrue(oneTime.withDueDate(LocalDate.of(2026, 3, 20)).canSave)
    }

    @Test
    fun `a past due date counts from the due date and a future one from today`() {
        val oneTime = initial.withRepeatType(RepeatType.ONE_TIME)
        val pastDue = LocalDate.of(2026, 2, 20)

        assertEquals(pastDue, oneTime.withDueDate(pastDue).startDate(today))
        assertEquals(today, oneTime.withDueDate(LocalDate.of(2026, 3, 20)).startDate(today))
    }

    @Test
    fun `several times a day with no time is an error and blocks saving`() {
        val state = givenSeveralTimesTask()

        assertTrue(TaskFormError.NoTimes in state.errors)
        assertFalse(state.canSave)
    }

    @Test
    fun `added times are sorted and a repeated time is dropped`() {
        val state = givenSeveralTimesTask().withTimeAdded(evening).withTimeAdded(morning).withTimeAdded(evening)

        assertEquals(listOf(morning, evening), state.times)
    }

    @Test
    fun `a removed time leaves the others and several times a day has no time of its own`() {
        val state =
            givenSeveralTimesTask()
                .withTimeAdded(morning)
                .withTimeAdded(evening)
                .withTime(morning)
                .withTimeRemoved(morning)

        assertEquals(listOf(evening), state.times)
        assertNull(state.toDraft().time)
    }

    @Test
    fun `chosen weekdays with none chosen is an error until a day is toggled on`() {
        val state = initial.withName("Vacuum").withRepeatType(RepeatType.WEEKDAYS)
        assertTrue(TaskFormError.NoWeekdays in state.errors)

        val chosen = state.withWeekdayToggled(DayOfWeek.SATURDAY)
        assertTrue(chosen.canSave)
        assertTrue(chosen.withWeekdayToggled(DayOfWeek.SATURDAY).weekdays.isEmpty())
    }

    @Test
    fun `the interval stepper stays within one and a year`() {
        val state = initial.withRepeatType(RepeatType.EVERY_N_DAYS)

        assertEquals(MIN_INTERVAL_DAYS, state.withInterval(0).intervalDays)
        assertEquals(MAX_INTERVAL_DAYS, state.withInterval(MAX_INTERVAL_DAYS + 1).intervalDays)
    }

    @Test
    fun `the day of month stays within the days a month can have`() {
        val state = initial.withRepeatType(RepeatType.MONTHLY)

        assertEquals(MAX_DAY_OF_MONTH, state.withDayOfMonth(32).dayOfMonth)
    }

    @Test
    fun `turning the goal on starts at the smallest goal`() {
        val state = initial.withGoalOn(true)

        assertTrue(state.goalOn)
        assertEquals(MIN_TASK_GOAL, state.goal)
    }

    @Test
    fun `a goal of one is an error because one is a normal tick`() {
        val state = initial.withName("Send CVs").withGoalOn(true).withGoal(1)

        assertTrue(state.errors.any { it is TaskFormError.GoalOutOfRange })
        assertFalse(state.canSave)
    }

    @Test
    fun `changing the repeat type keeps the name, category, note and goal`() {
        val state =
            initial
                .withName("Vacuum")
                .withCategory("home")
                .withNote("living room")
                .withGoalOn(true)
                .withGoal(3)
                .withRepeatType(RepeatType.WEEKDAYS)

        assertEquals("Vacuum", state.name)
        assertEquals("home", state.categoryId)
        assertEquals("living room", state.note)
        assertEquals(3, state.goal)
    }

    @Test
    fun `changing the repeat type resets the fields of the old type`() {
        val state =
            initial
                .withRepeatType(RepeatType.WEEKDAYS)
                .withWeekdayToggled(DayOfWeek.MONDAY)
                .withRepeatType(RepeatType.MONTHLY)
                .withRepeatType(RepeatType.WEEKDAYS)

        assertTrue(state.weekdays.isEmpty())
    }

    @Test
    fun `staying is forced to expiring when switching to a type that hides it`() {
        val monthlyThatStays = initial.withRepeatType(RepeatType.MONTHLY).withOnMiss(OnMiss.STAYS)
        assertEquals(OnMiss.STAYS, monthlyThatStays.onMiss)

        val daily = monthlyThatStays.withRepeatType(RepeatType.EVERY_DAY)

        assertEquals(OnMiss.EXPIRES, daily.onMiss)
        assertEquals(OnMiss.EXPIRES, daily.toDraft().onMiss)
    }

    @Test
    fun `staying cannot be chosen while the type hides it`() {
        assertEquals(OnMiss.EXPIRES, initial.withOnMiss(OnMiss.STAYS).onMiss)
    }

    @Test
    fun `the goal is cleared when switching to several times a day and cannot be turned on there`() {
        val state = initial.withGoalOn(true).withRepeatType(RepeatType.TIMES_PER_DAY)
        assertFalse(state.goalOn)

        val again = state.withGoalOn(true)
        assertFalse(again.goalOn)
        assertNull(again.toDraft().goal)
    }

    @Test
    fun `the draft trims the note and stores an empty note as none`() {
        assertEquals("with water", initial.withNote("  with water ").toDraft().note)
        assertNull(initial.withNote("   ").toDraft().note)
    }
}
