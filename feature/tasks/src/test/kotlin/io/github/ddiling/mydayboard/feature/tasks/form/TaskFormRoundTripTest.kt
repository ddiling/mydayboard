package io.github.ddiling.mydayboard.feature.tasks.form

import io.github.ddiling.mydayboard.core.domain.task.EveryNDaysMode
import io.github.ddiling.mydayboard.core.domain.task.OnMiss
import io.github.ddiling.mydayboard.core.domain.task.Repeat
import io.github.ddiling.mydayboard.core.domain.task.Task
import io.github.ddiling.mydayboard.core.domain.task.TaskDraft
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** Opening a task to edit and saving it unchanged must not alter it, whatever its repeat type. */
class TaskFormRoundTripTest {
    private val today = LocalDate.of(2026, 3, 2)
    private val reminder = LocalTime.of(9, 30)

    private fun givenTask(
        repeat: Repeat,
        time: LocalTime? = reminder,
        onMiss: OnMiss = OnMiss.STAYS,
    ) = Task(
        id = "task-1",
        name = "Water the plants",
        categoryId = "home",
        repeat = repeat,
        time = time,
        note = "balcony first",
        goal = 3,
        onMiss = onMiss,
        startDate = today,
        archivedAt = null,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    private fun draftOf(task: Task) =
        TaskDraft(
            name = task.name,
            categoryId = task.categoryId,
            repeat = task.repeat,
            time = task.time,
            note = task.note,
            goal = task.goal,
            onMiss = task.onMiss,
        )

    private fun assertRoundTrips(task: Task) {
        assertEquals(draftOf(task), task.toFormState(today).toDraft())
    }

    @Test
    fun `an every day task round-trips`() {
        assertRoundTrips(givenTask(Repeat.EveryDay, onMiss = OnMiss.EXPIRES))
    }

    @Test
    fun `a several times a day task round-trips`() {
        val times = listOf(LocalTime.of(8, 0), LocalTime.of(20, 0))

        assertRoundTrips(givenTask(Repeat.TimesPerDay(times), time = null, onMiss = OnMiss.EXPIRES).copy(goal = null))
    }

    @Test
    fun `a weekdays task round-trips`() {
        assertRoundTrips(givenTask(Repeat.Weekdays(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))))
    }

    @Test
    fun `an every N days task round-trips`() {
        val repeat = Repeat.EveryNDays(5, EveryNDaysMode.FROM_LAST_DONE, today.minusDays(1))

        assertRoundTrips(givenTask(repeat, time = null))
    }

    @Test
    fun `a monthly task round-trips`() {
        assertRoundTrips(givenTask(Repeat.Monthly(15)))
    }

    @Test
    fun `a one-time task round-trips`() {
        assertRoundTrips(givenTask(Repeat.OneTime(today.plusDays(10))))
    }
}
