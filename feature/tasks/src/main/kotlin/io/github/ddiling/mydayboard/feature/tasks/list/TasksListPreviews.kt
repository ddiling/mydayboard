package io.github.ddiling.mydayboard.feature.tasks.list

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import io.github.ddiling.mydayboard.core.designsystem.preview.PreviewSurface
import io.github.ddiling.mydayboard.core.domain.task.Repeat
import io.github.ddiling.mydayboard.core.domain.task.RowState
import io.github.ddiling.mydayboard.core.domain.task.TaskCategory
import io.github.ddiling.mydayboard.core.domain.task.TaskGroup
import io.github.ddiling.mydayboard.feature.tasks.common.previewRow
import io.github.ddiling.mydayboard.feature.tasks.common.previewToday
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime

private val home = TaskCategory("home", "Home", Instant.EPOCH, Instant.EPOCH)
private val job = TaskCategory("job", "Job", Instant.EPOCH, Instant.EPOCH)
private val workdays = Repeat.Weekdays(DayOfWeek.entries.take(5).toSet())

private val previewContent =
    TasksListUiState.Content(
        groups =
            listOf(
                TaskGroup(
                    home,
                    listOf(
                        previewRow(
                            "Breakfast",
                            Repeat.EveryDay,
                            RowState.DONE,
                            LocalTime.of(8, 30),
                        ).copy(done = 1, target = 1),
                        previewRow("Vacuum", workdays, RowState.PENDING),
                    ),
                ),
                TaskGroup(job, listOf(previewRow("Send CVs", workdays, RowState.PENDING).copy(done = 1, target = 3))),
                TaskGroup(
                    null,
                    listOf(
                        previewRow("Electricity bill", workdays, RowState.LATE, lateSince = previewToday.minusDays(1)),
                    ),
                ),
            ),
        today = previewToday,
        lateCount = 1,
        archivedCount = 0,
        undo = null,
    )

@Composable
private fun PreviewList(state: TasksListUiState) {
    PreviewSurface { TasksListScreen(state, TasksListActions()) }
}

@PreviewLightDark
@Composable
private fun TasksListPreview() = PreviewList(previewContent)

@PreviewLightDark
@Composable
private fun TasksListEmptyPreview() = PreviewList(TasksListUiState.Empty)

@PreviewLightDark
@Composable
private fun TasksListLoadingPreview() = PreviewList(TasksListUiState.Loading)

@Preview(name = "Large font light", fontScale = 2f)
@Preview(name = "Large font dark", fontScale = 2f, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun TasksListLargeFontPreview() = PreviewList(previewContent)
