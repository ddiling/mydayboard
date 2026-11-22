package io.github.ddiling.mydayboard.feature.tasks.detail

import io.github.ddiling.mydayboard.core.domain.task.OnMiss
import io.github.ddiling.mydayboard.core.domain.task.Repeat
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class TaskDetailHeroTest {
    private val fixture = TaskDetailFixture()
    private val morning = LocalTime.of(8, 0)
    private val evening = LocalTime.of(20, 0)

    @Test
    fun `a task nobody ticked yet says not done yet`() =
        runTest {
            val task = fixture.addTask("Pills")

            assertEquals(Hero.NotDone, fixture.content(task).hero)
        }

    @Test
    fun `a task ticked today says done today`() =
        runTest {
            val task = fixture.addTask("Pills")
            fixture.tickOn(task, fixture.today)

            assertEquals(Hero.DoneToday, fixture.content(task).hero)
        }

    @Test
    fun `a several-times task with one time ticked says one of two`() =
        runTest {
            val task = fixture.addTask("Pills", Repeat.TimesPerDay(listOf(morning, evening)))
            fixture.tickOn(task, fixture.today, morning)

            assertEquals(Hero.TimesProgress(1, 2), fixture.content(task).hero)
        }

    @Test
    fun `a count goal with two of three ticked says 2 of 3`() =
        runTest {
            val task = fixture.addTask("Send CVs", goal = 3)
            repeat(2) { fixture.tickOn(task, fixture.today) }

            assertEquals(Hero.CountProgress(2, 3), fixture.content(task).hero)
        }

    @Test
    fun `a late task says since when and still offers Mark done`() =
        runTest {
            val friday = LocalDate.of(2026, 11, 13)
            val task = fixture.addTask("Electricity bill", Repeat.OneTime(friday), onMiss = OnMiss.STAYS)

            val content = fixture.content(task)

            assertEquals(Hero.LateSince(friday), content.hero)
            assertEquals(BigButton.MarkDone, content.bigButton)
        }

    @Test
    fun `a task not due today has no big button and says when it is next due`() =
        runTest {
            val task = fixture.addTask("Vacuum", Repeat.Weekdays(setOf(DayOfWeek.FRIDAY)))

            val content = fixture.content(task)

            assertEquals(Hero.NotDueToday, content.hero)
            assertEquals(BigButton.None, content.bigButton)
            assertEquals(LocalDate.of(2026, 11, 20), content.nextDue)
        }

    @Test
    fun `a finished one-time task says done`() =
        runTest {
            val yesterday = fixture.today.minusDays(1)
            val task = fixture.addTask("Send CVs", Repeat.OneTime(yesterday), startDate = yesterday)
            fixture.tickOn(task, yesterday)

            val content = fixture.content(task)

            assertEquals(Hero.Finished(reached = true), content.hero)
            assertNull(content.nextDue)
        }

    @Test
    fun `an expired one-time task nobody did says missed`() =
        runTest {
            val yesterday = fixture.today.minusDays(1)
            val task = fixture.addTask("Send CVs", Repeat.OneTime(yesterday), startDate = yesterday)

            assertEquals(Hero.Finished(reached = false), fixture.content(task).hero)
        }
}
