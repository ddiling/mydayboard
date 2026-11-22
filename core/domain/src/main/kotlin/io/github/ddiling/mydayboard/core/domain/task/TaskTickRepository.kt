package io.github.ddiling.mydayboard.core.domain.task

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime

/**
 * Storage for task ticks (completions). Same contract as [TaskRepository]: no validation, ids and dates from the
 * injected `IdGenerator` and `Clock`, unknown or deleted ids are no-ops, flows apply `distinctUntilChanged()`.
 */
interface TaskTickRepository {
    /** Ticks of [taskId] ordered by `day`, `slot`, `id` ascending so the order is stable. */
    fun observeTicks(taskId: String): Flow<List<TaskTick>>

    /** Ticks of every live task, in the same order. */
    fun observeAllTicks(): Flow<List<TaskTick>>

    /** Adds a tick, or returns null when the task is unknown or deleted. */
    suspend fun addTick(
        taskId: String,
        day: LocalDate,
        slot: LocalTime?,
    ): TaskTick?

    suspend fun deleteTick(tickId: String)

    suspend fun restoreTick(tickId: String)

    /**
     * Makes [day] of [taskId] match [slots]: ticks for missing slots are added, ticks whose slot is not listed are
     * deleted, the rest stay. Returns what changed, so [undoDayChange] can revert it in one step.
     */
    suspend fun replaceDay(
        taskId: String,
        day: LocalDate,
        slots: List<LocalTime?>,
    ): DayChange

    /** Deletes the ticks [change] added and restores the ones it removed. */
    suspend fun undoDayChange(change: DayChange)
}
