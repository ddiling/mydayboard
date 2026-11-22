package io.github.ddiling.mydayboard.feature.tasks.form

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import io.github.ddiling.mydayboard.core.domain.task.TaskCategory
import io.github.ddiling.mydayboard.core.domain.task.TaskFormError
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
class TaskFormScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val today = LocalDate.of(2026, 3, 2)
    private val morning = LocalTime.of(8, 0)
    private val evening = LocalTime.of(21, 30)
    private val home = TaskCategory("home", "Home", Instant.EPOCH, Instant.EPOCH)

    private val blank = TaskFormUiState(today = today)

    private fun show(
        state: TaskFormUiState,
        actions: TaskFormActions = TaskFormActions(),
        categories: List<TaskCategory> = emptyList(),
    ) = composeRule.setContent { TaskFormScreen(state, categories, actions) }

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
    fun `the repeat dropdown offers the basic types and choosing one reports it`() {
        val chosen = mutableListOf<RepeatType>()
        show(blank, TaskFormActions(onRepeatTypeChange = { chosen += it }))

        composeRule.onNodeWithTag("tasks_form_repeat").performClick()
        composeRule.onNodeWithText("Several times a day").assertIsDisplayed()
        composeRule.onNodeWithText("Once a month").assertDoesNotExist()
        composeRule.onNodeWithText("Chosen weekdays").performClick()

        assertEquals(listOf(RepeatType.WEEKDAYS), chosen)
    }

    @Test
    fun `the category dropdown lists no category and the categories and choosing one reports its id`() {
        val chosen = mutableListOf<String?>()
        show(blank, TaskFormActions(onCategoryChange = { chosen += it }), listOf(home))

        composeRule.onNodeWithTag("tasks_form_category").performClick()
        composeRule.onAllNodesWithText("No category").assertCountEquals(2)
        composeRule.onNodeWithText("Home").performClick()

        assertEquals(listOf<String?>("home"), chosen)
    }

    @Test
    fun `several times a day shows the times as chips and the x removes one`() {
        val removed = mutableListOf<LocalTime>()
        show(
            blank.copy(repeatType = RepeatType.TIMES_PER_DAY, times = listOf(morning, evening)),
            TaskFormActions(onRemoveTime = { removed += it }),
        )

        composeRule.onNodeWithText("21:30").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Remove 08:00").performClick()

        assertEquals(listOf(morning), removed)
    }

    @Test
    fun `add time opens the clock and confirming reports the picked time`() {
        val added = mutableListOf<LocalTime>()
        show(blank.copy(repeatType = RepeatType.TIMES_PER_DAY), TaskFormActions(onAddTime = { added += it }))

        composeRule.onNodeWithText("Add time").performClick()
        composeRule.onNodeWithText("OK").performClick()

        assertEquals(listOf(DEFAULT_PICKER_TIME), added)
    }

    @Test
    fun `a blank name shows its error`() {
        show(blank.copy(name = " ", errors = listOf(TaskFormError.BlankName)))

        composeRule.onNodeWithText("Enter a name").assertIsDisplayed()
    }

    @Test
    fun `several times a day without a time shows its error`() {
        show(blank.copy(repeatType = RepeatType.TIMES_PER_DAY, errors = listOf(TaskFormError.NoTimes)))

        composeRule.onNodeWithText("Add at least one time").performScrollTo().assertIsDisplayed()
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

        composeRule.onNodeWithTag("tasks_form_repeat").performScrollTo().assertIsDisplayed()
    }
}
