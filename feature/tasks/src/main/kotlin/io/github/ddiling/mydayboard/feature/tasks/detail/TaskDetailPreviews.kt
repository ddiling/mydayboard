package io.github.ddiling.mydayboard.feature.tasks.detail

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import io.github.ddiling.mydayboard.core.designsystem.preview.PreviewSurface
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
internal fun previewTask(repeat: Repeat = workdays): Task =
    previewRow("English", repeat, RowState.PENDING).task.copy(note = "with Anna", startDate = firstDay)

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

@Composable
internal fun PreviewDetail(state: TaskDetailUiState) {
    PreviewSurface { TaskDetailScreen(state, TaskDetailActions()) }
}

@PreviewLightDark
@Composable
private fun TaskDetailNotDonePreview() = PreviewDetail(previewDetail())

@PreviewLightDark
@Composable
private fun TaskDetailLatePreview() =
    PreviewDetail(previewDetail(previewTask(Repeat.OneTime(previewToday.minusDays(2)))))

@PreviewLightDark
@Composable
private fun TaskDetailNotFoundPreview() = PreviewDetail(TaskDetailUiState.NotFound)

@PreviewLightDark
@Composable
private fun TaskDetailLoadingPreview() = PreviewDetail(TaskDetailUiState.Loading)

@Preview(name = "Large font light", fontScale = 2f)
@Preview(name = "Large font dark", fontScale = 2f, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun TaskDetailLargeFontPreview() {
    val twiceADay = previewTask(Repeat.TimesPerDay(listOf(LocalTime.of(8, 0), LocalTime.of(20, 0))))
    PreviewDetail(previewDetail(twiceADay, listOf(previewTick(twiceADay, previewToday, LocalTime.of(8, 0)))))
}
