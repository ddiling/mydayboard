package io.github.ddiling.mydayboard.core.domain.task

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Storage for tasks. Deletes are soft in storage (so sync can propagate them) but invisible here.
 *
 * Contract shared by every implementation:
 * - The repository does NOT validate. Callers use `validateTaskDraft` first.
 * - It sets `id`, `createdAt` and `updatedAt` itself, from the injected `IdGenerator` and `Clock`.
 * - Operations on unknown or deleted ids are no-ops: they change nothing and do not throw. A screen may still
 *   show an item that another device just deleted.
 * - Observed flows apply `distinctUntilChanged()`, so collectors are only woken by changes to what they observe.
 */
interface TaskRepository {
    /**
     * Live tasks, archived ones included (callers filter). `categoryId` is null while the task's category is
     * deleted, and comes back if the category is restored.
     */
    fun observeTasks(): Flow<List<Task>>

    /** The task with [id], or null when it is missing or deleted. */
    fun observeTask(id: String): Flow<Task?>

    /** Creates a task from [draft] that counts from [startDate]. */
    suspend fun createTask(
        draft: TaskDraft,
        startDate: LocalDate,
    ): Task

    /** Replaces everything the form edits; `startDate` and `archivedAt` are kept. */
    suspend fun updateTask(
        id: String,
        draft: TaskDraft,
    )

    /** Hides the task from today but keeps its history; sets `archivedAt` to the clock's instant. */
    suspend fun archiveTask(id: String)

    /** Clears `archivedAt`. */
    suspend fun restoreTask(id: String)

    /** Soft delete: the task and its ticks disappear from every flow. */
    suspend fun deleteTask(id: String)
}
