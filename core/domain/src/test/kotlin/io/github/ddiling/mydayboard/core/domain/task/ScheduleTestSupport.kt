package io.github.ddiling.mydayboard.core.domain.task

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** Wednesday; the schedule tests count days from here. */
internal val wednesday: LocalDate = LocalDate.of(2026, 11, 18)

private val someInstant = Instant.parse("2026-01-01T00:00:00Z")

internal fun task(
    repeat: Repeat = Repeat.EveryDay,
    startDate: LocalDate = wednesday,
    onMiss: OnMiss = OnMiss.EXPIRES,
    goal: Int? = null,
): Task =
    Task(
        id = "task",
        name = "Vacuum",
        categoryId = null,
        repeat = repeat,
        time = null,
        note = null,
        goal = goal,
        onMiss = onMiss,
        startDate = startDate,
        archivedAt = null,
        createdAt = someInstant,
        updatedAt = someInstant,
    )

internal fun tick(
    day: LocalDate,
    slot: LocalTime? = null,
): TaskTick =
    TaskTick(
        id = "tick-$day-$slot",
        taskId = "task",
        day = day,
        slot = slot,
        createdAt = someInstant,
        updatedAt = someInstant,
    )

internal fun List<Occurrence>.dues(): List<LocalDate> = map { it.due }
