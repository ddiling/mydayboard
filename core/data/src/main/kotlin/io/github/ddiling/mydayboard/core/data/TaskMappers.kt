package io.github.ddiling.mydayboard.core.data

import io.github.ddiling.mydayboard.core.database.model.TaskCategoryEntity
import io.github.ddiling.mydayboard.core.database.model.TaskEntity
import io.github.ddiling.mydayboard.core.database.model.TaskEntityOnMiss
import io.github.ddiling.mydayboard.core.domain.task.OnMiss
import io.github.ddiling.mydayboard.core.domain.task.Task
import io.github.ddiling.mydayboard.core.domain.task.TaskCategory
import io.github.ddiling.mydayboard.core.domain.task.TaskDraft
import java.time.Instant
import java.time.LocalDate

/** Domain fields are filled by the repository; here only the repeat is flattened into its own columns. */
internal fun TaskEntity.toDomain() =
    Task(
        id = id,
        name = name,
        categoryId = categoryId,
        repeat = toRepeat(),
        time = timeOfDay?.toTime(),
        note = note,
        goal = goal,
        onMiss = OnMiss.valueOf(onMiss.name),
        startDate = LocalDate.ofEpochDay(startDate),
        archivedAt = archivedAt?.let(Instant::ofEpochMilli),
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt),
    )

/** A new row is never deleted, so `deletedAt` stays null. Columns of other repeat types stay null. */
internal fun Task.toEntity(): TaskEntity {
    val base =
        TaskEntity(
            id = id,
            name = name,
            categoryId = categoryId,
            repeatType = repeat.toEntityType(),
            times = null,
            weekdays = null,
            intervalDays = null,
            intervalMode = null,
            anchorDate = null,
            dayOfMonth = null,
            timeOfDay = time?.toText(),
            note = note,
            goal = goal,
            onMiss = TaskEntityOnMiss.valueOf(onMiss.name),
            startDate = startDate.toEpochDay(),
            archivedAt = archivedAt?.toEpochMilli(),
            createdAt = createdAt.toEpochMilli(),
            updatedAt = updatedAt.toEpochMilli(),
        )
    return base.withRepeatColumns(repeat)
}

/** Gives a draft the identity and dates the repository owns. */
internal fun TaskDraft.toTask(
    id: String,
    startDate: LocalDate,
    archivedAt: Instant?,
    createdAt: Instant,
    updatedAt: Instant,
) = Task(id, name, categoryId, repeat, time, note, goal, onMiss, startDate, archivedAt, createdAt, updatedAt)

internal fun TaskCategoryEntity.toDomain() =
    TaskCategory(id, name, Instant.ofEpochMilli(createdAt), Instant.ofEpochMilli(updatedAt))

internal fun TaskCategory.toEntity() = TaskCategoryEntity(id, name, createdAt.toEpochMilli(), updatedAt.toEpochMilli())
