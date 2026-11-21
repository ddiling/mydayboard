package io.github.ddiling.mydayboard.feature.trackers.dashboard

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.ddiling.mydayboard.core.dashboard.DashboardHost
import io.github.ddiling.mydayboard.core.navigation.Destination
import io.github.ddiling.mydayboard.feature.trackers.detail.Undo
import io.github.ddiling.mydayboard.feature.trackers.detail.UndoKind
import io.github.ddiling.mydayboard.feature.trackers.navigation.TrackerDetailDestination
import io.github.ddiling.mydayboard.feature.trackers.navigation.TrackerEditorDestination
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Duration

private class RecordingHost : DashboardHost {
    override val snackbarHostState = SnackbarHostState()
    val navigated = mutableListOf<Destination>()

    override fun navigate(destination: Destination) {
        navigated += destination
    }
}

@RunWith(RobolectricTestRunner::class)
class TrackersDashboardCardContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val host = RecordingHost()
    private val water = TrackersDashboardRow.Counter("water-id", "Water", todayCount = 2, sinceLast = Duration.ZERO)
    private val sugar = TrackersDashboardRow.DaysSince("sugar-id", "No sugar", days = 3)
    private val plusOnes = mutableListOf<String>()
    private val resets = mutableListOf<String>()
    private var undos = 0

    private val actions =
        TrackersDashboardCardActions(
            onPlusOne = { plusOnes += it },
            onReset = { resets += it },
            onUndo = { undos++ },
        )

    private fun givenCard(
        rows: List<TrackersDashboardRow>,
        pendingUndo: Undo? = null,
    ) {
        composeRule.setContent {
            val state = TrackersDashboardCardUiState.Content(rows, rows.size, pendingUndo)
            TrackersDashboardCardContent(state, actions, host)
            SnackbarHost(host.snackbarHostState)
        }
    }

    @Test
    fun `plus one calls back with the id of its tracker`() {
        givenCard(listOf(water, sugar))

        composeRule.onNodeWithContentDescription("Add one to Water").performClick()

        assertEquals(listOf("water-id"), plusOnes)
    }

    @Test
    fun `reset calls back with the id of its tracker`() {
        givenCard(listOf(water, sugar))

        composeRule.onNodeWithContentDescription("Reset No sugar").performClick()

        assertEquals(listOf("sugar-id"), resets)
    }

    @Test
    fun `clicking a row opens that tracker's detail`() {
        givenCard(listOf(water, sugar))

        composeRule.onNodeWithTag("trackers_card_row_sugar-id").performClick()

        assertEquals(listOf<Destination>(TrackerDetailDestination("sugar-id")), host.navigated)
    }

    @Test
    fun `the empty state add button opens the editor for a new tracker`() {
        givenCard(emptyList())

        composeRule.onNodeWithText("Add tracker").performClick()

        assertEquals(listOf<Destination>(TrackerEditorDestination()), host.navigated)
    }

    @Test
    fun `an added action shows the Added snackbar`() {
        givenCard(listOf(water), Undo("event-1", UndoKind.ADDED))

        composeRule.onNodeWithText("Added").assertExists()
    }

    @Test
    fun `a reset action shows the Reset snackbar`() {
        // A counter row has no Reset button, so the only "Reset" text on screen is the snackbar message.
        givenCard(listOf(water), Undo("event-1", UndoKind.RESET))

        composeRule.onNodeWithText("Reset").assertExists()
    }

    @Test
    fun `the snackbar's Undo action calls onUndo`() {
        givenCard(listOf(water), Undo("event-1", UndoKind.ADDED))

        composeRule.onNodeWithText("Undo").performClick()

        assertEquals(1, undos)
    }

    @Test
    fun `clicking a row dismisses the pending undo before opening the detail`() {
        val orderedEvents = mutableListOf<String>()
        val orderedHost =
            object : DashboardHost {
                override val snackbarHostState = SnackbarHostState()

                override fun navigate(destination: Destination) {
                    orderedEvents += "navigated"
                }
            }
        composeRule.setContent {
            TrackersDashboardCardContent(
                TrackersDashboardCardUiState.Content(listOf(water), 1, Undo("event-1", UndoKind.ADDED)),
                actions.copy(onUndoDismissed = { orderedEvents += "dismissed" }),
                orderedHost,
            )
        }

        composeRule.onNodeWithTag("trackers_card_row_water-id").performClick()

        assertEquals(listOf("dismissed", "navigated"), orderedEvents)
    }
}
