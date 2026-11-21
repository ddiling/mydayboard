package io.github.ddiling.mydayboard.feature.trackers.reasons

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerReason
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class TrackerReasonsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun reason(
        id: String,
        text: String,
    ) = TrackerReason(id, "tracker-1", text, Instant.EPOCH, Instant.EPOCH)

    private val content =
        TrackerReasonsUiState.Content(
            "No sweets",
            listOf(reason("r1", "I sleep better"), reason("r2", "I save money")),
        )

    @Test
    fun `the add button calls onAddClick`() {
        var addClicks = 0
        composeRule.setContent { TrackerReasonsScreen(content, TrackerReasonsActions(onAddClick = { addClicks++ })) }

        composeRule.onNodeWithText("Add reason").performClick()

        assertEquals(1, addClicks)
    }

    @Test
    fun `tapping a row calls onEditClick with its id`() {
        val edited = mutableListOf<String>()
        composeRule.setContent { TrackerReasonsScreen(content, TrackerReasonsActions(onEditClick = { edited += it })) }

        composeRule.onNodeWithText("I save money").performClick()

        assertEquals(listOf("r2"), edited)
    }

    @Test
    fun `the delete accessibility action calls onDelete with the row id`() {
        val deleted = mutableListOf<String>()
        composeRule.setContent { TrackerReasonsScreen(content, TrackerReasonsActions(onDelete = { deleted += it })) }

        val node = composeRule.onNodeWithText("I sleep better").fetchSemanticsNode()
        val delete = node.config[SemanticsActions.CustomActions].first { it.label == "Delete" }
        composeRule.runOnUiThread { delete.action() }

        assertEquals(listOf("r1"), deleted)
    }

    @Test
    fun `the empty state shows the hint`() {
        composeRule.setContent { TrackerReasonsScreen(content.copy(reasons = emptyList()), TrackerReasonsActions()) }

        composeRule.onNodeWithText("No reasons yet. Add one that helps you.").assertIsDisplayed()
    }

    @Test
    fun `save is disabled while the field is blank`() {
        val editor = ReasonEditor(reasonId = null, text = "")
        composeRule.setContent { TrackerReasonsScreen(content.copy(editor = editor), TrackerReasonsActions()) }

        composeRule.onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test
    fun `every row can be reached with a large font`() {
        val many = (1..12).map { reason("r$it", "Reason number $it") }
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 2f)) {
                TrackerReasonsScreen(content.copy(reasons = many), TrackerReasonsActions())
            }
        }

        composeRule.onNodeWithTag("trackers_reasons_list").performScrollToNode(hasText("Reason number 12"))

        composeRule.onNodeWithText("Reason number 12").assertIsDisplayed()
    }
}
