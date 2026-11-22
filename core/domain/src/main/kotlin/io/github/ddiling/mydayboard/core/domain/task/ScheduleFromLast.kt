package io.github.ddiling.mydayboard.core.domain.task

import java.time.LocalDate

/**
 * Windows of an every-N-days task counted from the last completion: the next due day depends on when the
 * previous occurrence was done, so they are walked one at a time.
 *
 * An expiring occurrence is missed after its due day, and the next one is still due + N. A staying one
 * stays open until the target is reached; the next due day is that completion day + N, and with no
 * completion yet the walk stops at one open occurrence.
 */
internal fun fromLastWindows(
    task: Task,
    repeat: Repeat.EveryNDays,
    countedDays: List<LocalDate>,
): Sequence<DueWindow> =
    sequence {
        val target = targetOf(task)
        var due = repeat.firstDue
        while (true) {
            if (task.onMiss == OnMiss.EXPIRES) {
                yield(DueWindow(due, due))
                due = due.plusDays(repeat.days.toLong())
            } else {
                val completion = countedDays.filter { !it.isBefore(due) }.getOrNull(target - 1)
                if (completion == null) {
                    yield(DueWindow(due, null))
                    break
                }
                val next = completion.plusDays(repeat.days.toLong())
                yield(DueWindow(due, next.minusDays(1)))
                due = next
            }
        }
    }
