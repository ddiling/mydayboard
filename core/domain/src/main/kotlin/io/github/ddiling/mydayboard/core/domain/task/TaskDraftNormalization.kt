package io.github.ddiling.mydayboard.core.domain.task

/**
 * The draft as it must be stored. Validation only requires one time for a several-times-a-day task, so the
 * repositories sort the times and drop repeats; every consumer can then rely on [Repeat.TimesPerDay.times] being
 * sorted and distinct.
 */
fun TaskDraft.normalized(): TaskDraft =
    when (val r = repeat) {
        is Repeat.TimesPerDay -> copy(repeat = Repeat.TimesPerDay(r.times.distinct().sorted()))
        else -> this
    }
