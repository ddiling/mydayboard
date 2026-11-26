package io.github.ddiling.mydayboard.feature.tasks.list

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.ddiling.mydayboard.core.designsystem.preview.PreviewSurface
import io.github.ddiling.mydayboard.core.domain.task.Repeat
import io.github.ddiling.mydayboard.core.domain.task.RowState
import io.github.ddiling.mydayboard.core.domain.task.TaskGroup
import io.github.ddiling.mydayboard.feature.tasks.common.rowOf
import io.github.ddiling.mydayboard.feature.tasks.common.taskOf
import io.github.ddiling.mydayboard.feature.tasks.common.textsToday
import io.github.ddiling.mydayboard.feature.tasks.navigation.TasksListNotice
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TasksListArchiveScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val vacuum = taskOf(Repeat.EveryDay).copy(id = "vacuum", name = "Vacuum")
    private val groups = listOf(TaskGroup(null, listOf(rowOf(vacuum, RowState.PENDING))))

    private fun content(
        archivedCount: Int,
        groups: List<TaskGroup> = this.groups,
    ) = TasksListUiState.Content(groups, textsToday, lateCount = 0, archivedCount = archivedCount, undo = null)

    private fun show(
        state: TasksListUiState,
        actions: TasksListActions = TasksListActions(),
        notice: TasksListNotice? = null,
    ) = composeRule.setContent { PreviewSurface { TasksListScreen(state, actions, notice = notice) } }

    @Test
    fun `the archived row says how many tasks are archived`() {
        show(content(archivedCount = 3))

        composeRule.onNodeWithText("Archived · 3 tasks").assertIsDisplayed()
    }

    @Test
    fun `the archived row is hidden when nothing is archived`() {
        show(content(archivedCount = 0))

        composeRule.onNodeWithTag("tasks_list_archived").assertDoesNotExist()
    }

    @Test
    fun `tapping the archived row calls onArchiveClick`() {
        var opened = 0
        show(content(archivedCount = 1), TasksListActions(onArchiveClick = { opened++ }))

        composeRule.onNodeWithTag("tasks_list_archived").performClick()

        assertEquals(1, opened)
    }

    @Test
    fun `the archived row has a 48 dp target and a description`() {
        show(content(archivedCount = 2))

        composeRule.onNodeWithTag("tasks_list_archived").assertHeightIsAtLeast(48.dp)
        composeRule.onNode(hasContentDescription("Open archive, 2 tasks")).assertIsDisplayed()
    }

    @Test
    fun `when every task is archived the row is shown instead of the empty state`() {
        show(content(archivedCount = 1, groups = emptyList()))

        composeRule.onNodeWithTag("tasks_list_archived").assertIsDisplayed()
        composeRule.onNodeWithTag("tasks_list_empty_hint").assertDoesNotExist()
    }

    @Test
    fun `with a doubled font size the archived row can still be reached`() {
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 2f, fontScale = 2f)) {
                PreviewSurface { TasksListScreen(content(archivedCount = 12), TasksListActions()) }
            }
        }

        composeRule.onNodeWithTag("tasks_list_content").performScrollToNode(hasText("Archived · 12 tasks"))

        composeRule.onNodeWithText("Archived · 12 tasks").assertIsDisplayed()
    }

    @Test
    fun `an archived notice names the task and offers Undo that calls onUndoArchive with its id`() {
        val undone = mutableListOf<String>()
        show(content(0), TasksListActions(onUndoArchive = { undone += it }), TasksListNotice.Archived("pills", "Pills"))

        composeRule.onNodeWithText("Pills archived").assertIsDisplayed()
        composeRule.onNodeWithText("Undo").performClick()

        composeRule.waitForIdle()
        assertEquals(listOf("pills"), undone)
    }

    @Test
    fun `a deleted notice names the task and has no Undo`() {
        show(content(0), notice = TasksListNotice.Deleted("Pills"))

        composeRule.onNodeWithText("Pills deleted").assertIsDisplayed()
        composeRule.onNodeWithText("Undo").assertDoesNotExist()
    }

    @Test
    fun `a notice that has ended is reported as shown`() {
        var shown = 0
        show(content(0), TasksListActions(onNoticeShown = { shown++ }), TasksListNotice.Deleted("Pills"))
        composeRule.waitForIdle()

        composeRule.mainClock.advanceTimeBy(SNACKBAR_TIMEOUT_MS)

        composeRule.waitForIdle()
        assertEquals(1, shown)
    }

    private companion object {
        /** Longer than the Short snackbar duration (4 s), so it times out. */
        const val SNACKBAR_TIMEOUT_MS = 6_000L
    }
}
