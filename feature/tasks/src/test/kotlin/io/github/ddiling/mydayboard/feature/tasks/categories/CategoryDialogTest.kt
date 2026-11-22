package io.github.ddiling.mydayboard.feature.tasks.categories

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CategoryDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val newCategory = CategoryDialogState.Editing(id = null, name = "")
    private val existingCategory = CategoryDialogState.Editing(id = "home", name = "Home", canSave = true)

    private fun show(
        state: CategoryDialogState.Editing,
        actions: CategoryDialogActions = CategoryDialogActions(),
    ) = composeRule.setContent { CategoryDialogContent(state, actions) }

    @Test
    fun `save is disabled while the name is empty`() {
        show(newCategory)

        composeRule.onNodeWithTag("tasks_category_save").assertIsNotEnabled()
    }

    @Test
    fun `save and cancel call their callbacks`() {
        val calls = mutableListOf<String>()
        show(existingCategory, CategoryDialogActions(onSave = { calls += "save" }, onCancel = { calls += "cancel" }))

        composeRule.onNodeWithTag("tasks_category_save").performClick()
        composeRule.onNodeWithTag("tasks_category_cancel").performClick()

        assertEquals(listOf("save", "cancel"), calls)
    }

    @Test
    fun `delete calls its callback in edit mode`() {
        var deletes = 0
        show(existingCategory, CategoryDialogActions(onDelete = { deletes++ }))

        composeRule.onNodeWithTag("tasks_category_delete").performClick()

        assertEquals(1, deletes)
    }

    @Test
    fun `delete is not shown for a new category`() {
        show(newCategory)

        composeRule.onNodeWithTag("tasks_category_delete").assertDoesNotExist()
    }

    @Test
    fun `the duplicate error text is shown`() {
        show(newCategory.copy(name = "Home", error = CategoryNameError.Duplicate))

        composeRule.onNodeWithText("A category with this name already exists").assertIsDisplayed()
    }

    @Test
    fun `at double font size the content scrolls and the buttons stay reachable`() {
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 2f)) {
                CategoryDialogContent(existingCategory, CategoryDialogActions())
            }
        }

        composeRule.onNodeWithTag("tasks_category_delete").performScrollTo().assertIsDisplayed()
    }
}
