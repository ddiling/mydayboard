package io.github.ddiling.mydayboard.feature.tasks.categories

/** What the category dialog reports; grouped so the form can pass its editor's functions in one value. */
internal data class CategoryDialogActions(
    val onNameChange: (String) -> Unit = {},
    val onSave: () -> Unit = {},
    val onCancel: () -> Unit = {},
    val onDelete: () -> Unit = {},
)
