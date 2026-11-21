package io.github.ddiling.mydayboard.feature.trackers.reason

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerReason
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class ReasonPromptContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val sleepBetter = TrackerReason("r1", "t1", "I sleep better", Instant.EPOCH, Instant.EPOCH)
    private val counterWithTwo =
        ReasonPrompt.WithReason(
            "t1",
            "Water",
            ReasonPromptKind.COUNTER,
            sleepBetter,
            canSwap = true,
        )

    private fun show(
        prompt: ReasonPrompt,
        actions: ReasonPromptActions = ReasonPromptActions(),
    ) = composeRule.setContent { ReasonPromptContent(prompt, actions) }

    @Test
    fun `the counter dialog shows the reason in quotes with wait and log anyway`() {
        show(counterWithTwo)

        composeRule.onNodeWithText("“I sleep better”").assertIsDisplayed()
        composeRule.onNodeWithText("I'll wait").assertIsDisplayed()
        composeRule.onNodeWithText("Log anyway").assertIsDisplayed()
    }

    @Test
    fun `the days since dialog shows hold on and reset anyway`() {
        show(counterWithTwo.copy(kind = ReasonPromptKind.DAYS_SINCE))

        composeRule.onNodeWithText("I'll hold on").assertIsDisplayed()
        composeRule.onNodeWithText("Reset anyway").assertIsDisplayed()
    }

    @Test
    fun `another reason is hidden with one reason`() {
        show(counterWithTwo.copy(canSwap = false))

        composeRule.onNodeWithTag("trackers_reason_dialog_another").assertDoesNotExist()
    }

    @Test
    fun `the buttons are at least 48 dp high`() {
        show(counterWithTwo)

        composeRule.onNodeWithTag("trackers_reason_dialog_big").assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithTag("trackers_reason_dialog_log").assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun `each button calls its own callback`() {
        val calls = mutableListOf<String>()
        show(
            counterWithTwo,
            ReasonPromptActions(
                onWait = { calls += "wait" },
                onAnother = { calls += "another" },
                onLogAnyway = { calls += "log" },
            ),
        )

        composeRule.onNodeWithTag("trackers_reason_dialog_another").performClick()
        composeRule.onNodeWithTag("trackers_reason_dialog_big").performClick()
        composeRule.onNodeWithTag("trackers_reason_dialog_log").performClick()

        assertEquals(listOf("another", "wait", "log"), calls)
    }

    @Test
    fun `with no reasons the big button adds the first reason of that tracker`() {
        val added = mutableListOf<String>()
        val waited = mutableListOf<String>()
        show(
            ReasonPrompt.NoReasons("t1", "Water", ReasonPromptKind.COUNTER),
            ReasonPromptActions(onWait = { waited += "wait" }, onAddFirstReason = { added += it }),
        )

        composeRule.onNodeWithText("Add your first reason").performClick()

        assertEquals(listOf("t1"), added)
        assertEquals(emptyList<String>(), waited)
    }
}
