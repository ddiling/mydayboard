package io.github.ddiling.mydayboard.feature.trackers.detail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.ddiling.mydayboard.core.domain.tracker.Trend
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TrackerDetailScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val counter =
        TrackerDetailUiState.Counter(
            name = "Water",
            todayCount = 3,
            sinceLast = null,
            lastDays = emptyList(),
            trend = Trend.FLAT,
            history = emptyList(),
            pendingUndo = null,
        )

    @Test
    fun `the plus one button calls onPlusOne`() {
        var plusOnes = 0
        composeRule.setContent { TrackerDetailScreen(counter, TrackerDetailActions(onPlusOne = { plusOnes++ })) }

        composeRule.onNodeWithText("+1").performClick()

        assertEquals(1, plusOnes)
    }

    @Test
    fun `the edit action calls onEditClick`() {
        var edits = 0
        composeRule.setContent { TrackerDetailScreen(counter, TrackerDetailActions(onEditClick = { edits++ })) }

        composeRule.onNodeWithText("Edit").performClick()

        assertEquals(1, edits)
    }

    @Test
    fun `the edit action clears the pending undo so it does not come back after returning`() {
        var dismissals = 0
        val pending = counter.copy(pendingUndo = Undo("event-1", UndoKind.ADDED))
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            TrackerDetailScreen(
                pending,
                TrackerDetailActions(onUndoDismissed = { dismissals++ }),
            )
        }
        composeRule.mainClock.advanceTimeByFrame()

        composeRule.onNodeWithText("Edit").performClick()

        assertEquals(1, dismissals)
    }

    @Test
    fun `a pending undo shows a snackbar whose action calls onUndo`() {
        var undos = 0
        val pending = counter.copy(pendingUndo = Undo("event-1", UndoKind.ADDED))
        // The snackbar times out on virtual time, which the test clock would otherwise skip while idling.
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { TrackerDetailScreen(pending, TrackerDetailActions(onUndo = { undos++ })) }
        composeRule.mainClock.advanceTimeByFrame()

        composeRule.onNodeWithText("Added").assertIsDisplayed()
        composeRule.onNodeWithText("Undo").performClick()

        assertEquals(1, undos)
    }

    @Test
    fun `the trend is stated in words`() {
        composeRule.setContent { TrackerDetailScreen(counter, TrackerDetailActions()) }

        composeRule.onNodeWithText("Same as the week before").assertIsDisplayed()
    }
}
