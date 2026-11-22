package io.github.ddiling.mydayboard.feature.tasks.detail

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import io.github.ddiling.mydayboard.core.designsystem.preview.PreviewSurface
import io.github.ddiling.mydayboard.core.domain.task.Repeat
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class TaskDetailScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun show(
        state: TaskDetailUiState,
        actions: TaskDetailActions = TaskDetailActions(),
        fontScale: Float = 1f,
    ) = composeRule.setContent {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
            PreviewSurface { TaskDetailScreen(state, actions) }
        }
    }

    @Test
    fun `the pencil calls onEdit`() {
        var edits = 0
        show(previewDetail(), TaskDetailActions(onEdit = { edits++ }))

        composeRule.onNodeWithTag("tasks_detail_edit").performClick()

        assertEquals(1, edits)
    }

    @Test
    fun `the back arrow calls onBack`() {
        var backs = 0
        show(previewDetail(), TaskDetailActions(onBack = { backs++ }))

        composeRule.onNodeWithTag("tasks_detail_back").performClick()

        assertEquals(1, backs)
    }

    @Test
    fun `Back to tasks on the not found screen calls onBack`() {
        var backs = 0
        show(TaskDetailUiState.NotFound, TaskDetailActions(onBack = { backs++ }))

        composeRule.onNodeWithTag("tasks_detail_not_found_back").performClick()

        assertEquals(1, backs)
    }

    @Test
    fun `the not found screen has no pencil`() {
        show(TaskDetailUiState.NotFound)

        composeRule.onNodeWithTag("tasks_detail_edit").assertDoesNotExist()
    }

    @Test
    fun `loading shows a spinner`() {
        show(TaskDetailUiState.Loading)

        composeRule.onNodeWithTag("tasks_detail_loading").assertIsDisplayed()
    }

    @Test
    fun `the status word is a heading and the first thing in the content`() {
        show(previewDetail())

        composeRule.onNodeWithTag("tasks_detail_content").onChildAt(0).assert(hasTestTag("tasks_detail_hero"))
        composeRule
            .onNodeWithTag("tasks_detail_hero")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    @Test
    fun `a late task shows the note that it stays until done`() {
        show(previewDetail(previewTask(Repeat.OneTime(LocalDate.of(2026, 11, 18)))))

        composeRule.onNodeWithTag("tasks_detail_late_note").assertIsDisplayed()
    }

    @Test
    fun `a task that is not late shows no note`() {
        show(previewDetail())

        composeRule.onNodeWithTag("tasks_detail_late_note").assertDoesNotExist()
    }

    @Test
    fun `the stats are reachable at a double font size`() {
        show(previewDetail(), fontScale = 2f)

        composeRule.onNodeWithTag("tasks_detail_stats").assertIsDisplayed()
    }
}
