package io.github.ddiling.mydayboard.feature.tasks.common

import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.ddiling.mydayboard.core.designsystem.preview.PreviewSurface
import io.github.ddiling.mydayboard.core.domain.task.TickOutcome
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
class TaskUndoSnackbarTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val hostState = SnackbarHostState()
    private var undone = 0
    private val dismissed = mutableListOf<UndoOffer>()

    private fun added(
        done: Int,
        target: Int,
        slot: LocalTime? = null,
    ) = TickOutcome.Added("t1", "tick1", done, target, slot)

    private fun show(offer: () -> UndoOffer?) =
        composeRule.setContent {
            PreviewSurface {
                SnackbarHost(hostState)
                TaskUndoSnackbar(offer(), hostState, onUndo = { undone++ }, onDismissed = { dismissed += it })
            }
        }

    private fun messageOf(offer: UndoOffer): String {
        var message = ""
        composeRule.setContent { message = undoMessage(offer) }
        composeRule.waitForIdle()
        return message
    }

    @Test
    fun `a plain task says done`() {
        assertEquals("Breakfast done", messageOf(UndoOffer("Breakfast", added(1, 1))))
    }

    @Test
    fun `a count goal that is not finished says its progress`() {
        assertEquals("Send CVs 2/3", messageOf(UndoOffer("Send CVs", added(2, 3))))
    }

    @Test
    fun `a count goal that is finished says its progress and done`() {
        assertEquals("Send CVs 3/3 · done", messageOf(UndoOffer("Send CVs", added(3, 3))))
    }

    @Test
    fun `one of several times says which time is done`() {
        val offer = UndoOffer("Litter boxes", added(1, 3, LocalTime.of(20, 0)))

        assertEquals("Litter boxes 20:00 done", messageOf(offer))
    }

    @Test
    fun `tapping Undo calls onUndo`() {
        show { UndoOffer("Breakfast", added(1, 1)) }

        composeRule.onNodeWithText("Undo").performClick()

        composeRule.waitForIdle()
        assertEquals(1, undone)
    }

    @Test
    fun `a dismissed message reports the offer it showed`() {
        val offer = UndoOffer("Breakfast", added(1, 1))
        show { offer }
        composeRule.waitForIdle()

        composeRule.runOnIdle { hostState.currentSnackbarData?.dismiss() }

        composeRule.waitForIdle()
        assertEquals(listOf(offer), dismissed)
    }

    @Test
    fun `a newer offer replaces the older one without reporting it dismissed`() {
        var offer by mutableStateOf(UndoOffer("Breakfast", added(1, 1)))
        show { offer }
        composeRule.waitForIdle()

        offer = UndoOffer("Pills", added(1, 1))
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Pills done").assertIsDisplayed()
        assertEquals(emptyList<UndoOffer>(), dismissed)
    }
}
