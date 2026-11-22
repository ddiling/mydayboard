package io.github.ddiling.mydayboard.core.domain.task

import io.github.ddiling.mydayboard.core.domain.text.codePointLength
import io.github.ddiling.mydayboard.core.domain.text.hasNoVisibleCharacter
import io.github.ddiling.mydayboard.core.domain.text.normalizeName

/** The longest task name, in Unicode code points. */
const val MAX_TASK_NAME_LENGTH = 60

/** The longest category name, in Unicode code points. Shorter than a task name: it is a label on a filter chip. */
const val MAX_CATEGORY_NAME_LENGTH = 30

/** The longest task note, in Unicode code points. A note is a reminder line, not a document. */
const val MAX_TASK_NOTE_LENGTH = 80

/** The shortest and longest interval of an every-N-days task. A year is the longest period that still makes sense. */
const val MIN_INTERVAL_DAYS = 1
const val MAX_INTERVAL_DAYS = 365
const val MIN_DAY_OF_MONTH = 1
const val MAX_DAY_OF_MONTH = 31

/** The smallest and largest goal. A goal of 1 is the default and is stored as no goal. */
const val MIN_TASK_GOAL = 2
const val MAX_TASK_GOAL = 99

/** Result of [validateTaskName]. The UI turns each failure into its own localized message. */
sealed interface TaskNameValidation {
    /** The name is acceptable; [name] is the normalized text to store. */
    data class Valid(
        val name: String,
    ) : TaskNameValidation

    /** The name has no visible character. */
    data object Blank : TaskNameValidation

    /** The normalized name has more than [max] code points. */
    data class TooLong(
        val max: Int,
    ) : TaskNameValidation
}

/** Result of [validateCategoryName]. */
sealed interface CategoryNameValidation {
    /** The name is acceptable; [name] is the normalized text to store. */
    data class Valid(
        val name: String,
    ) : CategoryNameValidation

    /** The name has no visible character. */
    data object Blank : CategoryNameValidation

    /** The normalized name has more than [max] code points. */
    data class TooLong(
        val max: Int,
    ) : CategoryNameValidation

    /** Another category already has this name, ignoring case. */
    data object Duplicate : CategoryNameValidation
}

/** One problem found in a task form. The UI shows each next to its own field. */
sealed interface TaskFormError {
    data object BlankName : TaskFormError

    data class NameTooLong(
        val max: Int,
    ) : TaskFormError

    /** "Several times a day" with no time chosen. */
    data object NoTimes : TaskFormError

    /** "On chosen weekdays" with no weekday chosen. */
    data object NoWeekdays : TaskFormError

    data class IntervalOutOfRange(
        val min: Int,
        val max: Int,
    ) : TaskFormError

    data object DayOfMonthOutOfRange : TaskFormError

    data class GoalOutOfRange(
        val min: Int,
        val max: Int,
    ) : TaskFormError

    data class NoteTooLong(
        val max: Int,
    ) : TaskFormError
}

/**
 * Checks a task name typed by the user. It is normalized silently like a tracker name (Unicode NFC, runs of
 * whitespace collapsed to one space, ends trimmed) and counted in code points, so an emoji is one character.
 * Duplicates are allowed: tasks are told apart by id. The repository does not validate, so callers use this first.
 */
fun validateTaskName(raw: String): TaskNameValidation {
    val name = normalizeName(raw)
    return when {
        hasNoVisibleCharacter(name) -> TaskNameValidation.Blank
        name.codePointLength() > MAX_TASK_NAME_LENGTH -> TaskNameValidation.TooLong(MAX_TASK_NAME_LENGTH)
        else -> TaskNameValidation.Valid(name)
    }
}

/**
 * Checks a category name. Normalized like a task name. Categories are chosen from a list by name, so a name equal
 * to one in [existingNames] ignoring case is [CategoryNameValidation.Duplicate]. When renaming, pass the
 * category's own current name as [ignoreName], so keeping the name (or only changing its case) is not a duplicate.
 */
fun validateCategoryName(
    raw: String,
    existingNames: List<String>,
    ignoreName: String? = null,
): CategoryNameValidation {
    val name = normalizeName(raw)
    return when {
        hasNoVisibleCharacter(name) -> {
            CategoryNameValidation.Blank
        }

        name.codePointLength() > MAX_CATEGORY_NAME_LENGTH -> {
            CategoryNameValidation.TooLong(MAX_CATEGORY_NAME_LENGTH)
        }

        existingNames.any { it.equals(name, ignoreCase = true) && !it.equals(ignoreName, ignoreCase = true) } -> {
            CategoryNameValidation.Duplicate
        }

        else -> {
            CategoryNameValidation.Valid(name)
        }
    }
}

/**
 * Every problem in [draft], in the order of the form (name, repeat, goal, note), so the UI can show all at once
 * and focus the first. Empty means the draft can be saved. The name is checked as [validateTaskName] does.
 */
fun validateTaskDraft(draft: TaskDraft): List<TaskFormError> =
    listOfNotNull(nameError(draft.name)) +
        listOfNotNull(repeatError(draft.repeat)) +
        listOfNotNull(goalError(draft.goal), noteError(draft.note))

private fun nameError(raw: String): TaskFormError? =
    when (val result = validateTaskName(raw)) {
        is TaskNameValidation.Valid -> null
        TaskNameValidation.Blank -> TaskFormError.BlankName
        is TaskNameValidation.TooLong -> TaskFormError.NameTooLong(result.max)
    }

private fun repeatError(repeat: Repeat): TaskFormError? =
    when (repeat) {
        is Repeat.TimesPerDay -> {
            TaskFormError.NoTimes.takeIf { repeat.times.isEmpty() }
        }

        is Repeat.Weekdays -> {
            TaskFormError.NoWeekdays.takeIf { repeat.days.isEmpty() }
        }

        is Repeat.EveryNDays -> {
            TaskFormError
                .IntervalOutOfRange(MIN_INTERVAL_DAYS, MAX_INTERVAL_DAYS)
                .takeIf { repeat.days !in MIN_INTERVAL_DAYS..MAX_INTERVAL_DAYS }
        }

        is Repeat.Monthly -> {
            TaskFormError.DayOfMonthOutOfRange.takeIf { repeat.dayOfMonth !in MIN_DAY_OF_MONTH..MAX_DAY_OF_MONTH }
        }

        Repeat.EveryDay, is Repeat.OneTime -> {
            null
        }
    }

private fun goalError(goal: Int?): TaskFormError? =
    TaskFormError
        .GoalOutOfRange(MIN_TASK_GOAL, MAX_TASK_GOAL)
        .takeIf { goal != null && goal !in MIN_TASK_GOAL..MAX_TASK_GOAL }

private fun noteError(note: String?): TaskFormError? =
    TaskFormError
        .NoteTooLong(MAX_TASK_NOTE_LENGTH)
        .takeIf { note != null && note.codePointLength() > MAX_TASK_NOTE_LENGTH }
