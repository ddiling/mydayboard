package io.github.ddiling.mydayboard.feature.trackers.detail

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuickActionUndoTest {
    private val deleted = mutableListOf<String>()
    private val quickUndo = QuickActionUndo { deleted += it }

    @Test
    fun `a registered action is pending`() {
        quickUndo.register("e1")

        assertEquals(Undo("e1", UndoKind.ADDED), quickUndo.pending.value)
    }

    @Test
    fun `undo deletes the registered event and clears it`() =
        runTest {
            quickUndo.register("e1")

            quickUndo.undo()

            assertEquals(listOf("e1"), deleted)
            assertNull(quickUndo.pending.value)
        }

    @Test
    fun `a second action replaces the first so only the latest can be undone`() =
        runTest {
            quickUndo.register("first")
            quickUndo.register("second")

            quickUndo.undo()
            quickUndo.undo()

            assertEquals(listOf("second"), deleted)
        }

    @Test
    fun `dismiss clears the action without deleting anything`() =
        runTest {
            quickUndo.register("e1")

            quickUndo.dismiss()
            quickUndo.undo()

            assertNull(quickUndo.pending.value)
            assertEquals(emptyList<String>(), deleted)
        }
}
