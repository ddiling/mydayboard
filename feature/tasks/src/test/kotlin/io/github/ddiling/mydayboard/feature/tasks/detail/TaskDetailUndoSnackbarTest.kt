package io.github.ddiling.mydayboard.feature.tasks.detail

import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.ddiling.mydayboard.core.designsystem.preview.PreviewSurface
import io.github.ddiling.mydayboard.core.domain.task.TickOutcome
import io.github.ddiling.mydayboard.feature.tasks.common.UndoOffer
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TaskDetailUndoSnackbarTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val hostState = SnackbarHostState()
    private var undoTaps = 0
    private var dismissals = 0

    private val singleTick = TickOutcome.Added("t1", "tick1", done = 1, target = 1, slot = null)

    private fun showUndo(undo: DetailUndo?) {
        composeRule.setContent {
            PreviewSurface {
                SnackbarHost(hostState)
                TaskDetailUndoSnackbar(
                    undo = undo,
                    hostState = hostState,
                    onUndo = { undoTaps++ },
                    onDismissed = { dismissals++ },
                )
            }
        }
    }

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

    @Test
    fun `a tick shows the list message with Undo that calls onUndo`() {
        showUndo(DetailUndo.Ticked(UndoOffer("Breakfast", singleTick)))

        composeRule.onNodeWithText("Breakfast done").assertIsDisplayed()
        composeRule.onNodeWithText("Undo").performClick()

        composeRule.waitForIdle()
        assertEquals(1, undoTaps)
    }

    @Test
    fun `dismissing an untick message calls onDismissed`() {
        val outcome = TickOutcome.Removed("t", "tick", done = 0, target = 1)
        showUndo(DetailUndo.Unticked("English", outcome))
        composeRule.waitForIdle()

        composeRule.runOnIdle { hostState.currentSnackbarData?.dismiss() }

        composeRule.waitForIdle()
        assertEquals(1, dismissals)
    }
}
