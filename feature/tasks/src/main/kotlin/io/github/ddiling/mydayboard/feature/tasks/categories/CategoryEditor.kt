package io.github.ddiling.mydayboard.feature.tasks.categories

import io.github.ddiling.mydayboard.core.domain.task.CategoryNameValidation
import io.github.ddiling.mydayboard.core.domain.task.TaskCategoryRepository
import io.github.ddiling.mydayboard.core.domain.task.validateCategoryName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject

/**
 * The logic behind the category dialog, a plain class so the form, the list and the category screen share it
 * and behave the same. It only decides and remembers; the ViewModel that owns it calls it from its scope. Each
 * ViewModel gets its own instance. The names to compare with are read when the user types or saves, so a
 * category made elsewhere in the meantime is still caught as a duplicate.
 *
 * A name check can finish after the user moved on, so a result is applied only while the dialog is still open
 * and the text it judged is still the latest text; a late result never re-opens a closed dialog or replaces
 * newer text. Save is one at a time, so a double tap creates one category.
 *
 * Delete asks no question: it soft-deletes only the category, and its tasks keep their id, so [undoDelete]
 * puts everything back.
 */
internal class CategoryEditor
    @Inject
    constructor(
        private val categories: TaskCategoryRepository,
    ) {
        private val _state = MutableStateFlow<CategoryDialogState>(CategoryDialogState.Closed)

        /** The dialog to show. */
        val state: StateFlow<CategoryDialogState> = _state.asStateFlow()

        private val _deleted = MutableStateFlow<DeletedCategory?>(null)

        /** The last deleted category while its Undo is on offer, or null. */
        val deleted: StateFlow<DeletedCategory?> = _deleted.asStateFlow()

        /** The name the category had when the dialog opened; it is not a duplicate of itself. */
        private var originalName: String? = null

        /** The text of the newest name check; older results are ignored. Null while no dialog is open. */
        private var latestInput: String? = null

        private val saving = Mutex()

        /** Opens the dialog for a new category ([id] null) or for the category [id]; an unknown id opens nothing. */
        suspend fun open(id: String?) {
            val current = categories.observeCategories().first()
            val existing = id?.let { wanted -> current.firstOrNull { it.id == wanted } ?: return }
            originalName = existing?.name
            latestInput = null
            _state.value =
                CategoryDialogState.Editing(
                    id = id,
                    name = existing?.name.orEmpty(),
                    canSave = existing != null,
                )
        }

        suspend fun onNameChange(name: String) {
            if (_state.value !is CategoryDialogState.Editing) return
            latestInput = name
            val result = validate(name)
            _state.update { current ->
                val isLatest = current is CategoryDialogState.Editing && latestInput == name
                if (isLatest) current.judged(name, result) else current
            }
        }

        /**
         * Saves the typed name and closes the dialog. Returns the id of the created or renamed category, or null
         * when nothing was saved: the dialog is closed, or the name is not valid (the error stays on screen).
         */
        suspend fun onSave(): String? {
            if (!saving.tryLock()) return null
            try {
                return saveOpenDialog()
            } finally {
                saving.unlock()
            }
        }

        private suspend fun saveOpenDialog(): String? {
            val editing = _state.value as? CategoryDialogState.Editing ?: return null
            val result = validate(editing.name)
            var stillOpen = false
            _state.update { current ->
                stillOpen = current is CategoryDialogState.Editing && current.name == editing.name
                if (stillOpen) editing.judged(editing.name, result) else current
            }
            return if (stillOpen && result is CategoryNameValidation.Valid) store(editing.id, result.name) else null
        }

        private suspend fun store(
            id: String?,
            name: String,
        ): String {
            val savedId =
                if (id == null) {
                    categories.createCategory(name).id
                } else {
                    categories.renameCategory(id, name)
                    id
                }
            _state.value = CategoryDialogState.Closed
            return savedId
        }

        /** Deletes the category being edited, closes the dialog and offers Undo. */
        suspend fun onDelete() {
            val editing = _state.value as? CategoryDialogState.Editing ?: return
            val id = editing.id ?: return
            categories.deleteCategory(id)
            _deleted.value = DeletedCategory(id, originalName.orEmpty())
            _state.value = CategoryDialogState.Closed
        }

        /** Restores the deleted category with its tasks and ends the offer. */
        suspend fun undoDelete() {
            val gone = _deleted.value ?: return
            _deleted.value = null
            categories.restoreCategory(gone.id)
        }

        /** Ends the Undo offer (the snackbar timed out or was dismissed); the deletion stays. */
        fun onUndoHandled() {
            _deleted.value = null
        }

        /** Closes the dialog and saves nothing: Cancel, a tap outside and Back all end here. */
        fun dismiss() {
            latestInput = null
            _state.value = CategoryDialogState.Closed
        }

        private suspend fun validate(raw: String): CategoryNameValidation =
            validateCategoryName(raw, categories.observeCategories().first().map { it.name }, originalName)
    }

private fun CategoryDialogState.Editing.judged(
    newName: String,
    result: CategoryNameValidation,
): CategoryDialogState.Editing =
    copy(
        name = newName,
        error =
            when (result) {
                CategoryNameValidation.Duplicate -> CategoryNameError.Duplicate
                is CategoryNameValidation.TooLong -> CategoryNameError.TooLong(result.max)
                is CategoryNameValidation.Valid, CategoryNameValidation.Blank -> null
            },
        canSave = result is CategoryNameValidation.Valid,
    )
