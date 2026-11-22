package io.github.ddiling.mydayboard.core.data

import io.github.ddiling.mydayboard.core.database.model.TaskTickEntity
import io.github.ddiling.mydayboard.core.domain.task.TaskTick
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

private const val SECONDS_PER_MINUTE = 60

/** The slot is stored as a minute of the day, so seconds of a slot are not kept. */
internal fun LocalTime.toSlotMinute(): Int = toSecondOfDay() / SECONDS_PER_MINUTE

internal fun TaskTickEntity.toDomain() =
    TaskTick(
        id = id,
        taskId = taskId,
        day = LocalDate.ofEpochDay(day),
        slot = slotMinute?.let { LocalTime.ofSecondOfDay(it.toLong() * SECONDS_PER_MINUTE) },
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt),
    )
