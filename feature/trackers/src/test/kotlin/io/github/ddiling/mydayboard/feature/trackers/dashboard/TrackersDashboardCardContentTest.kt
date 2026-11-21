package io.github.ddiling.mydayboard.feature.trackers.dashboard

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import io.github.ddiling.mydayboard.core.dashboard.DashboardHost
import io.github.ddiling.mydayboard.core.navigation.Destination
import io.github.ddiling.mydayboard.feature.trackers.detail.Undo
import io.github.ddiling.mydayboard.feature.trackers.detail.UndoKind
import io.github.ddiling.mydayboard.feature.trackers.navigation.TrackerDetailDestination
import io.github.ddiling.mydayboard.feature.trackers.navigation.TrackerEditorDestination
import io.github.ddiling.mydayboard.feature.trackers.navigation.TrackersListDestination
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
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

    private fun daysSince(days: Int) = TrackersDashboardRow.DaysSince("sugar-id", "No sugar", days)

    private val sugar = daysSince(days = 3)
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
        totalCount: Int = rows.size,
        pendingUndo: Undo? = null,
    ) {
        composeRule.setContent {
            val state = TrackersDashboardCardUiState.Content(rows, totalCount, pendingUndo)
            TrackersDashboardCardContent(state, actions, host)
            SnackbarHost(host.snackbarHostState)
        }
    }

    private fun assertEmptySlots(vararg slots: Int) {
        (0 until MAX_PLACES).forEach { slot ->
            val node = composeRule.onNodeWithTag("empty_slot_$slot")
            if (slot in slots) node.assertExists() else node.assertDoesNotExist()
        }
    }

    @Test
    fun `with 2 trackers two empty add tiles are shown`() {
        givenCard(listOf(water, sugar))

        assertEmptySlots(2, 3)
    }

    @Test
    fun `with 0 trackers four empty add tiles are shown`() {
        givenCard(emptyList())

        assertEmptySlots(0, 1, 2, 3)
    }

    @Test
    fun `counter tile text reads 3 today and the time since the last event`() {
        givenCard(listOf(TrackersDashboardRow.Counter("water-id", "Water", 3, Duration.ofHours(2))))

        composeRule.onNodeWithText("3 today · 2 h ago").assertExists()
    }

    @Test
    fun `counter tile shows only 3 today when it has no event`() {
        givenCard(listOf(TrackersDashboardRow.Counter("water-id", "Water", 3, sinceLast = null)))

        composeRule.onNodeWithText("3 today").assertExists()
    }

    @Test
    fun `days-since tile reads 1 day`() {
        givenCard(listOf(daysSince(days = 1)))

        composeRule.onNodeWithText("1 day").assertExists()
    }

    @Test
    fun `days-since tile reads 5 days`() {
        givenCard(listOf(daysSince(days = 5)))

        composeRule.onNodeWithText("5 days").assertExists()
    }

    @Test
    @Config(qualifiers = "pl")
    fun `Polish days-since tile uses the one form for 1`() {
        givenCard(listOf(daysSince(days = 1)))

        composeRule.onNodeWithText("1 dzień").assertExists()
    }

    @Test
    @Config(qualifiers = "pl")
    fun `Polish days-since tile uses the few form for 2`() {
        givenCard(listOf(daysSince(days = 2)))

        composeRule.onNodeWithText("2 dni").assertExists()
    }

    @Test
    @Config(qualifiers = "pl")
    fun `Polish days-since tile uses the many form for 5`() {
        givenCard(listOf(daysSince(days = 5)))

        composeRule.onNodeWithText("5 dni").assertExists()
    }

    @Test
    fun `tapping a counter tile adds one event`() {
        givenCard(listOf(water, sugar))

        composeRule.onNodeWithTag("trackers_card_tile_water-id").performClick()

        assertEquals(listOf("water-id"), plusOnes)
    }

    @Test
    fun `tapping a days-since tile resets it`() {
        givenCard(listOf(water, sugar))

        composeRule.onNodeWithTag("trackers_card_tile_sugar-id").performClick()

        assertEquals(listOf("sugar-id"), resets)
    }

    @Test
    fun `long-pressing a tile opens the detail screen and adds no event`() {
        givenCard(listOf(water, sugar))

        composeRule.onNodeWithTag("trackers_card_tile_sugar-id").performTouchInput { longClick() }

        assertEquals(listOf<Destination>(TrackerDetailDestination("sugar-id")), host.navigated)
        assertEquals(emptyList<String>(), plusOnes + resets)
    }

    @Test
    fun `tapping the title opens the list`() {
        givenCard(listOf(water))

        composeRule.onNodeWithTag("open_trackers").performClick()

        assertEquals(listOf<Destination>(TrackersListDestination), host.navigated)
    }

    @Test
    fun `tapping plus opens the editor with no tracker id`() {
        givenCard(listOf(water))

        composeRule.onNodeWithTag("add_tracker").performClick()

        assertEquals(listOf<Destination>(TrackerEditorDestination()), host.navigated)
    }

    @Test
    fun `tapping an empty tile opens the editor`() {
        givenCard(listOf(water))

        composeRule.onNodeWithTag("empty_slot_1").performClick()

        assertEquals(listOf<Destination>(TrackerEditorDestination()), host.navigated)
    }

    @Test
    fun `the title shows how many trackers exist, not how many have a tile`() {
        givenCard(listOf(water), totalCount = 7)

        composeRule.onNodeWithTag("open_trackers").assertTextContains("7", substring = true)
    }

    @Test
    fun `an added action shows the Added snackbar`() {
        givenCard(listOf(water), pendingUndo = Undo("event-1", UndoKind.ADDED))

        composeRule.onNodeWithText("Added").assertExists()
    }

    @Test
    fun `a reset action shows the Reset snackbar`() {
        // The tile reads "2 today", so the only "Reset" text on screen is the snackbar message.
        givenCard(listOf(water), pendingUndo = Undo("event-1", UndoKind.RESET))

        composeRule.onNodeWithText("Reset").assertExists()
    }

    @Test
    fun `the snackbar's Undo action calls onUndo`() {
        givenCard(listOf(water), pendingUndo = Undo("event-1", UndoKind.ADDED))

        composeRule.onNodeWithText("Undo").performClick()

        assertEquals(1, undos)
    }

    @Test
    fun `opening the detail dismisses the pending undo first`() {
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

        composeRule.onNodeWithTag("trackers_card_tile_water-id").performTouchInput { longClick() }

        assertEquals(listOf("dismissed", "navigated"), orderedEvents)
    }
}
