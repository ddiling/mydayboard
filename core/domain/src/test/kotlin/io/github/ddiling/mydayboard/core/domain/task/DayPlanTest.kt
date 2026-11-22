package io.github.ddiling.mydayboard.core.domain.task

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class DayPlanTest {
    private val day = LocalDate.of(2025, 3, 12)
    private val morning = LocalTime.of(8, 0)
    private val evening = LocalTime.of(20, 0)
    private val early = Instant.parse("2025-03-12T06:00:00Z")
    private val late = Instant.parse("2025-03-12T09:00:00Z")

    private fun tick(
        id: String,
        slot: LocalTime? = null,
        createdAt: Instant = early,
    ) = TaskTick(id, "task", day, slot, createdAt, createdAt)

    @Test
    fun `nothing existing and nothing wanted plans nothing`() {
        assertEquals(DayPlan(emptyList(), emptyList()), planDay(emptyList(), emptyList()))
    }

    @Test
    fun `a timed slot named twice is added once`() {
        val plan = planDay(emptyList(), listOf(morning, morning))

        assertEquals(listOf<LocalTime?>(morning), plan.toAdd)
    }

    @Test
    fun `a count wanted above the existing ticks adds the difference`() {
        val plan = planDay(listOf(tick("a")), listOf(null, null, null))

        assertEquals(listOf<LocalTime?>(null, null), plan.toAdd)
        assertEquals(emptyList<TaskTick>(), plan.toRemove)
    }

    @Test
    fun `a count wanted below the existing ticks removes the newest by createdAt`() {
        val oldest = tick("z", createdAt = early)
        val newest = tick("a", createdAt = late)

        val plan = planDay(listOf(newest, oldest), listOf(null))

        assertEquals(listOf(newest), plan.toRemove)
        assertEquals(emptyList<LocalTime?>(), plan.toAdd)
    }

    @Test
    fun `ticks created at the same time are removed highest id first`() {
        val first = tick("a")
        val second = tick("b")

        val plan = planDay(listOf(second, first), listOf(null))

        assertEquals(listOf(second), plan.toRemove)
    }

    @Test
    fun `a timed slot with two live duplicates keeps only the oldest`() {
        val kept = tick("a", morning, early)
        val surplus = tick("b", morning, late)

        val plan = planDay(listOf(surplus, kept), listOf(morning))

        assertEquals(listOf(surplus), plan.toRemove)
        assertEquals(emptyList<LocalTime?>(), plan.toAdd)
    }

    @Test
    fun `a slot that is no longer wanted has all its ticks removed`() {
        val first = tick("a", evening)
        val second = tick("b", evening)

        val plan = planDay(listOf(first, second), emptyList())

        assertEquals(listOf(first, second), plan.toRemove)
    }

    @Test
    fun `timed and counted slots are planned independently`() {
        val existingMorning = tick("a", morning)
        val existingCount = tick("b")

        val plan = planDay(listOf(existingMorning, existingCount), listOf(morning, evening, null, null))

        assertEquals(emptyList<TaskTick>(), plan.toRemove)
        assertEquals(setOf<LocalTime?>(evening, null), plan.toAdd.toSet())
        assertEquals(2, plan.toAdd.size)
    }
}
