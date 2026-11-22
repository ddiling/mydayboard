package io.github.ddiling.mydayboard.feature.tasks.form

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import io.github.ddiling.mydayboard.core.domain.task.TaskFormError
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class TaskFormScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val today = LocalDate.of(2026, 3, 2)

    private val blank = TaskFormUiState(today = today)

    private fun show(
        state: TaskFormUiState,
        actions: TaskFormActions = TaskFormActions(),
    ) = composeRule.setContent { TaskFormScreen(state, actions) }

    @Test
    fun `save is disabled while the form is invalid`() {
        show(blank.copy(canSave = false))

        composeRule.onNodeWithTag("tasks_form_save").assertIsNotEnabled()
    }

    @Test
    fun `save is enabled when the form is valid`() {
        show(blank.copy(name = "Pills", canSave = true))

        composeRule.onNodeWithTag("tasks_form_save").assertIsEnabled()
    }

    @Test
    fun `tapping save calls onSave`() {
        var saves = 0
        show(blank.copy(name = "Pills", canSave = true), TaskFormActions(onSave = { saves++ }))

        composeRule.onNodeWithTag("tasks_form_save").performClick()

        assertEquals(1, saves)
    }

    @Test
    fun `the close button calls onClose`() {
        var closes = 0
        show(blank, TaskFormActions(onClose = { closes++ }))

        composeRule.onNodeWithContentDescription("Close").performClick()

        assertEquals(1, closes)
    }

    @Test
    fun `the back button calls onClose`() {
        var closes = 0
        show(blank, TaskFormActions(onClose = { closes++ }))

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }

        assertEquals(1, closes)
    }

    @Test
    fun `typing in the name field reports the typed text`() {
        val typed = mutableListOf<String>()
        show(blank, TaskFormActions(onNameChange = { typed += it }))

        composeRule.onNodeWithTag("tasks_form_name").performTextInput("Pills")

        assertEquals(listOf("Pills"), typed)
    }

    @Test
    fun `a blank name shows its error`() {
        show(blank.copy(name = " ", errors = listOf(TaskFormError.BlankName)))

        composeRule.onNodeWithText("Enter a name").assertIsDisplayed()
    }

    @Test
    fun `the name input is named by its label and carries the error`() {
        show(blank.copy(name = " ", errors = listOf(TaskFormError.BlankName)))

        composeRule
            .onNodeWithContentDescription("Name")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, "Enter a name"))
    }

    @Test
    fun `an untouched empty name shows no error`() {
        show(blank.copy(errors = listOf(TaskFormError.BlankName)))

        composeRule.onNodeWithText("Enter a name").assertDoesNotExist()
    }

    @Test
    fun `a task that was not found says so`() {
        show(blank.copy(load = FormLoad.NotFound))

        composeRule.onNodeWithText("Task not found").assertIsDisplayed()
    }

    @Test
    fun `the form is scrollable and its last field is reachable at double font size`() {
        RuntimeEnvironment.setFontScale(2f)
        show(blank)

        composeRule.onNodeWithTag("tasks_form_name").performScrollTo().assertIsDisplayed()
    }
}
