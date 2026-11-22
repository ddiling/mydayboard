package io.github.ddiling.mydayboard.core.domain.task

import java.time.temporal.ChronoUnit

/**
 * The draft as it must be stored. Times are saved as HH:mm, so they are first cut to whole minutes. Validation only
 * requires one time for a several-times-a-day task, so the repositories then sort the times and drop repeats; every
 * consumer can rely on [Repeat.TimesPerDay.times] being sorted and distinct.
 */
fun TaskDraft.normalized(): TaskDraft =
    when (val r = repeat) {
        is Repeat.TimesPerDay -> {
            val minutes = r.times.map { it.truncatedTo(ChronoUnit.MINUTES) }
            copy(repeat = Repeat.TimesPerDay(minutes.distinct().sorted()))
        }

        else -> {
            this
        }
    }
