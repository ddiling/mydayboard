package io.github.ddiling.mydayboard.feature.tasks.detail

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import io.github.ddiling.mydayboard.core.designsystem.preview.PreviewSurface
import io.github.ddiling.mydayboard.core.domain.task.TickOutcome
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TaskDetailUndoSnackbarTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `an untick shows its message with Undo that calls onUndo`() {
        var undos = 0
        val outcome = TickOutcome.Removed("t", "tick", done = 0, target = 1)
        val state = previewDetail(previewTask()).copy(pendingUndo = DetailUndo.Unticked("English", outcome))
        composeRule.setContent {
            PreviewSurface { TaskDetailScreen(state, TaskDetailActions(onUndo = { undos++ })) }
        }

        composeRule.onNode(hasText("English unticked")).assertIsDisplayed()
        composeRule.onNode(hasText("Undo")).performClick()

        composeRule.waitForIdle()
        assertEquals(1, undos)
    }
}
