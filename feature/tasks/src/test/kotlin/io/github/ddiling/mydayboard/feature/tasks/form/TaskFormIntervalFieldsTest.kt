package io.github.ddiling.mydayboard.feature.tasks.form

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import io.github.ddiling.mydayboard.core.domain.task.EveryNDaysMode
import io.github.ddiling.mydayboard.core.domain.task.MAX_INTERVAL_DAYS
import io.github.ddiling.mydayboard.core.domain.task.MIN_INTERVAL_DAYS
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class TaskFormIntervalFieldsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val today = LocalDate.of(2026, 3, 2)
    private val everyThreeDays = TaskFormUiState(today = today, repeatType = RepeatType.EVERY_N_DAYS)
    private val startedLater = LocalDate.of(2026, 4, 20)
    private val fixedEveryThreeDays = everyThreeDays.copy(intervalMode = EveryNDaysMode.FIXED_CALENDAR)

    private fun show(
        state: TaskFormUiState,
        actions: TaskFormActions = TaskFormActions(),
    ) = composeRule.setContent { TaskFormScreen(state, emptyList(), actions) }

    @Test
    fun `every N days shows the stepper and both modes with their help and marks the chosen one`() {
        show(everyThreeDays)

        composeRule.onNodeWithTag("tasks_form_interval").assertTextEquals("3")
        composeRule
            .onNodeWithText(
                "Done on Wed, next on Sat. Done late on Thu, next on Sun.",
            ).performScrollTo()
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(
                "Always on the same dates, even if you tick it late.",
            ).performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("tasks_form_mode_from_last").assertIsSelected()
        composeRule.onNodeWithTag("tasks_form_mode_fixed").assertIsNotSelected()
    }

    @Test
    fun `choosing a mode reports it`() {
        val chosen = mutableListOf<EveryNDaysMode>()
        show(everyThreeDays, TaskFormActions(onIntervalModeChange = { chosen += it }))

        composeRule.onNodeWithTag("tasks_form_mode_fixed").performScrollTo().performClick()

        assertEquals(listOf(EveryNDaysMode.FIXED_CALENDAR), chosen)
    }

    @Test
    fun `the plus button reports one more day`() {
        val intervals = mutableListOf<Int>()
        show(everyThreeDays, TaskFormActions(onIntervalChange = { intervals += it }))

        composeRule.onNodeWithTag("tasks_form_interval_more").performScrollTo().performClick()

        assertEquals(listOf(4), intervals)
    }

    @Test
    fun `the minus button reports one fewer day`() {
        val intervals = mutableListOf<Int>()
        show(everyThreeDays, TaskFormActions(onIntervalChange = { intervals += it }))

        composeRule.onNodeWithTag("tasks_form_interval_less").performScrollTo().performClick()

        assertEquals(listOf(2), intervals)
    }

    @Test
    fun `the two modes are one radio group`() {
        show(everyThreeDays)

        composeRule
            .onNodeWithTag("tasks_form_mode_fixed")
            .onParent()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup))
    }

    @Test
    fun `the minus button is off at the lowest interval`() {
        show(everyThreeDays.copy(intervalDays = MIN_INTERVAL_DAYS))

        composeRule.onNodeWithTag("tasks_form_interval_less").assertIsNotEnabled()
        composeRule.onNodeWithTag("tasks_form_interval_more").assertIsEnabled()
    }

    @Test
    fun `the plus button is off at the highest interval`() {
        show(everyThreeDays.copy(intervalDays = MAX_INTERVAL_DAYS))

        composeRule.onNodeWithTag("tasks_form_interval_more").assertIsNotEnabled()
        composeRule.onNodeWithTag("tasks_form_interval_less").assertIsEnabled()
    }

    @Test
    fun `the starting on field shows only in the fixed calendar mode`() {
        show(everyThreeDays)

        composeRule.onNodeWithTag("tasks_form_first_due").assertDoesNotExist()
    }

    @Test
    fun `the fixed calendar mode shows the starting on field`() {
        show(everyThreeDays.copy(intervalMode = EveryNDaysMode.FIXED_CALENDAR))

        composeRule.onNodeWithTag("tasks_form_first_due").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `every N days has no time field`() {
        show(everyThreeDays)

        composeRule.onNodeWithTag("tasks_form_time").assertDoesNotExist()
    }

    @Test
    fun `the starting on field is a button at least 48 dp tall`() {
        show(fixedEveryThreeDays)

        composeRule
            .onNodeWithTag("tasks_form_first_due")
            .performScrollTo()
            .assertHeightIsAtLeast(48.dp)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    @Test
    fun `the date picker opens on the date in the state, not on today`() {
        show(fixedEveryThreeDays.copy(firstDue = startedLater))

        composeRule.onNodeWithTag("tasks_form_first_due").performScrollTo().performClick()

        // One copy is the field behind the dialog, the other is the picker headline.
        composeRule.onAllNodesWithText("Apr 20, 2026").assertCountEquals(2)
    }

    @Test
    fun `confirming the date picker reports the day that was picked`() {
        val picked = mutableListOf<LocalDate>()
        show(
            fixedEveryThreeDays.copy(firstDue = startedLater),
            TaskFormActions(onFirstDueChange = { picked += it }),
        )

        composeRule.onNodeWithTag("tasks_form_first_due").performScrollTo().performClick()
        composeRule.onNodeWithText("April 25, 2026", substring = true).performClick()
        composeRule.onNodeWithText("OK").performClick()

        assertEquals(listOf(startedLater.withDayOfMonth(25)), picked)
    }

    @Test
    fun `cancelling the date picker reports nothing`() {
        val picked = mutableListOf<LocalDate>()
        show(fixedEveryThreeDays, TaskFormActions(onFirstDueChange = { picked += it }))

        composeRule.onNodeWithTag("tasks_form_first_due").performScrollTo().performClick()
        composeRule.onNodeWithText("Cancel").performClick()

        assertEquals(emptyList<LocalDate>(), picked)
    }
}
