package io.github.ddiling.mydayboard.feature.trackers.detail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.ddiling.mydayboard.core.domain.tracker.Trend
import io.github.ddiling.mydayboard.feature.trackers.event.EntryMode
import io.github.ddiling.mydayboard.feature.trackers.event.EventTimeEditorState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.LocalTime

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
    fun `undoing a deleted entry is worded as the entry deleted`() {
        val pending = counter.copy(pendingUndo = Undo("event-1", UndoKind.DELETED))
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { TrackerDetailScreen(pending, TrackerDetailActions()) }
        composeRule.mainClock.advanceTimeByFrame()

        composeRule.onNodeWithText("Entry deleted").assertIsDisplayed()
    }

    @Test
    fun `the add past entry action calls onAddPastEntry`() {
        var adds = 0
        composeRule.setContent { TrackerDetailScreen(counter, TrackerDetailActions(onAddPastEntry = { adds++ })) }

        composeRule.onNodeWithContentDescription("Add past entry").performClick()

        assertEquals(1, adds)
    }

    @Test
    fun `tapping a history row calls onEditEntry with that event id`() {
        val edited = mutableListOf<String>()
        val day = HistoryDay(LocalDate.of(2026, 3, 2), listOf(HistoryEntry("e7", LocalTime.of(9, 30))))
        val actions = TrackerDetailActions(onEditEntry = { edited += it })
        composeRule.setContent { TrackerDetailScreen(counter.copy(history = listOf(day)), actions) }

        composeRule.onNodeWithTag("trackers_detail_entry_e7").performClick()

        assertEquals(listOf("e7"), edited)
    }

    @Test
    fun `tapping a broken streak calls onEditEntry with the reset that ended it`() {
        val edited = mutableListOf<String>()
        val streak = BrokenStreak(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 20), 19, endedByEventId = "r2")
        composeRule.setContent {
            TrackerDetailScreen(
                daysSince.copy(brokenStreaks = listOf(streak)),
                TrackerDetailActions(onEditEntry = { edited += it }),
            )
        }

        composeRule.onNodeWithTag("trackers_detail_entry_r2").performClick()

        assertEquals(listOf("r2"), edited)
    }

    @Test
    fun `an open editor shows the entry dialog`() {
        val editor = EventTimeEditorState(EntryMode.Add, LocalDate.of(2026, 3, 2), LocalTime.of(8, 15), null)
        composeRule.setContent { TrackerDetailScreen(counter.copy(editor = editor), TrackerDetailActions()) }

        composeRule.onNodeWithTag("trackers_event_save").assertIsDisplayed()
    }

    private val daysSince =
        TrackerDetailUiState.DaysSince(
            name = "No sugar",
            currentDays = 3,
            currentStart = LocalDate.of(2026, 3, 2),
            longestDays = 5,
            brokenStreaks = emptyList<BrokenStreak>(),
            pendingUndo = null,
        )

    @Test
    fun `the reset button calls onReset`() {
        var resets = 0
        composeRule.setContent { TrackerDetailScreen(daysSince, TrackerDetailActions(onReset = { resets++ })) }

        composeRule.onNodeWithTag("trackers_detail_reset").performClick()

        assertEquals(1, resets)
    }

    @Test
    fun `a pending reset undo shows a reset snackbar whose action calls onUndo`() {
        var undos = 0
        val pending = daysSince.copy(pendingUndo = Undo("event-1", UndoKind.RESET))
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { TrackerDetailScreen(pending, TrackerDetailActions(onUndo = { undos++ })) }
        composeRule.mainClock.advanceTimeByFrame()

        // The button is also labelled "Reset", so the snackbar message is the other node with that text.
        composeRule.onNode(hasText("Reset") and hasTestTag("trackers_detail_reset").not()).assertIsDisplayed()
        composeRule.onNodeWithText("Undo").performClick()

        assertEquals(1, undos)
    }

    @Test
    fun `the edit action works for a days since tracker`() {
        var edits = 0
        composeRule.setContent { TrackerDetailScreen(daysSince, TrackerDetailActions(onEditClick = { edits++ })) }

        composeRule.onNodeWithText("Edit").performClick()

        assertEquals(1, edits)
    }

    @Test
    fun `the days are stated in words`() {
        composeRule.setContent { TrackerDetailScreen(daysSince, TrackerDetailActions()) }

        composeRule.onNodeWithText("3 days").assertIsDisplayed()
    }

    @Test
    fun `the trend is stated in words`() {
        composeRule.setContent { TrackerDetailScreen(counter, TrackerDetailActions()) }

        composeRule.onNodeWithText("Same as the week before").assertIsDisplayed()
    }
}
