package io.github.ddiling.mydayboard.core.testing

import io.github.ddiling.mydayboard.core.domain.task.DayChange
import io.github.ddiling.mydayboard.core.domain.task.TaskTick
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

/** Contract for replacing the ticks of one day and undoing that. */
abstract class TaskTickDayContractTest : TaskTickRepositoryContractBase() {
    @Test
    fun `replacing a day adds the missing slots`() =
        runTest {
            val task = givenTask()

            val change = ticks.replaceDay(task.id, day, listOf(morning, evening))

            assertEquals(listOf(morning, evening), liveTicksOf(task.id).map { it.slot })
            assertEquals(liveTicksOf(task.id).map { it.id }, change.addedIds)
        }

    @Test
    fun `replacing a day removes slots that are no longer wanted`() =
        runTest {
            val task = givenTask()
            givenTick(task.id, slot = morning)
            val dropped = givenTick(task.id, slot = evening)

            val change = ticks.replaceDay(task.id, day, listOf(morning))

            assertEquals(listOf(morning), liveTicksOf(task.id).map { it.slot })
            assertEquals(DayChange(addedIds = emptyList(), removedIds = listOf(dropped.id)), change)
        }

    @Test
    fun `replacing a day with the same slots changes nothing and returns an empty change`() =
        runTest {
            val task = givenTask()
            val kept = listOf(givenTick(task.id, slot = morning), givenTick(task.id, slot = evening))

            val change = ticks.replaceDay(task.id, day, listOf(morning, evening))

            assertEquals(kept, liveTicksOf(task.id))
            assertEquals(DayChange(emptyList(), emptyList()), change)
        }

    @Test
    fun `replacing a day twice with a slot that has seconds is stable`() =
        runTest {
            val task = givenTask()
            val withSeconds = listOf(LocalTime.of(8, 0, 30))
            ticks.replaceDay(task.id, day, withSeconds)

            val change = ticks.replaceDay(task.id, day, withSeconds)

            assertEquals(listOf(morning), liveTicksOf(task.id).map { it.slot })
            assertEquals(DayChange(emptyList(), emptyList()), change)
        }

    @Test
    fun `replacing a day with a count of three and no slot adds three ticks`() =
        runTest {
            val task = givenTask()
            val countGoal = 3

            val change = ticks.replaceDay(task.id, day, List(countGoal) { null })

            assertEquals(countGoal, liveTicksOf(task.id).size)
            assertEquals(countGoal, change.addedIds.size)
        }

    @Test
    fun `lowering a count from three to one removes the two newest ticks`() =
        runTest {
            val task = givenTask()
            val oldest = givenTick(task.id)
            clock.advanceBy(clockAdvance)
            val middle = givenTick(task.id)
            clock.advanceBy(clockAdvance)
            val newest = givenTick(task.id)

            val change = ticks.replaceDay(task.id, day, listOf(null))

            assertEquals(listOf(oldest), liveTicksOf(task.id))
            assertEquals(setOf(middle.id, newest.id), change.removedIds.toSet())
        }

    @Test
    fun `replacing a day does not touch other days`() =
        runTest {
            val task = givenTask()
            val other = givenTick(task.id, otherDay, morning)

            ticks.replaceDay(task.id, day, emptyList())

            assertEquals(listOf(other), liveTicksOf(task.id))
        }

    @Test
    fun `replacing a day of a deleted task changes nothing`() =
        runTest {
            val task = givenTask()
            givenTick(task.id, slot = morning)
            tasks.deleteTask(task.id)

            val change = ticks.replaceDay(task.id, day, listOf(evening))

            assertEquals(DayChange(emptyList(), emptyList()), change)
        }

    @Test
    fun `undoing an add removes the added ticks`() =
        runTest {
            val task = givenTask()
            val change = ticks.replaceDay(task.id, day, listOf(morning, evening))

            ticks.undoDayChange(change)

            assertEquals(emptyList<TaskTick>(), liveTicksOf(task.id))
        }

    @Test
    fun `undoing a remove brings the removed ticks back`() =
        runTest {
            val task = givenTask()
            val tick = givenTick(task.id, slot = morning)
            val change = ticks.replaceDay(task.id, day, emptyList())
            clock.advanceBy(clockAdvance)

            ticks.undoDayChange(change)

            assertEquals(listOf(tick.copy(updatedAt = nowAfterAdvance)), liveTicksOf(task.id))
        }
}
