package io.github.ddiling.mydayboard.feature.trackers.list

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TrackersListScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val content =
        TrackersListUiState.Content(
            listOf(TrackerListItem.DaysSince(id = "tracker-1", name = "No sugar", days = 3)),
        )

    @Test
    fun `the add button calls onAddClick`() {
        var addClicks = 0
        composeRule.setContent { TrackersListScreen(content, onTrackerClick = {}, onAddClick = { addClicks++ }) }

        composeRule.onNodeWithText("Add tracker").performClick()

        assertEquals(1, addClicks)
    }

    @Test
    fun `clicking a tracker calls onTrackerClick with its id`() {
        val clicked = mutableListOf<String>()
        composeRule.setContent { TrackersListScreen(content, onTrackerClick = { clicked += it }, onAddClick = {}) }

        composeRule.onNodeWithText("No sugar").performClick()

        assertEquals(listOf("tracker-1"), clicked)
    }

    @Test
    fun `the empty state shows the add button`() {
        composeRule.setContent { TrackersListScreen(TrackersListUiState.Empty, onTrackerClick = {}, onAddClick = {}) }

        composeRule.onNodeWithText("Add tracker").assertIsDisplayed()
    }
}
