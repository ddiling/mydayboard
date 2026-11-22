package io.github.ddiling.mydayboard.core.domain.task

import io.github.ddiling.mydayboard.core.testing.TestZones
import io.github.ddiling.mydayboard.core.testing.warsaw
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class TaskActionsTest {
    private val fixture = TasksOverviewFixture()
    private val actions = TaskActions(fixture.tasks, fixture.ticks, fixture.clock)
    private val today = fixture.today
    private val yesterday = today.minusDays(1)
    private val morning = LocalTime.of(8, 0)
    private val evening = LocalTime.of(20, 0)

    private suspend fun ticksOf(task: Task) = fixture.ticks.observeTicks(task.id).first()

    private suspend fun goalTask(goal: Int = 3) = fixture.addTask("Water filter") { copy(goal = goal) }

    private suspend fun twiceADay() = fixture.addTask("Pills", Repeat.TimesPerDay(listOf(morning, evening)))

    @Test
    fun `swipe on a plain task adds one tick for today`() =
        runTest {
            val task = fixture.addTask("Vacuum")

            val outcome = actions.swipeDone(task.id)

            assertEquals(1, outcome?.done)
            assertEquals(1, outcome?.target)
            assertEquals(listOf(today), ticksOf(task).map { it.day })
        }

    @Test
    fun `swipe on a done plain task does nothing`() =
        runTest {
            val task = fixture.addTask("Vacuum")
            actions.swipeDone(task.id)

            assertNull(actions.swipeDone(task.id))
            assertEquals(1, ticksOf(task).size)
        }

    @Test
    fun `swipe on a count goal adds one and reports the progress`() =
        runTest {
            val task = goalTask()
            actions.swipeDone(task.id)

            val outcome = actions.swipeDone(task.id)

            assertEquals(2, outcome?.done)
            assertEquals(3, outcome?.target)
        }

    @Test
    fun `swipe at the goal returns null`() =
        runTest {
            val task = goalTask(goal = 2)
            actions.swipeDone(task.id)
            actions.swipeDone(task.id)

            assertNull(actions.swipeDone(task.id))
            assertEquals(2, ticksOf(task).size)
        }

    @Test
    fun `swipe on a several-times task ticks the next undone time`() =
        runTest {
            val task = twiceADay()
            fixture.ticks.addTick(task.id, today, morning)

            val outcome = actions.swipeDone(task.id)

            assertEquals(evening, outcome?.slot)
            assertEquals(2, outcome?.done)
        }

    @Test
    fun `swipe on a several-times task with every time done returns null`() =
        runTest {
            val task = twiceADay()
            fixture.ticks.addTick(task.id, today, morning)
            fixture.ticks.addTick(task.id, today, evening)

            assertNull(actions.swipeDone(task.id))
            assertEquals(2, ticksOf(task).size)
        }

    @Test
    fun `swipe on a late stays task ticks today and the occurrence becomes done`() =
        runTest {
            val task =
                fixture.addTask(
                    "Bills",
                    Repeat.Weekdays(setOf(yesterday.dayOfWeek)),
                    startDate = yesterday.minusDays(7),
                ) {
                    copy(onMiss = OnMiss.STAYS)
                }
            assertEquals(RowState.LATE, fixture.rowOf(task).state)

            actions.swipeDone(task.id)

            assertEquals(listOf(today), ticksOf(task).map { it.day })
            assertEquals(OccurrenceStatus.DONE, occurrencesOf(task, ticksOf(task), today).last().status)
        }

    @Test
    fun `swipe on a task not due today returns null and stores nothing`() =
        runTest {
            val task = fixture.addTask("Vacuum", Repeat.Weekdays(setOf(today.plusDays(1).dayOfWeek)))

            assertNull(actions.swipeDone(task.id))
            assertTrue(ticksOf(task).isEmpty())
        }

    @Test
    fun `swipe on an archived task returns null`() =
        runTest {
            val task = fixture.addTask("Vacuum")
            fixture.tasks.archiveTask(task.id)

            assertNull(actions.swipeDone(task.id))
            assertTrue(ticksOf(task).isEmpty())
        }

    @Test
    fun `swipe on a deleted task returns null`() =
        runTest {
            val task = fixture.addTask("Vacuum")
            fixture.tasks.deleteTask(task.id)

            assertNull(actions.swipeDone(task.id))
        }

    @Test
    fun `untick removes the only tick of today`() =
        runTest {
            val task = fixture.addTask("Vacuum")
            actions.swipeDone(task.id)

            val outcome = actions.untickToday(task.id)

            assertEquals(0, outcome?.done)
            assertTrue(ticksOf(task).isEmpty())
        }

    @Test
    fun `untick on a goal task removes one tick`() =
        runTest {
            val task = goalTask()
            repeat(3) { actions.swipeDone(task.id) }

            val outcome = actions.untickToday(task.id)

            assertEquals(2, outcome?.done)
            assertEquals(3, outcome?.target)
            assertEquals(2, ticksOf(task).size)
        }

    @Test
    fun `untick with nothing ticked returns null`() =
        runTest {
            val task = fixture.addTask("Vacuum")

            assertNull(actions.untickToday(task.id))
        }

    @Test
    fun `undo of an added tick removes it`() =
        runTest {
            val task = fixture.addTask("Vacuum")
            val added = checkNotNull(actions.swipeDone(task.id))

            actions.undo(added)

            assertTrue(ticksOf(task).isEmpty())
        }

    @Test
    fun `undo of a removed tick restores it`() =
        runTest {
            val task = fixture.addTask("Vacuum")
            actions.swipeDone(task.id)
            val removed = checkNotNull(actions.untickToday(task.id))

            actions.undo(removed)

            assertEquals(listOf(removed.tickId), ticksOf(task).map { it.id })
        }

    @Test
    fun `markDay for yesterday adds the tick for that day`() =
        runTest {
            val task = fixture.addTask("Vacuum", startDate = yesterday.minusDays(3))

            val outcome = actions.markDay(task.id, yesterday, listOf(null))

            assertEquals(yesterday, outcome?.day)
            assertEquals(listOf(yesterday), ticksOf(task).map { it.day })
        }

    @Test
    fun `markDay makes the day match the given slots`() =
        runTest {
            val task = fixture.addTask("Pills", Repeat.TimesPerDay(listOf(morning, evening)), startDate = yesterday)
            actions.markDay(task.id, yesterday, listOf(morning, evening))

            actions.markDay(task.id, yesterday, listOf(morning))

            assertEquals(listOf(morning), ticksOf(task).map { it.slot })
        }

    @Test
    fun `markDay for today or the future returns null and stores nothing`() =
        runTest {
            val task = fixture.addTask("Vacuum", startDate = yesterday)

            assertNull(actions.markDay(task.id, today, listOf(null)))
            assertNull(actions.markDay(task.id, today.plusDays(1), listOf(null)))
            assertTrue(ticksOf(task).isEmpty())
        }

    @Test
    fun `markDay before the start date returns null`() =
        runTest {
            val task = fixture.addTask("Vacuum", startDate = yesterday)

            assertNull(actions.markDay(task.id, yesterday.minusDays(1), listOf(null)))
            assertTrue(ticksOf(task).isEmpty())
        }

    @Test
    fun `undo of a replaced day restores the previous state`() =
        runTest {
            val task = fixture.addTask("Pills", Repeat.TimesPerDay(listOf(morning, evening)), startDate = yesterday)
            actions.markDay(task.id, yesterday, listOf(morning, evening))
            val before = ticksOf(task).map { it.id }
            val change = checkNotNull(actions.markDay(task.id, yesterday, listOf(morning)))

            actions.undo(change)

            assertEquals(before, ticksOf(task).map { it.id })
        }

    @Test
    fun `stats change at once after markDay`() =
        runTest {
            val task = fixture.addTask("Vacuum", startDate = today.minusDays(3))
            val before = computeTaskStats(task, ticksOf(task), today).streak

            actions.markDay(task.id, yesterday, listOf(null))

            assertEquals(before + 1, computeTaskStats(task, ticksOf(task), today).streak)
        }

    @Test
    fun `swipe just before midnight ticks that day and just after ticks the next`() =
        runTest {
            val task = fixture.addTask("Vacuum")
            fixture.clock.now = warsaw(2026, 11, 18, 23, 59).plusMillis(59_999)
            actions.swipeDone(task.id)

            fixture.clock.advanceBy(Duration.ofMillis(1))
            actions.swipeDone(task.id)

            assertEquals(listOf(today, today.plusDays(1)), ticksOf(task).map { it.day })
        }

    @Test
    fun `at ten past midnight yesterday can be marked`() =
        runTest {
            val task = fixture.addTask("Dinner", startDate = yesterday)
            fixture.clock.now = warsaw(2026, 11, 19, 0, 10)

            val outcome = actions.markDay(task.id, today, listOf(null))

            assertEquals(today, outcome?.day)
            assertEquals(listOf(today), ticksOf(task).map { it.day })
        }

    @Test
    fun `swipes at both ends of the spring-forward day tick that date once each`() =
        runTest {
            val springDay = LocalDate.of(2026, 3, 29)
            assertSwipesLandOnOneDate(springDay, warsaw(2026, 3, 29, 0, 30), warsaw(2026, 3, 29, 23, 30))
        }

    @Test
    fun `swipes at both ends of the fall-back day tick that date once each`() =
        runTest {
            val fallDay = LocalDate.of(2026, 10, 25)
            assertSwipesLandOnOneDate(fallDay, warsaw(2026, 10, 25, 0, 30), warsaw(2026, 10, 25, 23, 30))
        }

    @Test
    fun `after a zone change the same instant ticks the new local day`() =
        runTest {
            val task = fixture.addTask("Vacuum")
            fixture.clock.now = warsaw(2026, 11, 18, 23, 30)
            fixture.clock.zone = TestZones.TOKYO

            actions.swipeDone(task.id)

            assertEquals(listOf(today.plusDays(1)), ticksOf(task).map { it.day })
        }

    private suspend fun assertSwipesLandOnOneDate(
        day: LocalDate,
        early: Instant,
        late: Instant,
    ) {
        val task = fixture.addTask("Pills", Repeat.TimesPerDay(listOf(morning, evening)), startDate = day)
        fixture.clock.now = early
        actions.swipeDone(task.id)
        fixture.clock.now = late
        actions.swipeDone(task.id)

        assertEquals(listOf(day, day), ticksOf(task).map { it.day })
    }
}
