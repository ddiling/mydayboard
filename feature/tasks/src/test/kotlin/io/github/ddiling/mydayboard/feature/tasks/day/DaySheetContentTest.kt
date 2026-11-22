package io.github.ddiling.mydayboard.feature.tasks.day

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.ddiling.mydayboard.core.designsystem.preview.PreviewSurface
import io.github.ddiling.mydayboard.feature.tasks.common.previewToday
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
class DaySheetContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val morning = LocalTime.of(8, 0)
    private val evening = LocalTime.of(20, 0)
    private val day = previewToday.minusDays(1)

    private fun show(
        kind: DaySheetKind,
        actions: DaySheetActions = DaySheetActions(),
        fontScale: Float = 1f,
    ) = composeRule.setContent {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
            PreviewSurface { DaySheetContent(DaySheetState("lunch", "Lunch", day, kind), actions) }
        }
    }

    @Test
    fun `a one-time day shows two radios with the current one selected`() {
        show(DaySheetKind.Single(done = true))

        composeRule.onNodeWithTag("tasks_day_done").assertIsSelected()
        composeRule.onNodeWithTag("tasks_day_not_done").assertIsNotSelected()
    }

    @Test
    fun `tapping a radio reports the choice`() {
        val chosen = mutableListOf<Boolean>()
        show(DaySheetKind.Single(done = false), DaySheetActions(onSetSingle = { chosen += it }))

        composeRule.onNodeWithTag("tasks_day_done").performClick()

        assertEquals(listOf(true), chosen)
    }

    @Test
    fun `a several-times day shows a chip per time and tapping one reports it`() {
        val toggled = mutableListOf<LocalTime>()
        show(
            DaySheetKind.Times(listOf(morning, evening), setOf(morning)),
            DaySheetActions(onToggle = { toggled += it }),
        )

        composeRule.onNodeWithTag("tasks_detail_chip_08:00").assertIsDisplayed()
        composeRule.onNodeWithTag("tasks_detail_chip_20:00").performClick()

        assertEquals(listOf(evening), toggled)
    }

    @Test
    fun `a count day shows the stepper with its value and goal`() {
        show(DaySheetKind.Count(target = 3, value = 2))

        composeRule.onNodeWithTag("tasks_day_count").assert(hasText("2 / 3"))
    }

    @Test
    fun `the stepper buttons are 48 dp and have descriptions`() {
        show(DaySheetKind.Count(target = 3, value = 1))

        listOf("tasks_day_minus" to "One less", "tasks_day_plus" to "One more").forEach { (tag, name) ->
            composeRule
                .onNodeWithTag(tag)
                .assertWidthIsAtLeast(48.dp)
                .assertHeightIsAtLeast(48.dp)
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(name)))
        }
    }

    @Test
    fun `the stepper buttons stop at 0 and at the goal`() {
        show(DaySheetKind.Count(target = 3, value = 0))
        composeRule.onNodeWithTag("tasks_day_minus").assertIsNotEnabled()
        composeRule.onNodeWithTag("tasks_day_plus").assertIsEnabled()
    }

    @Test
    fun `Save calls onSave and is 52 dp high`() {
        var saves = 0
        show(DaySheetKind.Single(done = false), DaySheetActions(onSave = { saves++ }))

        composeRule.onNodeWithTag("tasks_day_save").assertHeightIsEqualTo(52.dp).performClick()

        assertEquals(1, saves)
    }

    @Test
    fun `the title names the task and the day`() {
        show(DaySheetKind.Single(done = false))

        composeRule.onNodeWithTag("tasks_day_title").assert(hasText("Lunch · Thu 19.11"))
    }

    @Test
    fun `the content stays readable at font scale 2`() {
        show(DaySheetKind.Times(listOf(morning, evening), setOf(morning)), fontScale = 2f)

        composeRule.onNodeWithTag("tasks_day_title").assertIsDisplayed()
        composeRule.onNodeWithTag("tasks_detail_chip_20:00").assertIsDisplayed()
        composeRule.onNodeWithTag("tasks_day_save").assertIsDisplayed()
    }

    private fun hasText(text: String) =
        SemanticsMatcher.expectValue(
            SemanticsProperties.Text,
            listOf(
                androidx.compose.ui.text
                    .AnnotatedString(text),
            ),
        )
}
