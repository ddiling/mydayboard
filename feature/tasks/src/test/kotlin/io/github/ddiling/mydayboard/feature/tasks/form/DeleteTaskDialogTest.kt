package io.github.ddiling.mydayboard.feature.tasks.form

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class DeleteTaskDialogTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val editing = TaskFormUiState(today = LocalDate.of(2026, 3, 2), isEditing = true)

    private fun showForm(
        state: TaskFormUiState,
        actions: TaskFormActions = TaskFormActions(),
    ) = composeRule.setContent { TaskFormScreen(state, emptyList(), actions) }

    @Test
    fun `STOP OR REMOVE is shown when editing`() {
        showForm(editing)

        composeRule.onNodeWithTag("tasks_form_archive").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("tasks_form_delete").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `STOP OR REMOVE is hidden for a new task`() {
        showForm(editing.copy(isEditing = false))

        composeRule.onAllNodesWithTag("tasks_form_archive").assertCountEquals(0)
        composeRule.onAllNodesWithTag("tasks_form_delete").assertCountEquals(0)
    }

    @Test
    fun `the archive row calls onArchive`() {
        var archives = 0
        showForm(editing, TaskFormActions(onArchive = { archives++ }))

        composeRule.onNodeWithTag("tasks_form_archive").performScrollTo().performClick()

        assertEquals(1, archives)
    }

    @Test
    fun `the delete row calls onDeleteRequest and is a button`() {
        var requests = 0
        showForm(editing, TaskFormActions(onDeleteRequest = { requests++ }))

        composeRule
            .onNodeWithTag("tasks_form_delete")
            .performScrollTo()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()

        assertEquals(1, requests)
    }

    @Test
    fun `both rows have a 48 dp tall target`() {
        showForm(editing)

        composeRule.onNodeWithTag("tasks_form_archive").performScrollTo().assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithTag("tasks_form_delete").performScrollTo().assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun `the dialog names the task`() {
        composeRule.setContent { DeleteTaskContent("Pills", TaskFormActions()) }

        composeRule.onNodeWithText("Delete Pills and all its history?").assertIsDisplayed()
    }

    @Test
    fun `Keep it calls onDeleteDismiss and Delete calls onDeleteConfirm`() {
        var keeps = 0
        var deletes = 0
        composeRule.setContent {
            DeleteTaskContent("Pills", TaskFormActions(onDeleteDismiss = { keeps++ }, onDeleteConfirm = { deletes++ }))
        }

        composeRule.onNodeWithTag("tasks_delete_keep").performClick()
        composeRule.onNodeWithTag("tasks_delete_confirm").performClick()

        assertEquals(1, keeps)
        assertEquals(1, deletes)
    }

    @Test
    fun `both buttons stay reachable at twice the font size`() {
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 2f)) {
                DeleteTaskContent("Pills", TaskFormActions())
            }
        }

        composeRule.onNodeWithTag("tasks_delete_keep").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("tasks_delete_confirm").performScrollTo().assertIsDisplayed()
    }
}
