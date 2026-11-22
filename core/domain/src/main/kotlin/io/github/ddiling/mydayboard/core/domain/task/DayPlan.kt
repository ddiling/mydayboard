package io.github.ddiling.mydayboard.core.domain.task

import java.time.LocalTime

/** What [planDay] decided: [toRemove] are existing ticks to delete, [toAdd] the slots that need a new tick. */
data class DayPlan(
    val toRemove: List<TaskTick>,
    val toAdd: List<LocalTime?>,
)

/**
 * Compares the live ticks of one day with the wanted [slots]. Both repositories use it, so they cannot drift apart.
 * A timed slot is a yes/no mark, so it never has two ticks, even if the list names it twice. A tick without a slot
 * is a count, so its wanted number is how many nulls the list has. When ticks must go, the newest go first, so an
 * old completion keeps its original time.
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
