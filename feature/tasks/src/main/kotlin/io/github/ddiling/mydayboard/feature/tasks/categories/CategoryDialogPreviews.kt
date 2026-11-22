package io.github.ddiling.mydayboard.feature.tasks.categories

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import io.github.ddiling.mydayboard.core.designsystem.preview.PreviewSurface

private val newCategory = CategoryDialogState.Editing(id = null, name = "")
private val existingCategory = CategoryDialogState.Editing(id = "home", name = "Home", canSave = true)

@Composable
private fun PreviewDialog(state: CategoryDialogState.Editing) {
    PreviewSurface { CategoryDialogContent(state, CategoryDialogActions()) }
}

@PreviewLightDark
@Composable
private fun CategoryDialogNewPreview() = PreviewDialog(newCategory)

@PreviewLightDark
@Composable
private fun CategoryDialogEditPreview() = PreviewDialog(existingCategory)

@PreviewLightDark
@Composable
private fun CategoryDialogDuplicatePreview() =
    PreviewDialog(newCategory.copy(name = "Home", error = CategoryNameError.Duplicate))

@Preview(name = "Large font light", fontScale = 2f)
@Preview(name = "Large font dark", fontScale = 2f, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun CategoryDialogLargeFontPreview() = PreviewDialog(existingCategory)
