package io.github.ddiling.mydayboard.feature.tasks.common

import io.github.ddiling.mydayboard.core.domain.task.OnMiss
import io.github.ddiling.mydayboard.core.domain.task.Repeat
import io.github.ddiling.mydayboard.core.domain.task.RowState
import io.github.ddiling.mydayboard.core.domain.task.Task
import io.github.ddiling.mydayboard.core.domain.task.TaskRow
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** Friday 2026-11-20: the day the text tests are judged on. */
internal val textsToday: LocalDate = LocalDate.of(2026, 11, 20)

internal fun taskOf(
    repeat: Repeat,
    time: LocalTime? = null,
) = Task(
    id = "t1",
    name = "Vacuum",
    categoryId = null,
    repeat = repeat,
    time = time,
    note = null,
    goal = null,
    onMiss = OnMiss.STAYS,
    startDate = LocalDate.of(2026, 1, 1),
    archivedAt = null,
    createdAt = Instant.EPOCH,
    updatedAt = Instant.EPOCH,
)

internal fun rowOf(
    task: Task,
    state: RowState,
    nextDue: LocalDate? = null,
    lateSince: LocalDate? = null,
) = TaskRow(task, null, state, done = 0, target = 0, task.time, nextDue, lateSince)
