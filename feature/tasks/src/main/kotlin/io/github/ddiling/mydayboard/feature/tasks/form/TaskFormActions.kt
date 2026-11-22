package io.github.ddiling.mydayboard.feature.tasks.form

import io.github.ddiling.mydayboard.core.domain.task.EveryNDaysMode
import io.github.ddiling.mydayboard.core.domain.task.OnMiss
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** What the form screen reports to its ViewModel; grouped so the screen's parameter list stays short. */
internal data class TaskFormActions(
    val onNameChange: (String) -> Unit = {},
    val onCategoryChange: (String?) -> Unit = {},
    val onNewCategory: () -> Unit = {},
    val onRepeatTypeChange: (RepeatType) -> Unit = {},
    val onAddTime: (LocalTime) -> Unit = {},
    val onRemoveTime: (LocalTime) -> Unit = {},
    val onToggleWeekday: (DayOfWeek) -> Unit = {},
    val onIntervalChange: (Int) -> Unit = {},
    val onIntervalModeChange: (EveryNDaysMode) -> Unit = {},
    val onFirstDueChange: (LocalDate) -> Unit = {},
    val onDayOfMonthChange: (Int) -> Unit = {},
    val onDueDateChange: (LocalDate) -> Unit = {},
    val onTimeChange: (LocalTime?) -> Unit = {},
    val onNoteChange: (String) -> Unit = {},
    val onGoalOnChange: (Boolean) -> Unit = {},
    val onGoalChange: (Int) -> Unit = {},
    val onOnMissChange: (OnMiss) -> Unit = {},
    val onSave: () -> Unit = {},
    val onSavedHandled: () -> Unit = {},
    val onClose: () -> Unit = {},
)
