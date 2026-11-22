package io.github.ddiling.mydayboard.feature.tasks.form

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import io.github.ddiling.mydayboard.core.domain.task.EveryNDaysMode
import io.github.ddiling.mydayboard.core.testing.TestZones
import io.github.ddiling.mydayboard.core.testing.warsaw
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class TaskFormNextDatesTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val today = LocalDate.of(2026, 11, 22)
    private val fixedFromTheFirst =
        TaskFormUiState(
            today = today,
            repeatType = RepeatType.EVERY_N_DAYS,
            intervalMode = EveryNDaysMode.FIXED_CALENDAR,
            intervalDays = 3,
            firstDue = LocalDate.of(2026, 12, 1),
        )

    private fun show(state: TaskFormUiState) =
        composeRule.setContent { TaskFormScreen(state, emptyList(), TaskFormActions()) }

    @Test
    fun `the fixed calendar lists the dates counted from the starting day`() {
        show(fixedFromTheFirst)

        composeRule
            .onNodeWithTag("tasks_form_next_dates")
            .performScrollTo()
            .assertTextEquals("Next: Dec 1, 2026, Dec 4, 2026, Dec 7, 2026")
    }

    @Test
    fun `a new task counted from the last time is first due on its start day`() {
        val fromLast =
            fixedFromTheFirst.copy(
                intervalMode = EveryNDaysMode.FROM_LAST_DONE,
                intervalDays = 14,
                firstDue = today,
            )

        assertEquals(
            listOf(today, today.plusDays(14), today.plusDays(28)),
            fromLast.nextDueDates(),
        )
    }

    @Test
    fun `other repeat types list no dates and show no line`() {
        val everyDay = fixedFromTheFirst.copy(repeatType = RepeatType.EVERY_DAY)
        show(everyDay)

        assertEquals(emptyList<LocalDate>(), everyDay.nextDueDates())
        composeRule.onNodeWithTag("tasks_form_next_dates").assertDoesNotExist()
    }

    @Test
    fun `the dates follow the day of the injected clock's zone`() {
        val fixture = TaskFormFixture(warsaw(2026, 3, 1, 23, 30))
        val inWarsaw = fixture.viewModelFor().also { it.actions.onRepeatTypeChange(RepeatType.EVERY_N_DAYS) }
        fixture.clock.zone = TestZones.TOKYO
        val inTokyo = fixture.viewModelFor().also { it.actions.onRepeatTypeChange(RepeatType.EVERY_N_DAYS) }

        assertEquals(
            LocalDate.of(2026, 3, 1),
            inWarsaw.uiState.value
                .nextDueDates()
                .first(),
        )
        assertEquals(
            LocalDate.of(2026, 3, 2),
            inTokyo.uiState.value
                .nextDueDates()
                .first(),
        )
    }

    @Test
    @Config(qualifiers = "w320dp-h480dp")
    fun `every section of the longest form is reachable by scrolling at double font size`() {
        RuntimeEnvironment.setFontScale(2f)
        show(fixedFromTheFirst.copy(goalOn = true, goal = 3))

        listOf(
            "tasks_form_name",
            "tasks_form_first_due",
            "tasks_form_next_dates",
            "tasks_form_goal",
            "tasks_form_missed_expires",
        ).forEach { composeRule.onNodeWithTag(it).performScrollTo().assertIsDisplayed() }
    }
}
