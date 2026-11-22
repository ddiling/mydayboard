package io.github.ddiling.mydayboard.feature.tasks.categories

import io.github.ddiling.mydayboard.core.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Robolectric supplies the real `Bundle` that `toRoute` needs to read the route arguments. */
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class CategoryStatsViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val fixture = CategoryStatsFixture()

    private fun TestScope.collect(viewModel: CategoryStatsViewModel) {
        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()
    }

    @Test
    fun `the state is Loading until the first data arrives`() =
        runTest {
            val meals = fixture.addCategory()

            assertEquals(CategoryStatsUiState.Loading, fixture.viewModel(meals.id).uiState.value)
        }

    @Test
    fun `renaming through the dialog changes the title`() =
        runTest {
            val meals = fixture.addCategory()
            fixture.addMeal(meals, "Breakfast", doneDays = 1)
            val viewModel = fixture.viewModel(meals.id)
            collect(viewModel)

            viewModel.onEdit()
            runCurrent()
            viewModel.categoryDialogActions.onNameChange("Food")
            runCurrent()
            viewModel.categoryDialogActions.onSave()
            runCurrent()

            assertEquals("Food", (viewModel.uiState.value as CategoryStatsUiState.Content).categoryName)
            assertEquals(CategoryDialogState.Closed, viewModel.categoryDialog.value)
        }

    @Test
    fun `deleting through the dialog leaves the screen and keeps the tasks`() =
        runTest {
            val meals = fixture.addCategory()
            fixture.addMeal(meals, "Breakfast", doneDays = 1)
            val viewModel = fixture.viewModel(meals.id)
            collect(viewModel)

            viewModel.onEdit()
            runCurrent()
            viewModel.categoryDialogActions.onDelete()
            runCurrent()

            assertTrue(viewModel.left.value)
            assertEquals(
                listOf("Breakfast"),
                fixture.tasks
                    .observeTasks()
                    .first()
                    .map { it.name },
            )
        }

    @Test
    fun `the screen does not leave while the category exists`() =
        runTest {
            val meals = fixture.addCategory()
            val viewModel = fixture.viewModel(meals.id)

            viewModel.onEdit()
            runCurrent()
            viewModel.categoryDialogActions.onCancel()

            assertFalse(viewModel.left.value)
        }

    @Test
    fun `handling the leave clears it`() =
        runTest {
            val meals = fixture.addCategory()
            val viewModel = fixture.viewModel(meals.id)
            viewModel.onEdit()
            runCurrent()
            viewModel.categoryDialogActions.onDelete()
            runCurrent()

            viewModel.onLeftHandled()

            assertFalse(viewModel.left.value)
        }
}
