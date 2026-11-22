package io.github.ddiling.mydayboard.feature.tasks.categories

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import io.github.ddiling.mydayboard.core.designsystem.preview.PreviewSurface
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CategoryStatsScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val content = previewStatsContent

    private fun show(
        state: CategoryStatsUiState,
        actions: CategoryStatsActions = CategoryStatsActions(),
        fontScale: Float = 1f,
        dialog: CategoryDialogState = CategoryDialogState.Closed,
    ) = composeRule.setContent {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
            PreviewSurface { CategoryStatsScreen(state, actions, dialog = dialog) }
        }
    }

    @Test
    fun `the back button calls onBack`() {
        var backs = 0
        show(content, CategoryStatsActions(onBack = { backs++ }))

        composeRule.onNodeWithTag("tasks_catstats_back").performClick()

        assertEquals(1, backs)
    }

    @Test
    fun `the pencil calls onEdit`() {
        var edits = 0
        show(content, CategoryStatsActions(onEdit = { edits++ }))

        composeRule.onNodeWithTag("tasks_catstats_edit").performClick()

        assertEquals(1, edits)
    }

    @Test
    fun `an empty category says there are no tasks in it`() {
        show(CategoryStatsUiState.Empty("Meals"))

        composeRule.onNode(hasText("No tasks in Meals")).assertIsDisplayed()
    }

    @Test
    fun `an unknown category says it is gone and offers no pencil`() {
        show(CategoryStatsUiState.NotFound)

        composeRule.onNode(hasText("This category is gone")).assertIsDisplayed()
        composeRule.onNodeWithTag("tasks_catstats_edit").assertDoesNotExist()
    }

    @Test
    fun `loading shows a spinner`() {
        show(CategoryStatsUiState.Loading)

        composeRule.onNodeWithTag("tasks_catstats_loading").assertIsDisplayed()
    }

    @Test
    fun `an open dialog is shown above the screen`() {
        show(content, dialog = CategoryDialogState.Editing(id = "meals", name = "Meals", canSave = true))

        composeRule.onNode(hasText("Edit category")).assertIsDisplayed()
    }
}
