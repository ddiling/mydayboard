package io.github.ddiling.mydayboard.core.testing

import io.github.ddiling.mydayboard.core.domain.id.IdGenerator
import io.github.ddiling.mydayboard.core.domain.task.DayChange
import io.github.ddiling.mydayboard.core.domain.task.TaskTick
import io.github.ddiling.mydayboard.core.domain.task.TaskTickRepository
import io.github.ddiling.mydayboard.core.domain.task.planDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * In-memory [TaskTickRepository] for ViewModel tests. It passes the same contract tests as the Room
 * implementation. Pass the [FakeTaskStore] of the [FakeTaskRepository] to use the same tasks.
 */
class FakeTaskTickRepository(
    private val clock: Clock,
    private val ids: IdGenerator,
    store: FakeTaskStore = FakeTaskStore(),
) : TaskTickRepository {
    private val state = store.state

    private val tickOrder = compareBy<TaskTick> { it.day }.thenBy(nullsFirst()) { it.slot }.thenBy { it.id }

    override fun observeTicks(taskId: String): Flow<List<TaskTick>> =
        state.map { s -> s.liveTicks().filter { it.taskId == taskId } }.distinctUntilChanged()

    override fun observeAllTicks(): Flow<List<TaskTick>> = state.map { it.liveTicks() }.distinctUntilChanged()

    override suspend fun addTick(
        taskId: String,
        day: LocalDate,
        slot: LocalTime?,
    ): TaskTick? {
        return insert(taskId, day, slot, clock.instant())
    }

    override suspend fun deleteTick(tickId: String) {
        removeTick(tickId, clock.instant())
    }

    override suspend fun restoreTick(tickId: String) {
        val now = clock.instant()
        state.update { s -> s.withRestored(tickId, now) }
    }

    override suspend fun replaceDay(
        taskId: String,
        day: LocalDate,
        slots: List<LocalTime?>,
    ): DayChange {
        val now = clock.instant()
        val live = state.value.liveTicks().filter { it.taskId == taskId && it.day == day }
        val plan = planDay(live, slots)
        plan.toRemove.forEach { removeTick(it.id, now) }
        val added = plan.toAdd.mapNotNull { insert(taskId, day, it, now)?.id }
        return DayChange(addedIds = added, removedIds = plan.toRemove.map { it.id })
    }

    override suspend fun undoDayChange(change: DayChange) {
        val now = clock.instant()
        change.addedIds.forEach { removeTick(it, now) }
        state.update { s -> change.removedIds.fold(s) { acc, id -> acc.withRestored(id, now) } }
    }

    private fun insert(
        taskId: String,
        day: LocalDate,
        slot: LocalTime?,
        now: Instant,
    ): TaskTick? {
        val s = state.value
        val existing = s.ticks.firstOrNull { it.taskId == taskId && it.day == day && it.slot == slot }
        return when {
            s.tasks.none { it.id == taskId } -> {
                null
            }

            slot != null && existing != null -> {
                existing
            }

            else -> {
                TaskTick(ids.newId(), taskId, day, slot, now, now).also { tick ->
                    state.update {
                        it.copy(
                            ticks =
                                it.ticks + tick,
                        )
                    }
                }
            }
        }
    }

    private fun removeTick(
        id: String,
        now: Instant,
    ) = state.update { s ->
        val tick = s.liveTicks().firstOrNull { it.id == id } ?: return@update s
        s.copy(ticks = s.ticks - tick, deletedTicks = s.deletedTicks + tick.copy(updatedAt = now))
    }

    private fun FakeTaskState.withRestored(
        id: String,
        now: Instant,
    ): FakeTaskState {
        val tick = deletedTicks.firstOrNull { it.id == id }
        if (tick == null || tasks.none { it.id == tick.taskId }) return this
        return copy(ticks = ticks + tick.copy(updatedAt = now), deletedTicks = deletedTicks - tick)
    }

    private fun FakeTaskState.liveTicks(): List<TaskTick> =
        ticks.filter { tick -> tasks.any { it.id == tick.taskId } }.sortedWith(tickOrder)
}
