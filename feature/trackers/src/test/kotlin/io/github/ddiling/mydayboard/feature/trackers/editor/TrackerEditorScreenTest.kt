package io.github.ddiling.mydayboard.feature.trackers.editor

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.github.ddiling.mydayboard.core.domain.tracker.MAX_TRACKER_NAME_LENGTH
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class TrackerEditorScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val today = LocalDate.of(2026, 3, 2)

    private fun state(
        name: String = "",
        nameEdited: Boolean = false,
        isEditing: Boolean = false,
        showDeleteConfirmation: Boolean = false,
    ) = TrackerEditorUiState(
        load = EditorLoad.Ready,
        isEditing = isEditing,
        name = name,
        nameEdited = nameEdited,
        startDate = today,
        today = today,
        showDeleteConfirmation = showDeleteConfirmation,
    )

    @Test
    fun `typing in the name field reports the typed text`() {
        val typed = mutableListOf<String>()
        composeRule.setContent {
            TrackerEditorScreen(state(), TrackerEditorActions(onNameChange = { typed += it }))
        }

        composeRule.onNodeWithText("Name").performTextInput("Water")

        assertEquals(listOf("Water"), typed)
    }

    @Test
    fun `a blank name shows its error and disables save`() {
        composeRule.setContent {
            TrackerEditorScreen(state(name = " ", nameEdited = true), TrackerEditorActions())
        }

        composeRule.onNodeWithText("Enter a name").assertIsDisplayed()
        composeRule.onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test
    fun `a too long name shows the limit`() {
        val name = "x".repeat(MAX_TRACKER_NAME_LENGTH + 1)
        composeRule.setContent { TrackerEditorScreen(state(name, true), TrackerEditorActions()) }

        composeRule.onNodeWithText("Name is too long (max $MAX_TRACKER_NAME_LENGTH)").assertIsDisplayed()
    }

    @Test
    fun `a valid name enables save and clicking it calls onSave`() {
        var saves = 0
        composeRule.setContent {
            TrackerEditorScreen(state(name = "Water", nameEdited = true), TrackerEditorActions(onSave = { saves++ }))
        }

        composeRule.onNodeWithText("Save").assertIsEnabled().performClick()

        assertEquals(1, saves)
    }

    @Test
    fun `confirming the delete dialog calls onDeleteConfirm`() {
        var confirms = 0
        composeRule.setContent {
            TrackerEditorScreen(
                state(name = "Water", isEditing = true, showDeleteConfirmation = true),
                TrackerEditorActions(onDeleteConfirm = { confirms++ }),
            )
        }

        composeRule.onNodeWithText("Delete tracker?").assertIsDisplayed()
        composeRule.onNode(hasText("Delete") and hasAnyAncestor(isDialog())).performClick()

        assertEquals(1, confirms)
    }

    @Test
    fun `cancelling the delete dialog calls onDeleteDismiss`() {
        var dismissals = 0
        composeRule.setContent {
            TrackerEditorScreen(
                state(name = "Water", isEditing = true, showDeleteConfirmation = true),
                TrackerEditorActions(onDeleteDismiss = { dismissals++ }),
            )
        }

        composeRule.onNodeWithText("Cancel").performClick()

        assertEquals(1, dismissals)
    }

    @Test
    fun `the not found state offers a back button`() {
        var backs = 0
        val notFound = state(isEditing = true).copy(load = EditorLoad.NotFound)
        composeRule.setContent { TrackerEditorScreen(notFound, TrackerEditorActions(onBack = { backs++ })) }

        composeRule.onNodeWithText("Tracker not found").assertIsDisplayed()
        composeRule.onNodeWithText("Back").performClick()

        assertEquals(1, backs)
    }
}
