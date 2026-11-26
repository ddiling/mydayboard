package io.github.ddiling.mydayboard.feature.tasks.detail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.ddiling.mydayboard.core.designsystem.preview.PreviewSurface
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Robolectric supplies the real `Bundle` that `toRoute` needs, and a main looper for the view model. */
@RunWith(RobolectricTestRunner::class)
class TaskDetailRouteTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val fixture = TaskDetailFixture()

    private fun showFor(
        taskName: String,
        onEdit: () -> Unit = {},
    ) {
        val task = runBlocking { fixture.addTask(taskName) }
        composeRule.setContent {
            PreviewSurface { TaskDetailRoute(onBack = {}, onEdit = onEdit, viewModel = fixture.viewModel(task.id)) }
        }
    }

    @Test
    fun `pressing the big button through the view model ticks the task`() {
        showFor("Pills")

        composeRule.onNodeWithTag("tasks_detail_big_button").performClick()

        composeRule.waitUntil { composeRule.onAllNodesWithText("Done today").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("Done today").assertIsDisplayed()
    }

    @Test
    fun `undo after an untick ticks the task again through the view model`() {
        val task = runBlocking { fixture.addTask("Pills").also { fixture.tickOn(it, fixture.today) } }
        composeRule.setContent {
            PreviewSurface { TaskDetailRoute(onBack = {}, onEdit = {}, viewModel = fixture.viewModel(task.id)) }
        }
        composeRule.waitUntil { composeRule.onAllNodesWithText("Done today").fetchSemanticsNodes().isNotEmpty() }

        composeRule.onNodeWithTag("tasks_detail_big_button").performClick()
        composeRule.waitUntil { composeRule.onAllNodesWithText("Pills unticked").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("Not done yet").assertIsDisplayed()

        composeRule.onNodeWithText("Undo").performClick()

        composeRule.waitUntil { composeRule.onAllNodesWithText("Done today").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("Done today").assertIsDisplayed()
    }

    @Test
    fun `the pencil calls onEdit`() {
        var edits = 0
        showFor("Pills") { edits++ }

        composeRule.onNodeWithTag("tasks_detail_edit").performClick()

        assertEquals(1, edits)
    }
}
