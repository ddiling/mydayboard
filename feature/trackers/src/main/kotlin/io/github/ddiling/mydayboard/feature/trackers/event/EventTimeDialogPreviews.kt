package io.github.ddiling.mydayboard.feature.trackers.event

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import io.github.ddiling.mydayboard.core.designsystem.preview.PreviewSurface
import java.time.LocalDate
import java.time.LocalTime

private val previewAdd = EventTimeEditorState(EntryMode.Add, LocalDate.of(2026, 3, 2), LocalTime.of(9, 30), null)
private val previewEdit = previewAdd.copy(mode = EntryMode.Edit("e1"))
private val previewError = previewEdit.copy(error = EntryTimeError.BEFORE_TRACKER_START)

@Composable
private fun DialogPreview(state: EventTimeEditorState) =
    PreviewSurface { EventTimeDialog(state, is24Hour = true, actions = EventTimeDialogActions()) }

@PreviewLightDark
@Composable
private fun EventTimeDialogAddPreview() = DialogPreview(previewAdd)

@PreviewLightDark
@Composable
private fun EventTimeDialogEditPreview() = DialogPreview(previewEdit)

@PreviewLightDark
@Composable
private fun EventTimeDialogErrorPreview() = DialogPreview(previewError)

@Preview(name = "Entry dialog large font", fontScale = 2f)
@Composable
private fun EventTimeDialogLargeFontPreview() = DialogPreview(previewError)
