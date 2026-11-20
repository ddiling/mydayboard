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

private fun previewState(
    name: String = "",
    nameEdited: Boolean = false,
    type: TrackerType = TrackerType.EVENT_COUNTER,
    startDate: LocalDate = previewDate,
    isEditing: Boolean = false,
    load: EditorLoad = EditorLoad.Ready,
) = TrackerEditorUiState(load, isEditing, name, nameEdited, type, startDate, previewDate)

@PreviewLightDark
@Composable
private fun TrackerEditorCreateEmptyPreview() {
    PreviewSurface { TrackerEditorScreen(previewState(), TrackerEditorActions()) }
}

@PreviewLightDark
@Composable
private fun TrackerEditorCreateErrorPreview() {
    PreviewSurface {
        TrackerEditorScreen(previewState("x".repeat(MAX_TRACKER_NAME_LENGTH + 1), true), TrackerEditorActions())
    }
}

@PreviewLightDark
@Composable
private fun TrackerEditorDaysSincePreview() {
    val state = previewState("No sugar", true, TrackerType.DAYS_SINCE, previewDate.minusDays(12))
    PreviewSurface { TrackerEditorScreen(state, TrackerEditorActions()) }
}

@PreviewLightDark
@Composable
private fun TrackerEditorEditPreview() {
    PreviewSurface { TrackerEditorScreen(previewState("Water", isEditing = true), TrackerEditorActions()) }
}

@PreviewLightDark
@Composable
private fun TrackerEditorNotFoundPreview() {
    PreviewSurface {
        TrackerEditorScreen(previewState(isEditing = true, load = EditorLoad.NotFound), TrackerEditorActions())
    }
}

@Preview(name = "Large font light", fontScale = 2f)
@Preview(name = "Large font dark", fontScale = 2f, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun TrackerEditorLargeFontPreview() {
    PreviewSurface { TrackerEditorScreen(previewState("Water", true, isEditing = true), TrackerEditorActions()) }
}
