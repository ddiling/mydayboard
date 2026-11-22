package io.github.ddiling.mydayboard.core.domain.task

import io.github.ddiling.mydayboard.core.testing.TestZones
import io.github.ddiling.mydayboard.core.testing.warsaw
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** "Today" for the time chips comes from the clock at every call, in the clock's zone. */
class TaskActionsSlotTimeTest {
    private val fixture = TasksOverviewFixture()
    private val actions = TaskActions(fixture.tasks, fixture.ticks, fixture.clock)
    private val today = fixture.today
    private val morning = LocalTime.of(8, 0)
    private val evening = LocalTime.of(20, 0)
    private val lastMillisecondOfToday = warsaw(2026, 11, 18, 23, 59).plusMillis(59_999)

    private suspend fun twiceADay(startDate: LocalDate = today) =
        fixture.addTask("Pills", Repeat.TimesPerDay(listOf(morning, evening)), startDate = startDate)

    private suspend fun daysTicked(task: Task) =
        fixture.ticks
            .observeTicks(task.id)
            .first()
            .map { it.day }

    @Test
    fun `a time ticked just before midnight counts for the old day and just after it for the new day`() =
        runTest {
            val task = twiceADay()
            fixture.clock.now = lastMillisecondOfToday
            actions.tickSlotToday(task.id, morning)

            fixture.clock.advanceBy(Duration.ofMillis(1))
            actions.tickSlotToday(task.id, morning)

            assertEquals(listOf(today, today.plusDays(1)), daysTicked(task))
        }

    @Test
    fun `at midnight unticking a time does not remove the tick of the day that just ended`() =
        runTest {
            val task = twiceADay()
            fixture.clock.now = lastMillisecondOfToday
            actions.tickSlotToday(task.id, morning)

            fixture.clock.advanceBy(Duration.ofMillis(1))

            assertNull(actions.untickSlotToday(task.id, morning))
            assertEquals(listOf(today), daysTicked(task))
        }

    @Test
    fun `times ticked at both ends of the spring-forward day land on that date`() =
        runTest {
            assertTicksLandOnOneDate(
                LocalDate.of(2026, 3, 29),
                warsaw(2026, 3, 29, 0, 30),
                warsaw(2026, 3, 29, 23, 30),
            )
        }

    @Test
    fun `times ticked at both ends of the fall-back day land on that date`() =
        runTest {
            assertTicksLandOnOneDate(
                LocalDate.of(2026, 10, 25),
                warsaw(2026, 10, 25, 0, 30),
                warsaw(2026, 10, 25, 23, 30),
            )
        }

    @Test
    fun `after a zone change the same instant ticks a time on the new local day`() =
        runTest {
            val task = twiceADay()
            fixture.clock.now = warsaw(2026, 11, 18, 23, 30)
            fixture.clock.zone = TestZones.TOKYO

            actions.tickSlotToday(task.id, morning)

            assertEquals(listOf(today.plusDays(1)), daysTicked(task))
        }

    @Test
    fun `after a zone change the same instant unticks only the new local day`() =
        runTest {
            val task = twiceADay()
            fixture.clock.now = warsaw(2026, 11, 18, 23, 30)
            actions.tickSlotToday(task.id, morning)
            fixture.clock.zone = TestZones.TOKYO

            assertNull(actions.untickSlotToday(task.id, morning))
            assertEquals(listOf(today), daysTicked(task))
        }

    private suspend fun assertTicksLandOnOneDate(
        day: LocalDate,
        early: Instant,
        late: Instant,
    ) {
        val task = twiceADay(startDate = day)
        fixture.clock.now = early
        actions.tickSlotToday(task.id, morning)
        fixture.clock.now = late
        actions.tickSlotToday(task.id, evening)

        assertEquals(listOf(day, day), daysTicked(task))
    }
}
