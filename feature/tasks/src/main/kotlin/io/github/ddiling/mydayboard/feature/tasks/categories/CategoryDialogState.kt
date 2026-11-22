package io.github.ddiling.mydayboard.feature.tasks.categories

/** What the category dialog shows. */
internal sealed interface CategoryDialogState {
    /** No dialog is open. */
    data object Closed : CategoryDialogState

    /**
     * The dialog is open. [id] is null when a new category is being made. [error] is shown under the field;
     * [canSave] is separate because a blank name disables Save without being worth an error message while the
     * user has not typed anything yet.
     */
    data class Editing(
        val id: String?,
        val name: String,
        val error: CategoryNameError? = null,
        val canSave: Boolean = false,
    ) : CategoryDialogState
}

/** A problem with the typed category name that the dialog explains in words. */
internal sealed interface CategoryNameError {
    /** Another category has this name, ignoring case. */
    data object Duplicate : CategoryNameError

    /** The name has more than [max] code points. */
    data class TooLong(
        val max: Int,
    ) : CategoryNameError
}

/** A category that was just deleted, kept so the screen can offer Undo with its name. */
internal data class DeletedCategory(
    val id: String,
    val name: String,
)
