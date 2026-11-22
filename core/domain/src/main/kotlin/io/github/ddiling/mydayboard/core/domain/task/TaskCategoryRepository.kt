package io.github.ddiling.mydayboard.core.domain.task

import kotlinx.coroutines.flow.Flow

/**
 * Storage for task categories. Same contract as [TaskRepository]: no validation (callers use
 * `validateCategoryName`), ids and dates from the injected `IdGenerator` and `Clock`, unknown or deleted ids are
 * no-ops, flows apply `distinctUntilChanged()`.
 */
interface TaskCategoryRepository {
    /** Live categories, oldest first (`createdAt`, then `id`). */
    fun observeCategories(): Flow<List<TaskCategory>>

    suspend fun createCategory(name: String): TaskCategory

    suspend fun renameCategory(
        id: String,
        name: String,
    )

    /**
     * Soft delete. Tasks keep their category id, so restoring the category (an Undo) puts them back; until then
     * they read as having no category.
     */
    suspend fun deleteCategory(id: String)

    suspend fun restoreCategory(id: String)
}
