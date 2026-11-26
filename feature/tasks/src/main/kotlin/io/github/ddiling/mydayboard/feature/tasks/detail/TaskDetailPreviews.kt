package io.github.ddiling.mydayboard.feature.tasks.detail

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import io.github.ddiling.mydayboard.core.designsystem.preview.PreviewSurface
import io.github.ddiling.mydayboard.core.domain.task.OnMiss
import io.github.ddiling.mydayboard.core.domain.task.Repeat
import io.github.ddiling.mydayboard.core.domain.task.Task
import io.github.ddiling.mydayboard.feature.tasks.common.previewToday
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

private val pastDays =
    listOf(2, 4, 6, 9, 13, 16, 18).map { LocalDate.of(2026, 11, it) }

/** Ticks on most past planned days (Mon, Wed, Fri) and none on Nov 11, so the history shows a miss. */
private fun Task.pastTicks() = pastDays.map { previewTick(this, it) }

@Composable
internal fun PreviewDetail(state: TaskDetailUiState) {
    PreviewSurface { TaskDetailScreen(state, TaskDetailActions()) }
}

@PreviewLightDark
@Composable
private fun TaskDetailNotDonePreview() = PreviewDetail(previewDetail(ticks = previewTask().pastTicks()))

@PreviewLightDark
@Composable
private fun TaskDetailDonePreview() {
    val task = previewTask()
    PreviewDetail(previewDetail(task, task.pastTicks() + previewTick(task, previewToday)))
}

@PreviewLightDark
@Composable
private fun TaskDetailSeveralTimesPreview() {
    val morning = LocalTime.of(8, 0)
    val evening = LocalTime.of(20, 0)
    val task = previewTask(Repeat.TimesPerDay(listOf(morning, evening)))
    val earlier =
        listOf(18, 17).flatMap { day ->
            listOf(morning, evening).map { previewTick(task, LocalDate.of(2026, 11, day), it) }
        }
    val halfDay = previewTick(task, LocalDate.of(2026, 11, 19), morning)
    PreviewDetail(previewDetail(task, earlier + halfDay + previewTick(task, previewToday, morning)))
}

@PreviewLightDark
@Composable
private fun TaskDetailCountGoalPreview() {
    val task = previewTask(Repeat.EveryDay).copy(goal = 3)
    val ticks = List(2) { previewTick(task, previewToday.minusDays(1)) } + previewTick(task, previewToday)
    PreviewDetail(previewDetail(task, ticks))
}

@PreviewLightDark
@Composable
private fun TaskDetailNotDuePreview() {
    val task = previewTask(Repeat.Weekdays(setOf(DayOfWeek.MONDAY)))
    PreviewDetail(previewDetail(task, listOf(previewTick(task, LocalDate.of(2026, 11, 16)))))
}

@PreviewLightDark
@Composable
private fun TaskDetailLatePreview() =
    PreviewDetail(previewDetail(previewTask(Repeat.OneTime(previewToday.minusDays(2)), OnMiss.STAYS)))

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
