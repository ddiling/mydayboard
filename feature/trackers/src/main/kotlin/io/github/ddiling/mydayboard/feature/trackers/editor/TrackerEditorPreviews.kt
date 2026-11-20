package io.github.ddiling.mydayboard.feature.trackers.editor

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import io.github.ddiling.mydayboard.core.designsystem.preview.PreviewSurface
import io.github.ddiling.mydayboard.core.domain.tracker.MAX_TRACKER_NAME_LENGTH
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerType
import java.time.LocalDate

private val previewDate = LocalDate.of(2026, 3, 2)

private val blankState = TrackerEditorUiState(startDate = previewDate, today = previewDate, load = EditorLoad.Ready)

@PreviewLightDark
@Composable
private fun TrackerEditorCreateEmptyPreview() {
    PreviewSurface { TrackerEditorScreen(blankState, TrackerEditorActions()) }
}

@PreviewLightDark
@Composable
private fun TrackerEditorCreateErrorPreview() {
    PreviewSurface {
        TrackerEditorScreen(
            blankState.copy(name = "x".repeat(MAX_TRACKER_NAME_LENGTH + 1), nameEdited = true),
            TrackerEditorActions(),
        )
    }
}

@PreviewLightDark
@Composable
private fun TrackerEditorDaysSincePreview() {
    val state = blankState.copy(name = "No sugar", type = TrackerType.DAYS_SINCE, startDate = previewDate.minusDays(12))
    PreviewSurface { TrackerEditorScreen(state, TrackerEditorActions()) }
}

@PreviewLightDark
@Composable
private fun TrackerEditorEditPreview() {
    PreviewSurface { TrackerEditorScreen(blankState.copy(name = "Water", isEditing = true), TrackerEditorActions()) }
}

@PreviewLightDark
@Composable
private fun TrackerEditorNotFoundPreview() {
    val state = blankState.copy(isEditing = true, load = EditorLoad.NotFound)
    PreviewSurface { TrackerEditorScreen(state, TrackerEditorActions()) }
}

@Preview(name = "Large font light", fontScale = 2f)
@Preview(name = "Large font dark", fontScale = 2f, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun TrackerEditorLargeFontPreview() {
    val state = blankState.copy(name = "Water", nameEdited = true, isEditing = true)
    PreviewSurface { TrackerEditorScreen(state, TrackerEditorActions()) }
}
