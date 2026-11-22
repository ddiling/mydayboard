package io.github.ddiling.mydayboard.feature.tasks.form

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import io.github.ddiling.mydayboard.core.designsystem.preview.PreviewSurface
import io.github.ddiling.mydayboard.core.domain.task.EveryNDaysMode
import io.github.ddiling.mydayboard.core.domain.task.TaskCategory
import io.github.ddiling.mydayboard.core.domain.task.TaskFormError
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

private val previewToday = LocalDate.of(2026, 3, 2)

private val previewCategories =
    listOf(TaskCategory("home", "Home", Instant.EPOCH, Instant.EPOCH))

private val emptyForm = TaskFormUiState(today = previewToday)

@Composable
private fun PreviewForm(state: TaskFormUiState) {
    PreviewSurface { TaskFormScreen(state, previewCategories, TaskFormActions()) }
}

@PreviewLightDark
@Composable
private fun TaskFormEmptyPreview() = PreviewForm(emptyForm)

@PreviewLightDark
@Composable
private fun TaskFormSeveralTimesPreview() =
    PreviewForm(
        emptyForm.copy(
            name = "Pills",
            repeatType = RepeatType.TIMES_PER_DAY,
            times = listOf(LocalTime.of(8, 0), LocalTime.of(14, 0), LocalTime.of(21, 30)),
            canSave = true,
        ),
    )

@PreviewLightDark
@Composable
private fun TaskFormEveryDayPreview() =
    PreviewForm(
        emptyForm.copy(
            name = "Vacuum",
            categoryId = "home",
            time = LocalTime.of(18, 0),
            note = "Living room",
            canSave = true,
        ),
    )

@PreviewLightDark
@Composable
private fun TaskFormWeekdaysPreview() =
    PreviewForm(
        emptyForm.copy(
            name = "English",
            repeatType = RepeatType.WEEKDAYS,
            weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            canSave = true,
        ),
    )

@PreviewLightDark
@Composable
private fun TaskFormErrorsPreview() =
    PreviewForm(
        emptyForm.copy(
            name = " ",
            repeatType = RepeatType.WEEKDAYS,
            errors = listOf(TaskFormError.BlankName, TaskFormError.NoWeekdays),
        ),
    )

@PreviewLightDark
@Composable
private fun TaskFormEveryNDaysPreview() =
    PreviewForm(
        emptyForm.copy(
            name = "Water filter",
            repeatType = RepeatType.EVERY_N_DAYS,
            intervalMode = EveryNDaysMode.FIXED_CALENDAR,
            canSave = true,
        ),
    )

@PreviewLightDark
@Composable
private fun TaskFormMonthlyPreview() =
    PreviewForm(
        emptyForm.copy(name = "Pay bills", repeatType = RepeatType.MONTHLY, dayOfMonth = 15, canSave = true),
    )

@PreviewLightDark
@Composable
private fun TaskFormOneTimePreview() =
    PreviewForm(
        emptyForm.copy(
            name = "Send CVs",
            repeatType = RepeatType.ONE_TIME,
            dueDate = previewToday.plusDays(7),
            canSave = true,
        ),
    )

@Preview(name = "Large font light", fontScale = 2f)
@Preview(name = "Large font dark", fontScale = 2f, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun TaskFormLargeFontPreview() =
    PreviewForm(
        emptyForm.copy(
            name = "Pills",
            repeatType = RepeatType.WEEKDAYS,
            weekdays = setOf(DayOfWeek.TUESDAY),
            canSave = true,
        ),
    )
