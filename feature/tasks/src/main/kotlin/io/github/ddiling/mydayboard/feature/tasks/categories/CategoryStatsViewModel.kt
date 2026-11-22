package io.github.ddiling.mydayboard.feature.tasks.categories

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ddiling.mydayboard.feature.tasks.navigation.CategoryStatsDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L

/**
 * The statistics of one category and its edit dialog. The pencil opens the shared category dialog for this
 * category; a rename shows at once because the title comes from the repository, and a delete sets [left] so the
 * screen goes back to the list. Deleting moves the tasks to "No category", so none of them is lost.
 */
@HiltViewModel
internal class CategoryStatsViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        observe: ObserveCategoryStats,
        private val categoryEditor: CategoryEditor,
    ) : ViewModel() {
        private val categoryId: String = savedStateHandle.toRoute<CategoryStatsDestination>().categoryId
        private val _left = MutableStateFlow(false)

        val uiState: StateFlow<CategoryStatsUiState> =
            observe(categoryId).stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
                CategoryStatsUiState.Loading,
            )

        val categoryDialog: StateFlow<CategoryDialogState> = categoryEditor.state

        /** True once the category was deleted and the screen should close; [onLeftHandled] clears it. */
        val left: StateFlow<Boolean> = _left.asStateFlow()

        val categoryDialogActions =
            CategoryDialogActions(
                onNameChange = { name -> viewModelScope.launch { categoryEditor.onNameChange(name) } },
                onSave = { viewModelScope.launch { categoryEditor.onSave() } },
                onCancel = categoryEditor::dismiss,
                onDelete = ::onDelete,
            )

        /** Opens the dialog for this category. */
        fun onEdit() {
            viewModelScope.launch { categoryEditor.open(categoryId) }
        }

        fun onLeftHandled() {
            _left.value = false
        }

        private fun onDelete() {
            viewModelScope.launch {
                categoryEditor.onDelete()
                if (categoryEditor.deleted.value?.id == categoryId) _left.value = true
            }
        }
    }
