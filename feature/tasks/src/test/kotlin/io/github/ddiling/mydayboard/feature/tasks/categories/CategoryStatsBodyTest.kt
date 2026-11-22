package io.github.ddiling.mydayboard.feature.tasks.categories

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import io.github.ddiling.mydayboard.core.designsystem.preview.PreviewSurface
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CategoryStatsBodyTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val dinner = TaskLine("dinner", "Dinner", skipped = 1)
    private val snack = TaskLine("snack", "Snack", skipped = 0)
    private val content = previewStatsContent.copy(tasks = listOf(dinner), archivedLines = listOf(snack))

    private fun show(
        state: CategoryStatsUiState.Content,
        onTaskClick: (String) -> Unit = {},
        fontScale: Float = 1f,
    ) = composeRule.setContent {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
            PreviewSurface { CategoryStatsBody(state, onTaskClick) }
        }
    }

    @Test
    fun `the hero shows the average with the unit text`() {
        show(content)

        composeRule.onNodeWithTag("tasks_catstats_hero", useUnmergedTree = true).assertTextEquals("2.4")
        composeRule
            .onNodeWithTag("tasks_catstats_hero_unit", useUnmergedTree = true)
            .assertTextEquals("tasks done a day, average of the last 30 days")
    }

    @Test
    fun `a task row shows how often it was skipped`() {
        show(content)

        composeRule.onNode(hasText("skipped 1 time")).assertIsDisplayed()
    }

    @Test
    fun `tapping a task calls onTaskClick with its id`() {
        val clicked = mutableListOf<String>()
        show(content, onTaskClick = { clicked += it })

        composeRule.onNodeWithTag("tasks_catstats_row_dinner").performClick()

        assertEquals(listOf("dinner"), clicked)
    }

    @Test
    fun `an archived task cannot be opened`() {
        val clicked = mutableListOf<String>()
        show(content, onTaskClick = { clicked += it })

        composeRule.onNodeWithTag("tasks_catstats_row_snack").performClick()

        assertEquals(emptyList<String>(), clicked)
    }

    @Test
    fun `the section titles are headings`() {
        show(content)

        val heading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)
        composeRule.onNode(heading and hasText("ARCHIVED TASKS STILL COUNT")).assertIsDisplayed()
    }

    @Test
    fun `with a doubled font size the archived tasks can still be reached`() {
        show(content, fontScale = 2f)

        composeRule.onNodeWithTag("tasks_catstats_content").performScrollToNode(hasText("Snack"))

        composeRule.onNode(hasText("Snack")).assertIsDisplayed()
    }
}
