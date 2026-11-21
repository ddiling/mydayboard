package io.github.ddiling.mydayboard.feature.trackers.editor

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerType
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class TrackerEditorReasonsSectionTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val today = LocalDate.of(2026, 3, 2)

    private fun state(
        isEditing: Boolean = false,
        askForReason: Boolean = false,
        reasonCount: Int = 0,
        type: TrackerType = TrackerType.EVENT_COUNTER,
    ) = TrackerEditorUiState(
        load = EditorLoad.Ready,
        isEditing = isEditing,
        type = type,
        askForReason = askForReason,
        reasonCount = reasonCount,
        startDate = today,
        today = today,
    )

    private fun show(
        state: TrackerEditorUiState,
        actions: TrackerEditorActions = TrackerEditorActions(),
    ) = composeRule.setContent { TrackerEditorScreen(state, actions) }

    @Test
    fun `the switch row is one toggleable element that shows its state`() {
        show(state(askForReason = true))

        composeRule.onNodeWithText("Ask me for a reason before logging").assertIsOn()
    }

    @Test
    fun `tapping the switch row reports the opposite value`() {
        val changes = mutableListOf<Boolean>()
        show(state(askForReason = false), TrackerEditorActions(onAskForReasonChange = { changes += it }))

        composeRule.onNodeWithText("Ask me for a reason before logging").performClick()

        assertEquals(listOf(true), changes)
    }

    @Test
    fun `the hint for a counter mentions the plus one`() {
        show(state(type = TrackerType.EVENT_COUNTER))

        composeRule.onNodeWithText("Before +1 you see a reason.").assertIsDisplayed()
    }

    @Test
    fun `the hint for days since mentions the reset`() {
        show(state(type = TrackerType.DAYS_SINCE))

        composeRule.onNodeWithText("Before Reset you see a reason.").assertIsDisplayed()
    }

    @Test
    fun `a new tracker has no reasons row`() {
        show(state(isEditing = false, reasonCount = 2))

        composeRule.onNodeWithText("2 reasons").assertDoesNotExist()
    }

    @Test
    fun `the reasons row shows one reason in singular`() {
        show(state(isEditing = true, reasonCount = 1))

        composeRule.onNodeWithText("1 reason").assertIsDisplayed()
    }

    @Test
    fun `the reasons row shows the plural count`() {
        show(state(isEditing = true, reasonCount = 2))

        composeRule.onNodeWithText("2 reasons").assertIsDisplayed()
    }

    @Test
    fun `the reasons row says there are none yet for zero`() {
        show(state(isEditing = true, reasonCount = 0))

        composeRule.onNodeWithText("No reasons yet").assertIsDisplayed()
    }

    @Test
    fun `tapping the reasons row opens the reasons`() {
        var opened = 0
        show(state(isEditing = true, reasonCount = 2), TrackerEditorActions(onOpenReasons = { opened++ }))

        composeRule.onNodeWithText("2 reasons").performClick()

        assertEquals(1, opened)
    }
}
