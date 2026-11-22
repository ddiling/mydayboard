package io.github.ddiling.mydayboard.core.domain.task

import java.time.LocalTime

/** What [planDay] decided: [toRemove] are existing ticks to delete, [toAdd] the slots that need a new tick. */
data class DayPlan(
    val toRemove: List<TaskTick>,
    val toAdd: List<LocalTime?>,
)

/**
 * Compares the live ticks of one day with the wanted [slots]; both repositories use it so they cannot drift. A
 * timed slot is a yes/no mark with one tick, even if named twice. A tick without a slot is a count: as many as
 * there are nulls. Surplus ticks go newest first, so an old completion keeps its original time.
 */
fun planDay(
    existing: List<TaskTick>,
    slots: List<LocalTime?>,
): DayPlan {
    val wanted = slots.groupingBy { it }.eachCount().mapValues { (slot, count) -> if (slot == null) count else 1 }
    val have = existing.sortedWith(compareBy<TaskTick> { it.createdAt }.thenBy { it.id }).groupBy { it.slot }
    val toRemove = have.flatMap { (slot, ticks) -> ticks.drop(wanted[slot] ?: 0) }
    val toAdd = wanted.flatMap { (slot, count) -> List((count - (have[slot]?.size ?: 0)).coerceAtLeast(0)) { slot } }
    return DayPlan(toRemove, toAdd)
}
