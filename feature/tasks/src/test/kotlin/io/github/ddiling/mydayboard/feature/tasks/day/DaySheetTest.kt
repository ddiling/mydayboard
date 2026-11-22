package io.github.ddiling.mydayboard.feature.tasks.day

import io.github.ddiling.mydayboard.core.domain.task.Repeat
import io.github.ddiling.mydayboard.core.domain.task.Task
import io.github.ddiling.mydayboard.core.domain.task.TaskActions
import io.github.ddiling.mydayboard.feature.tasks.detail.TaskDetailFixture
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class DaySheetTest {
    private val fixture = TaskDetailFixture()
    private val sheet = DaySheet(TaskActions(fixture.tasks, fixture.ticks, fixture.clock), fixture.clock)
    private val started = fixture.today.minusDays(10)
    private val pastDay = fixture.today.minusDays(3)
    private val morning = LocalTime.of(8, 0)
    private val evening = LocalTime.of(20, 0)

    private suspend fun open(
        task: Task,
        day: LocalDate = pastDay,
    ) = sheet.open(task, fixture.ticks.observeTicks(task.id).first(), day)

    private suspend fun tickedSlots(
        task: Task,
        day: LocalDate = pastDay,
    ) = fixture.ticks
        .observeTicks(task.id)
        .first()
        .filter { it.day == day }
        .map { it.slot }

    @Test
    fun `opening a day with no tick shows Not done selected`() =
        runTest {
            val task = fixture.addTask("Vacuum", startDate = started)

            open(task)

            assertEquals(DaySheetKind.Single(done = false), sheet.state.value?.kind)
        }

    @Test
    fun `choosing Done and Save marks the day and offers Undo`() =
        runTest {
            val task = fixture.addTask("Vacuum", startDate = started)
            open(task)

            sheet.setSingle(done = true)
            val marked = sheet.save()

            assertEquals(listOf<LocalTime?>(null), tickedSlots(task))
            assertEquals(pastDay, marked?.outcome?.day)
            assertNull(sheet.state.value)
        }

    @Test
    fun `a several-times day opens with the ticked times and toggling changes only the draft`() =
        runTest {
            val task = fixture.addTask("Pills", Repeat.TimesPerDay(listOf(morning, evening)), startDate = started)
            fixture.tickOn(task, pastDay, morning)
            open(task)

            sheet.toggle(evening)

            val kind = sheet.state.value?.kind
            assertEquals(DaySheetKind.Times(listOf(morning, evening), setOf(morning, evening)), kind)
            assertEquals(listOf<LocalTime?>(morning), tickedSlots(task))
        }

    @Test
    fun `toggling a ticked time unticks it`() =
        runTest {
            val task = fixture.addTask("Pills", Repeat.TimesPerDay(listOf(morning, evening)), startDate = started)
            fixture.tickOn(task, pastDay, morning)
            open(task)

            sheet.toggle(morning)

            assertEquals(emptySet<LocalTime>(), (sheet.state.value?.kind as DaySheetKind.Times).chosen)
        }

    @Test
    fun `Save for times marks exactly the chosen slots`() =
        runTest {
            val task = fixture.addTask("Pills", Repeat.TimesPerDay(listOf(morning, evening)), startDate = started)
            fixture.tickOn(task, pastDay, morning)
            open(task)
            sheet.toggle(morning)
            sheet.toggle(evening)

            sheet.save()

            assertEquals(listOf<LocalTime?>(evening), tickedSlots(task))
        }

    @Test
    fun `the count stepper stays between 0 and the goal`() =
        runTest {
            val task = fixture.addTask("Send CVs", goal = 3, startDate = started)
            repeat(2) { fixture.tickOn(task, pastDay) }
            open(task)

            repeat(5) { sheet.step(+1) }
            assertEquals(DaySheetKind.Count(target = 3, value = 3), sheet.state.value?.kind)

            repeat(5) { sheet.step(-1) }
            assertEquals(DaySheetKind.Count(target = 3, value = 0), sheet.state.value?.kind)
        }

    @Test
    fun `Save for a count writes that many ticks`() =
        runTest {
            val task = fixture.addTask("Send CVs", goal = 3, startDate = started)
            open(task)
            repeat(2) { sheet.step(+1) }

            sheet.save()

            assertEquals(listOf<LocalTime?>(null, null), tickedSlots(task))
        }

    @Test
    fun `dismiss saves nothing`() =
        runTest {
            val task = fixture.addTask("Vacuum", startDate = started)
            open(task)
            sheet.setSingle(done = true)

            sheet.dismiss()

            assertNull(sheet.state.value)
            assertEquals(emptyList<LocalTime?>(), tickedSlots(task))
            assertNull(sheet.save())
        }

    @Test
    fun `today and the future cannot be opened`() =
        runTest {
            val task = fixture.addTask("Vacuum", startDate = started)

            assertFalse(open(task, fixture.today))
            assertFalse(open(task, fixture.today.plusDays(1)))
            assertNull(sheet.state.value)
        }

    @Test
    fun `the day before the start date cannot be opened`() =
        runTest {
            val task = fixture.addTask("Vacuum", startDate = pastDay)

            assertFalse(open(task, pastDay.minusDays(1)))
            assertTrue(open(task, pastDay))
            assertNotNull(sheet.state.value)
        }

    @Test
    fun `a day that is refused at Save closes the sheet and offers nothing`() =
        runTest {
            val task = fixture.addTask("Vacuum", startDate = started)
            open(task)
            fixture.tasks.archiveTask(task.id)

            assertNull(sheet.save())
            assertNull(sheet.state.value)
        }
}
