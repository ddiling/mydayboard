package io.github.ddiling.mydayboard.feature.tasks.common

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import io.github.ddiling.mydayboard.core.designsystem.preview.PreviewSurface
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val DRAG_PX = 120f

@RunWith(RobolectricTestRunner::class)
class SwipeToDoneRowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var swipes = 0

    private fun show(enabled: Boolean = true) =
        composeRule.setContent {
            PreviewSurface {
                SwipeToDoneRow(onDone = { swipes++ }, enabled = enabled) {
                    Text("Vacuum", Modifier.fillMaxWidth().height(48.dp).testTag("row"))
                }
            }
        }

    @Test
    fun `swiping right calls onDone`() {
        show()

        composeRule.onNodeWithTag("row").performTouchInput { swipeRight() }

        composeRule.waitForIdle()
        assertEquals(1, swipes)
    }

    @Test
    fun `the row springs back so a second swipe counts again`() {
        show()

        composeRule.onNodeWithTag("row").performTouchInput { swipeRight() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("row").performTouchInput { swipeRight() }

        composeRule.waitForIdle()
        assertEquals(2, swipes)
    }

    @Test
    fun `swiping left does nothing`() {
        show()

        composeRule.onNodeWithTag("row").performTouchInput { swipeLeft() }

        composeRule.waitForIdle()
        assertEquals(0, swipes)
    }

    @Test
    fun `a disabled row ignores the swipe`() {
        show(enabled = false)

        composeRule.onNodeWithTag("row").performTouchInput { swipeRight() }

        composeRule.waitForIdle()
        assertEquals(0, swipes)
    }

    @Test
    fun `the Done label is hidden until the row is dragged`() {
        show()

        composeRule.onNodeWithText("Done").assertDoesNotExist()
    }

    @Test
    fun `dragging right shows the Done label`() {
        show()

        composeRule.onNodeWithTag("row").performTouchInput {
            down(center)
            moveBy(Offset(DRAG_PX, 0f))
        }

        composeRule.onNodeWithText("Done").assertIsDisplayed()
    }

    @Test
    fun `screen readers get a Mark done action`() {
        show()

        val withActions = composeRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions))
        val action = withActions.fetchSemanticsNode().config[SemanticsActions.CustomActions].single()
        composeRule.runOnIdle { action.action() }

        assertEquals("Mark done", action.label)
        assertEquals(1, swipes)
    }
}
