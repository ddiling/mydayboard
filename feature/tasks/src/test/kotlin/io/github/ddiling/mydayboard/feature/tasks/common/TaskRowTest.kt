package io.github.ddiling.mydayboard.feature.tasks.common

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import io.github.ddiling.mydayboard.core.designsystem.preview.PreviewSurface
import io.github.ddiling.mydayboard.core.domain.task.Repeat
import io.github.ddiling.mydayboard.core.domain.task.RowState
import io.github.ddiling.mydayboard.core.domain.task.TaskRow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
class TaskRowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val pending = rowOf(taskOf(Repeat.EveryDay, LocalTime.of(8, 30)), RowState.PENDING)

    private fun show(
        row: TaskRow,
        onClick: () -> Unit = {},
    ) = composeRule.setContent { PreviewSurface { TaskRowLine(row, textsToday, onClick) } }

    @Test
    fun `tapping a row calls onClick`() {
        var clicks = 0
        show(pending, onClick = { clicks++ })

        composeRule.onNodeWithTag("tasks_list_row_t1").performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun `a row shows the name and the schedule under it`() {
        show(pending)

        composeRule.onNode(hasText("Vacuum")).assertIsDisplayed()
        composeRule.onNode(hasText("08:30")).assertIsDisplayed()
    }

    @Test
    fun `a count goal row shows its progress at the right`() {
        show(pending.copy(done = 1, target = 3))

        composeRule.onNode(hasText("1/3")).assertIsDisplayed()
    }

    @Test
    fun `a done row has a tick that says done`() {
        show(pending.copy(state = RowState.DONE, done = 1, target = 1))

        composeRule.onNode(hasContentDescription("Done"), useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `a row that is not done has no tick`() {
        show(pending)

        composeRule.onNode(hasContentDescription("Done"), useUnmergedTree = true).assertDoesNotExist()
    }
}
