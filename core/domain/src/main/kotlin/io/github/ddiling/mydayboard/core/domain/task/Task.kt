package io.github.ddiling.mydayboard.core.domain.task

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** A task as stored. [archivedAt] is set while the task is archived (hidden from today, kept with history). */
data class Task(
    val id: String,
    val name: String,
    val categoryId: String?,
    val repeat: Repeat,
    val time: LocalTime?,
    val note: String?,
    val goal: Int?,
    val onMiss: OnMiss,
    /** First day the task counts; earlier days are never planned. */
    val startDate: LocalDate,
    val archivedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/** A named group of tasks. */
data class TaskCategory(
    val id: String,
    val name: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/** One completion of [taskId] on [day]; [slot] is the time it was for when the task repeats several times a day. */
data class TaskTick(
    val id: String,
    val taskId: String,
    val day: LocalDate,
    val slot: LocalTime?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/** What a day-sheet save changed, so one Undo can revert exactly that: delete the added ticks, restore the removed. */
data class DayChange(
    val addedIds: List<String>,
    val removedIds: List<String>,
)
