package io.github.ddiling.mydayboard.feature.tasks.form

import io.github.ddiling.mydayboard.core.testing.MainDispatcherRule
import io.github.ddiling.mydayboard.feature.tasks.categories.CategoryDialogState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class TaskFormNewCategoryTest {
    @get:Rule
    val mainRule = MainDispatcherRule(StandardTestDispatcher())

    private val fixture = TaskFormFixture()
    private val viewModel by lazy { fixture.viewModelFor() }

    @Test
    fun `new category from the form selects the new category after save`() =
        runTest {
            viewModel.actions.onNewCategory()
            advanceUntilIdle()
            viewModel.categoryDialogActions.onNameChange("Garden")
            advanceUntilIdle()
            viewModel.categoryDialogActions.onSave()
            advanceUntilIdle()

            val created =
                fixture.categories
                    .observeCategories()
                    .first()
                    .single()
            assertEquals(created.id, viewModel.uiState.value.categoryId)
            assertEquals(CategoryDialogState.Closed, viewModel.categoryDialog.value)
        }

    @Test
    fun `cancelling the dialog keeps the chosen category`() =
        runTest {
            val health = fixture.categories.createCategory("Health")
            viewModel.actions.onCategoryChange(health.id)
            viewModel.actions.onNewCategory()
            advanceUntilIdle()
            viewModel.categoryDialogActions.onCancel()

            assertEquals(health.id, viewModel.uiState.value.categoryId)
            assertEquals(CategoryDialogState.Closed, viewModel.categoryDialog.value)
        }
}
