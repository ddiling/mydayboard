package io.github.ddiling.mydayboard.feature.tasks.form

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.github.ddiling.mydayboard.core.designsystem.preview.PreviewSurface

@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DeleteTaskDialogPreview() = PreviewSurface { DeleteTaskContent("Pills", TaskFormActions()) }

@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun StopOrRemovePreview() = PreviewSurface { StopOrRemoveSection(TaskFormActions()) }
