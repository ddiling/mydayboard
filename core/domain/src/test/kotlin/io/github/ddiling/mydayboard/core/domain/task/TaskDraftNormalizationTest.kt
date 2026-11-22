package io.github.ddiling.mydayboard.core.domain.task

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class TaskDraftNormalizationTest {
    private val morning = LocalTime.of(8, 0)
    private val evening = LocalTime.of(20, 0)
    private val draft = TaskDraft("Pills", null, Repeat.EveryDay, null, null, null, OnMiss.STAYS)

    @Test
    fun `times per day are sorted and without repeats`() {
        val unsorted = draft.copy(repeat = Repeat.TimesPerDay(listOf(evening, morning, morning)))

        assertEquals(Repeat.TimesPerDay(listOf(morning, evening)), unsorted.normalized().repeat)
    }

    @Test
    fun `other repeat types are left as they are`() {
        assertEquals(draft, draft.normalized())
    }
}
