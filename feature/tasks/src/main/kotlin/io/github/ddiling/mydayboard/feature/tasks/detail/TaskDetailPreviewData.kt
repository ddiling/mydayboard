package io.github.ddiling.mydayboard.feature.tasks.detail

import io.github.ddiling.mydayboard.core.domain.task.OnMiss
import io.github.ddiling.mydayboard.core.domain.task.Repeat
import io.github.ddiling.mydayboard.core.domain.task.RowState
import io.github.ddiling.mydayboard.core.domain.task.Task
import io.github.ddiling.mydayboard.core.domain.task.TaskTick
import io.github.ddiling.mydayboard.feature.tasks.common.previewRow
import io.github.ddiling.mydayboard.feature.tasks.common.previewToday
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

private val workdays = Repeat.Weekdays(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
private val firstDay = LocalDate.of(2026, 11, 2)

/** A task that started on a Monday, so its history has days to show; today ([previewToday]) is a Friday. */
internal fun previewTask(
    repeat: Repeat = workdays,
    onMiss: OnMiss = OnMiss.EXPIRES,
): Task =
    previewRow("English", repeat, RowState.PENDING).task.copy(note = "with Anna", startDate = firstDay, onMiss = onMiss)

internal fun previewTick(
    task: Task,
    day: LocalDate,
    slot: LocalTime? = null,
) = TaskTick("$day $slot", task.id, day, slot, Instant.EPOCH, Instant.EPOCH)

/** The state the screen would get for [task] with [ticks], built by the real builder so previews cannot drift. */
internal fun previewDetail(
    task: Task = previewTask(),
    ticks: List<TaskTick> = emptyList(),
) = detailContent(task, "Learning", ticks, previewToday)
