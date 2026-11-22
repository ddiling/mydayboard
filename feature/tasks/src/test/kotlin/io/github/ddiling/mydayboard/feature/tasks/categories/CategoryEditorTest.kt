package io.github.ddiling.mydayboard.feature.tasks.categories

import io.github.ddiling.mydayboard.core.domain.task.MAX_CATEGORY_NAME_LENGTH
import io.github.ddiling.mydayboard.core.domain.task.OnMiss
import io.github.ddiling.mydayboard.core.domain.task.Repeat
import io.github.ddiling.mydayboard.core.domain.task.TaskCategory
import io.github.ddiling.mydayboard.core.domain.task.TaskDraft
import io.github.ddiling.mydayboard.core.testing.FakeTaskCategoryRepository
import io.github.ddiling.mydayboard.core.testing.FakeTaskRepository
import io.github.ddiling.mydayboard.core.testing.FakeTaskStore
import io.github.ddiling.mydayboard.core.testing.MutableClock
import io.github.ddiling.mydayboard.core.testing.SequentialIdGenerator
import io.github.ddiling.mydayboard.core.testing.TestZones
import io.github.ddiling.mydayboard.core.testing.warsaw
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CategoryEditorTest {
    private val clock = MutableClock(warsaw(2026, 3, 2, 10, 30), TestZones.WARSAW)
    private val ids = SequentialIdGenerator()
    private val store = FakeTaskStore()
    private val categories = FakeTaskCategoryRepository(clock, ids, store)
    private val tasks = FakeTaskRepository(clock, ids, store)
    private val editor = CategoryEditor(categories)

    private suspend fun stored(): List<TaskCategory> = categories.observeCategories().first()

    private fun editing() = editor.state.value as CategoryDialogState.Editing

    @Test
    fun `opening a new category starts with an empty name`() =
        runTest {
            editor.open(null)

            assertEquals(CategoryDialogState.Editing(id = null, name = ""), editor.state.value)
        }

    @Test
    fun `save is disabled for a blank name`() =
        runTest {
            editor.open(null)
            editor.onNameChange("   ")

            assertFalse(editing().canSave)
        }

    @Test
    fun `a name equal to another ignoring case shows the duplicate error and saves nothing`() =
        runTest {
            categories.createCategory("Home")
            editor.open(null)
            editor.onNameChange("hOME")
            val savedId = editor.onSave()

            assertEquals(CategoryNameError.Duplicate, editing().error)
            assertFalse(editing().canSave)
            assertNull(savedId)
            assertEquals(listOf("Home"), stored().map { it.name })
        }

    @Test
    fun `the own name in edit mode is not a duplicate`() =
        runTest {
            val home = categories.createCategory("Home")
            editor.open(home.id)
            editor.onNameChange("HOME")

            assertNull(editing().error)
            assertTrue(editing().canSave)
        }

    @Test
    fun `save creates the category and returns its id`() =
        runTest {
            editor.open(null)
            editor.onNameChange("  Garden  ")
            val savedId = editor.onSave()

            val created = stored().single()
            assertEquals(created.id, savedId)
            assertEquals("Garden", created.name)
            assertEquals(CategoryDialogState.Closed, editor.state.value)
        }

    @Test
    fun `save in edit mode renames the category`() =
        runTest {
            val home = categories.createCategory("Home")
            editor.open(home.id)
            editor.onNameChange("House")
            val savedId = editor.onSave()

            assertEquals(home.id, savedId)
            assertEquals(listOf("House"), stored().map { it.name })
        }

    @Test
    fun `opening an unknown category opens nothing`() =
        runTest {
            editor.open("missing")

            assertEquals(CategoryDialogState.Closed, editor.state.value)
        }

    @Test
    fun `delete soft-deletes the category and offers undo`() =
        runTest {
            val home = categories.createCategory("Home")
            editor.open(home.id)
            editor.onDelete()

            assertTrue(stored().isEmpty())
            assertEquals(DeletedCategory(home.id, "Home"), editor.deleted.value)
            assertEquals(CategoryDialogState.Closed, editor.state.value)
        }

    @Test
    fun `undo restores the category with its tasks`() =
        runTest {
            val home = categories.createCategory("Home")
            val draft = TaskDraft("Vacuum", home.id, Repeat.EveryDay, null, null, null, OnMiss.EXPIRES)
            val task = tasks.createTask(draft, LocalDate.of(2026, 3, 1))
            editor.open(home.id)
            editor.onDelete()
            editor.undoDelete()

            assertEquals(listOf(home.id), stored().map { it.id })
            assertEquals(home.id, tasks.observeTask(task.id).first()?.categoryId)
            assertNull(editor.deleted.value)
        }

    @Test
    fun `handling the undo offer keeps the category deleted`() =
        runTest {
            val home = categories.createCategory("Home")
            editor.open(home.id)
            editor.onDelete()
            editor.onUndoHandled()

            assertNull(editor.deleted.value)
            assertTrue(stored().isEmpty())
        }

    @Test
    fun `dismiss closes the dialog and changes nothing`() =
        runTest {
            val home = categories.createCategory("Home")
            editor.open(home.id)
            editor.onNameChange("House")
            editor.dismiss()

            assertEquals(CategoryDialogState.Closed, editor.state.value)
            assertEquals(listOf("Home"), stored().map { it.name })
        }

    @Test
    fun `a name over 30 code points shows the too-long error`() =
        runTest {
            editor.open(null)
            editor.onNameChange("😀".repeat(MAX_CATEGORY_NAME_LENGTH + 1))

            assertEquals(CategoryNameError.TooLong(MAX_CATEGORY_NAME_LENGTH), editing().error)
            assertFalse(editing().canSave)
        }
}
