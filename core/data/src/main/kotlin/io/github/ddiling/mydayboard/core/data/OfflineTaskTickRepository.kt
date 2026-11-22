package io.github.ddiling.mydayboard.core.data

import io.github.ddiling.mydayboard.core.database.dao.TaskTickDao
import io.github.ddiling.mydayboard.core.domain.id.IdGenerator
import io.github.ddiling.mydayboard.core.domain.task.DayChange
import io.github.ddiling.mydayboard.core.domain.task.TaskTick
import io.github.ddiling.mydayboard.core.domain.task.TaskTickRepository
import io.github.ddiling.mydayboard.core.domain.task.planDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

private const val NOT_INSERTED = -1L

/**
 * [TaskTickRepository] on Room. Every write is one conditional DAO statement, so unknown or deleted ids are
 * no-ops. [replaceDay] is not one transaction: each statement is safe alone, so a crash between two leaves a
 * partly changed day, which the user sees and can fix.
 *
 * A timed slot never gets a second live tick, because the database would accept the duplicate: adding one
 * returns the existing tick, and the check and the insert are one statement, so parallel calls agree. Slots
 * are kept to whole minutes, as stored. Ticks without a slot are a count, so they may repeat.
 */
internal class OfflineTaskTickRepository
    @Inject
    constructor(
        private val dao: TaskTickDao,
        private val clock: Clock,
        private val ids: IdGenerator,
    ) : TaskTickRepository {
        override fun observeTicks(taskId: String): Flow<List<TaskTick>> =
            dao.observeForTask(taskId).map { rows -> rows.map { it.toDomain() } }.distinctUntilChanged()

        override fun observeAllTicks(): Flow<List<TaskTick>> =
            dao.observeAllLive().map { rows -> rows.map { it.toDomain() } }.distinctUntilChanged()

        override suspend fun addTick(
            taskId: String,
            day: LocalDate,
            slot: LocalTime?,
        ): TaskTick? {
            // Room stores epoch millis, so the returned tick must not carry finer precision than what is read back.
            val now = clock.instant().truncatedTo(ChronoUnit.MILLIS).toEpochMilli()
            return insert(taskId, day, slot?.toWholeMinute(), now)
        }

        override suspend fun deleteTick(tickId: String) {
            dao.softDeleteIfLive(tickId, now = clock.millis())
        }

        override suspend fun restoreTick(tickId: String) {
            dao.restoreIfDeleted(tickId, now = clock.millis())
        }

        override suspend fun replaceDay(
            taskId: String,
            day: LocalDate,
            slots: List<LocalTime?>,
        ): DayChange {
            // One reading, so that every row this call touches carries the same instant.
            val now = clock.instant().truncatedTo(ChronoUnit.MILLIS).toEpochMilli()
            val plan = planDay(liveTicks(taskId, day), slots.map { it?.toWholeMinute() })
            val removed = plan.toRemove.filter { dao.softDeleteIfLive(it.id, now) > 0 }.map { it.id }
            val added = plan.toAdd.mapNotNull { insert(taskId, day, it, now)?.id }
            return DayChange(addedIds = added, removedIds = removed)
        }

        override suspend fun undoDayChange(change: DayChange) {
            val now = clock.millis()
            change.addedIds.forEach { dao.softDeleteIfLive(it, now) }
            change.removedIds.forEach { dao.restoreIfDeleted(it, now) }
        }

        private fun LocalTime.toWholeMinute(): LocalTime = truncatedTo(ChronoUnit.MINUTES)

        private suspend fun liveTicks(
            taskId: String,
            day: LocalDate,
        ): List<TaskTick> = dao.findLiveForDay(taskId, day.toEpochDay()).map { it.toDomain() }

        private suspend fun insert(
            taskId: String,
            day: LocalDate,
            slot: LocalTime?,
            now: Long,
        ): TaskTick? {
            val id = ids.newId()
            val inserted = dao.insertForLiveTask(id, taskId, day.toEpochDay(), slot?.toSlotMinute(), now)
            if (inserted == NOT_INSERTED) return slot?.let { s -> liveTicks(taskId, day).firstOrNull { it.slot == s } }
            val time = Instant.ofEpochMilli(now)
            return TaskTick(id, taskId, day, slot, time, time)
        }
    }
