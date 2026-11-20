package io.github.ddiling.mydayboard.feature.trackers.event

import io.github.ddiling.mydayboard.core.domain.tracker.Tracker
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerEvent
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerType
import io.github.ddiling.mydayboard.core.testing.FakeTrackerRepository
import io.github.ddiling.mydayboard.core.testing.MutableClock
import io.github.ddiling.mydayboard.core.testing.SequentialIdGenerator
import io.github.ddiling.mydayboard.core.testing.TestZones
import io.github.ddiling.mydayboard.core.testing.warsaw
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class EventTimeEditorTest {
    private val now = warsaw(2026, 4, 1, 10, 30)
    private val today = LocalDate.of(2026, 4, 1)
    private val clock = MutableClock(now, TestZones.WARSAW)
    private val repository = FakeTrackerRepository(clock, SequentialIdGenerator())
    private val deletedIds = mutableListOf<String>()
    private val pastDate = LocalDate.of(2026, 3, 30)
    private val pastTime = LocalTime.of(8, 15)

    private fun TestScope.editorFor(tracker: Tracker) =
        EventTimeEditor(this, tracker.id, repository, clock) { deletedIds += it }

    private suspend fun givenCounter() =
        repository.createTracker("Water", TrackerType.EVENT_COUNTER, warsaw(2026, 1, 1))

    private suspend fun givenDaysSince(startedAt: Instant = warsaw(2026, 3, 1)) =
        repository.createTracker("No sugar", TrackerType.DAYS_SINCE, startedAt)

    private suspend fun eventTimes(tracker: Tracker): List<Instant> =
        repository.observeEvents(tracker.id).first().map { it.occurredAt }

    private suspend fun eventsOf(tracker: Tracker): List<TrackerEvent> = repository.observeEvents(tracker.id).first()

    private fun TestScope.pick(
        editor: EventTimeEditor,
        date: LocalDate,
        time: LocalTime,
    ) {
        editor.changeDate(date)
        editor.changeTime(time)
        runCurrent()
    }

    @Test
    fun `opening add starts at the current minute in the clock zone`() =
        runTest {
            clock.now = warsaw(2026, 4, 1, 10, 30).plusSeconds(40)
            val editor = editorFor(givenCounter())

            editor.openAdd()
            runCurrent()

            assertEquals(EventTimeEditorState(EntryMode.Add, today, LocalTime.of(10, 30), null), editor.state.value)
        }

    @Test
    fun `opening edit shows the local date and time of that event`() =
        runTest {
            val tracker = givenCounter()
            val event = repository.addEvent(tracker.id, warsaw(2026, 3, 30, 8, 15))!!
            val editor = editorFor(tracker)

            editor.openEdit(event.id)
            runCurrent()

            assertEquals(EventTimeEditorState(EntryMode.Edit(event.id), pastDate, pastTime, null), editor.state.value)
        }

    @Test
    fun `opening edit for an unknown event stays closed`() =
        runTest {
            val editor = editorFor(givenCounter())

            editor.openEdit("missing")
            runCurrent()

            assertNull(editor.state.value)
        }

    @Test
    fun `saving an add stores the event at the picked past time and closes the dialog`() =
        runTest {
            val tracker = givenCounter()
            val editor = editorFor(tracker)
            editor.openAdd()
            runCurrent()
            pick(editor, pastDate, pastTime)

            editor.save()
            runCurrent()

            assertEquals(listOf(warsaw(2026, 3, 30, 8, 15)), eventTimes(tracker))
            assertNull(editor.state.value)
        }

    @Test
    fun `a time the clocks skipped is stored an hour later on the spring change day`() =
        runTest {
            val tracker = givenCounter()
            val editor = editorFor(tracker)
            editor.openAdd()
            runCurrent()
            pick(editor, LocalDate.of(2026, 3, 29), LocalTime.of(2, 30))

            editor.save()
            runCurrent()

            val threeThirtyCest = Instant.parse("2026-03-29T01:30:00Z")
            assertEquals(listOf(threeThirtyCest), eventTimes(tracker))
        }

    @Test
    fun `a future time shows in-future and saves nothing`() =
        runTest {
            val tracker = givenCounter()
            val editor = editorFor(tracker)
            editor.openAdd()
            runCurrent()
            pick(editor, today, LocalTime.of(10, 31))

            editor.save()
            runCurrent()

            assertEquals(EntryTimeError.IN_FUTURE, editor.state.value?.error)
            assertFalse(editor.state.value?.canSave ?: true)
            assertTrue(eventTimes(tracker).isEmpty())
        }

    @Test
    fun `a days-since time before the start shows before-start and saves nothing`() =
        runTest {
            val tracker = givenDaysSince(startedAt = warsaw(2026, 3, 1, 12))
            val editor = editorFor(tracker)
            editor.openAdd()
            runCurrent()
            pick(editor, LocalDate.of(2026, 3, 1), LocalTime.of(11, 59))

            editor.save()
            runCurrent()

            assertEquals(EntryTimeError.BEFORE_TRACKER_START, editor.state.value?.error)
            assertTrue(eventTimes(tracker).isEmpty())
        }

    @Test
    fun `a counter accepts a time before its start`() =
        runTest {
            val editor = editorFor(givenCounter())
            editor.openAdd()
            runCurrent()

            pick(editor, LocalDate.of(2025, 12, 1), LocalTime.of(9, 0))

            assertNull(editor.state.value?.error)
        }

    @Test
    fun `a time in the start minute but before its seconds is stored as the start`() =
        runTest {
            val startedAt = warsaw(2026, 3, 1, 12).plusSeconds(30)
            val tracker = givenDaysSince(startedAt)
            val editor = editorFor(tracker)
            editor.openAdd()
            runCurrent()
            pick(editor, LocalDate.of(2026, 3, 1), LocalTime.of(12, 0))

            editor.save()
            runCurrent()

            assertEquals(listOf(startedAt), eventTimes(tracker))
        }

    @Test
    fun `an error is judged against the clock at the moment of the change`() =
        runTest {
            val editor = editorFor(givenCounter())
            editor.openAdd()
            runCurrent()
            pick(editor, today, LocalTime.of(10, 40))
            clock.now = warsaw(2026, 4, 1, 10, 45)

            pick(editor, today, LocalTime.of(10, 40))

            assertNull(editor.state.value?.error)
        }

    @Test
    fun `saving an edit moves the event to the picked time`() =
        runTest {
            val tracker = givenCounter()
            val event = repository.addEvent(tracker.id, warsaw(2026, 3, 30, 8, 15))!!
            val editor = editorFor(tracker)
            editor.openEdit(event.id)
            runCurrent()
            pick(editor, pastDate, LocalTime.of(9, 45))

            editor.save()
            runCurrent()

            assertEquals(listOf(warsaw(2026, 3, 30, 9, 45)), eventTimes(tracker))
        }

    @Test
    fun `deleting an edited entry removes it, closes the dialog and reports its id`() =
        runTest {
            val tracker = givenCounter()
            val event = repository.addEvent(tracker.id, warsaw(2026, 3, 30, 8, 15))!!
            val editor = editorFor(tracker)
            editor.openEdit(event.id)
            runCurrent()

            editor.delete()
            runCurrent()

            assertTrue(eventsOf(tracker).isEmpty())
            assertNull(editor.state.value)
            assertEquals(listOf(event.id), deletedIds)
        }

    @Test
    fun `delete does nothing in add mode`() =
        runTest {
            val tracker = givenCounter()
            repository.addEvent(tracker.id, warsaw(2026, 3, 30, 8, 15))
            val editor = editorFor(tracker)
            editor.openAdd()
            runCurrent()

            editor.delete()
            runCurrent()

            assertEquals(1, eventsOf(tracker).size)
            assertEquals(emptyList<String>(), deletedIds)
        }

    @Test
    fun `dismiss closes the dialog and saves nothing`() =
        runTest {
            val tracker = givenCounter()
            val editor = editorFor(tracker)
            editor.openAdd()
            runCurrent()

            editor.dismiss()
            runCurrent()

            assertNull(editor.state.value)
            assertTrue(eventTimes(tracker).isEmpty())
        }
}
