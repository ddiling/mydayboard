package io.github.ddiling.mydayboard.feature.tasks.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import io.github.ddiling.mydayboard.core.designsystem.preview.PreviewSurface
import io.github.ddiling.mydayboard.core.domain.task.OnMiss
import io.github.ddiling.mydayboard.core.domain.task.Repeat
import io.github.ddiling.mydayboard.core.domain.task.RowState
import io.github.ddiling.mydayboard.core.domain.task.Task
import io.github.ddiling.mydayboard.core.domain.task.TaskRow
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

private val previewToday = LocalDate.of(2026, 11, 20)

private fun previewRow(
    name: String,
    repeat: Repeat,
    state: RowState,
    time: LocalTime? = null,
    lateSince: LocalDate? = null,
): TaskRow {
    val task =
        Task(name, name, null, repeat, time, null, null, OnMiss.STAYS, previewToday, null, Instant.EPOCH, Instant.EPOCH)
    return TaskRow(task, null, state, done = 0, target = 0, time = time, nextDue = previewToday, lateSince = lateSince)
}

@PreviewLightDark
@Composable
private fun TaskRowsPreview() {
    val workdays = Repeat.Weekdays(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
    PreviewSurface {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            TaskRowLine(
                previewRow("Breakfast", Repeat.EveryDay, RowState.PENDING, LocalTime.of(8, 30)),
                previewToday,
                {},
            )
            TaskRowLine(
                previewRow("Send CVs", Repeat.EveryDay, RowState.PENDING).copy(done = 1, target = 3),
                previewToday,
                {},
            )
            TaskRowLine(
                previewRow("Electricity bill", workdays, RowState.LATE, lateSince = previewToday.minusDays(1)),
                previewToday,
                {},
            )
            TaskRowLine(
                previewRow("Vacuum", workdays, RowState.DONE).copy(done = 1, target = 1),
                previewToday,
                {},
            )
        }
    }
}
