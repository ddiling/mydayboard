package io.github.ddiling.mydayboard.feature.tasks.day

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import io.github.ddiling.mydayboard.core.designsystem.preview.PreviewSurface
import io.github.ddiling.mydayboard.feature.tasks.common.previewToday
import java.time.LocalTime

private val morning = LocalTime.of(8, 0)
private val evening = LocalTime.of(20, 0)
private val savedDay = previewToday.minusDays(1)

@Composable
private fun PreviewSheet(kind: DaySheetKind) {
    PreviewSurface { DaySheetContent(DaySheetState("lunch", "Lunch", savedDay, kind), DaySheetActions()) }
}

@PreviewLightDark
@Composable
private fun DaySheetSinglePreview() = PreviewSheet(DaySheetKind.Single(done = false))

@PreviewLightDark
@Composable
private fun DaySheetTimesPreview() = PreviewSheet(DaySheetKind.Times(listOf(morning, evening), setOf(morning)))

@PreviewLightDark
@Composable
private fun DaySheetCountPreview() = PreviewSheet(DaySheetKind.Count(target = 3, value = 3))

@Preview(name = "Large font light", fontScale = 2f)
@Preview(name = "Large font dark", fontScale = 2f, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DaySheetLargeFontPreview() = PreviewSheet(DaySheetKind.Times(listOf(morning, evening), setOf(morning)))
