package io.github.ddiling.mydayboard.feature.trackers.event

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
class EventTimeDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val addState = EventTimeEditorState(EntryMode.Add, LocalDate.of(2026, 3, 2), LocalTime.of(8, 15), null)
    private val editState = addState.copy(mode = EntryMode.Edit("e1"))

    private fun show(
        state: EventTimeEditorState,
        actions: EventTimeDialogActions = EventTimeDialogActions(),
        is24Hour: Boolean = true,
    ) = composeRule.setContent { EventTimeDialog(state, is24Hour, actions) }

    @Test
    fun `the dialog shows the picked date and time`() {
        show(addState)

        composeRule.onNodeWithTag("trackers_event_date").assertTextContains("2026", substring = true)
        composeRule.onNodeWithTag("trackers_event_time").assertTextContains("08:15", substring = true)
    }

    @Test
    fun `a 12 hour device shows the time with am or pm`() {
        show(addState, is24Hour = false)

        composeRule.onNodeWithTag("trackers_event_time").assertTextContains("8:15 AM", substring = true)
    }

    @Test
    fun `a future time shows its error text`() {
        show(addState.copy(error = EntryTimeError.IN_FUTURE))

        composeRule.onNodeWithTag("trackers_event_error").assertTextContains("future", substring = true)
    }

    @Test
    fun `a time before the tracker started shows its error text`() {
        show(addState.copy(error = EntryTimeError.BEFORE_TRACKER_START))

        composeRule
            .onNodeWithTag(
                "trackers_event_error",
            ).assertTextContains("before the tracker started", substring = true)
    }

    @Test
    fun `save is disabled while there is an error`() {
        show(addState.copy(error = EntryTimeError.IN_FUTURE))

        composeRule.onNodeWithTag("trackers_event_save").assertIsNotEnabled()
    }

    @Test
    fun `save is enabled without an error and calls onSave`() {
        var saves = 0
        show(addState, EventTimeDialogActions(onSave = { saves++ }))

        composeRule.onNodeWithTag("trackers_event_save").assertIsEnabled().performClick()

        assertEquals(1, saves)
    }

    @Test
    fun `delete is shown when editing and calls onDelete`() {
        var deletes = 0
        show(editState, EventTimeDialogActions(onDelete = { deletes++ }))

        composeRule.onNodeWithTag("trackers_event_delete").performClick()

        assertEquals(1, deletes)
    }

    @Test
    fun `delete is not shown when adding`() {
        show(addState)

        composeRule.onNodeWithTag("trackers_event_delete").assertDoesNotExist()
    }
}
