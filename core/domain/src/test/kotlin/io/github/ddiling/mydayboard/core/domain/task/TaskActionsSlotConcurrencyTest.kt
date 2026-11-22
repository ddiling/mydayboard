package io.github.ddiling.mydayboard.core.domain.task

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

/** A double tap on one time chip must not both see the time as free (or as ticked). */
class TaskActionsSlotConcurrencyTest {
    private val fixture = TasksOverviewFixture()
    private val racing = TaskActions(fixture.tasks, YieldingTicks(fixture.ticks), fixture.clock)
    private val morning = LocalTime.of(8, 0)

    private suspend fun twiceADay() = fixture.addTask("Pills", Repeat.TimesPerDay(listOf(morning, LocalTime.of(20, 0))))

    @Test
    fun `two concurrent ticks of the same time store one tick and only one succeeds`() =
        runTest {
            val task = twiceADay()

            val outcomes =
                listOf(
                    async { racing.tickSlotToday(task.id, morning) },
                    async { racing.tickSlotToday(task.id, morning) },
                ).awaitAll()

            assertEquals(1, outcomes.count { it != null })
            assertEquals(
                1,
                fixture.ticks
                    .observeTicks(task.id)
                    .first()
                    .size,
            )
        }

    @Test
    fun `two concurrent unticks of the same time only succeed once`() =
        runTest {
            val task = twiceADay()
            racing.tickSlotToday(task.id, morning)

            val outcomes =
                listOf(
                    async { racing.untickSlotToday(task.id, morning) },
                    async { racing.untickSlotToday(task.id, morning) },
                ).awaitAll()

            assertEquals(1, outcomes.count { it != null })
        }
}
