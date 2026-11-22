package io.github.ddiling.mydayboard.core.domain.task

import io.github.ddiling.mydayboard.core.domain.time.today
import io.github.ddiling.mydayboard.core.testing.MutableClock
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ScheduleWindowTest {
    private val friday = wednesday.plusDays(2)
    private val warsaw = ZoneId.of("Europe/Warsaw")
    private val honolulu = ZoneId.of("Pacific/Honolulu")
    private val fridayOnly = Repeat.Weekdays(setOf(DayOfWeek.FRIDAY))
    private val mondayAndFriday = Repeat.Weekdays(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))

    private fun statusOfFriday(
        onMiss: OnMiss,
        today: LocalDate,
    ): OccurrenceStatus =
        occurrencesOf(task(mondayAndFriday, friday, onMiss), emptyList(), today).first { it.due == friday }.status

    @Test
    fun `a staying weekday task is late on Saturday and Sunday and ended on Monday`() {
        val statuses = (1L..3L).map { statusOfFriday(OnMiss.STAYS, friday.plusDays(it)) }

        assertEquals(listOf(OccurrenceStatus.LATE, OccurrenceStatus.LATE, OccurrenceStatus.ENDED), statuses)
    }

    @Test
    fun `an expiring task undone on Friday has ended on Saturday`() {
        assertEquals(OccurrenceStatus.ENDED, statusOfFriday(OnMiss.EXPIRES, friday.plusDays(1)))
    }

    @Test
    fun `today's occurrence is due today when undone and done when ticked`() {
        val undone = occurrencesOf(task(), emptyList(), wednesday).single().status
        val done = occurrencesOf(task(), listOf(tick(wednesday)), wednesday).single().status

        assertEquals(listOf(OccurrenceStatus.DUE_TODAY, OccurrenceStatus.DONE), listOf(undone, done))
    }

    @Test
    fun `a count goal of 3 with two ticks is done 2 of 3 with credit 0 point 67`() {
        val ticks = listOf(tick(wednesday), tick(wednesday).copy(id = "second"))

        val result = occurrencesOf(task(goal = 3), ticks, wednesday).single()

        assertEquals(2, result.done)
        assertEquals(0.67, result.credit, 0.005)
    }

    @Test
    fun `credit is capped at 1 when ticks exceed the goal`() {
        val ticks = (1..4).map { tick(wednesday).copy(id = "tick$it") }

        val result = occurrencesOf(task(goal = 3), ticks, wednesday).single()

        assertEquals(1.0, result.credit, 0.0)
    }

    @Test
    fun `ticks outside any window are ignored`() {
        val result = occurrencesOf(task(fridayOnly, friday), listOf(tick(friday.minusDays(2))), friday)

        assertEquals(0, result.single().done)
    }

    @Test
    fun `until before today stops the list there but statuses are judged against today`() {
        val result = occurrencesOf(task(startDate = wednesday), emptyList(), friday, wednesday)

        assertEquals(listOf(wednesday), result.dues())
        assertEquals(OccurrenceStatus.ENDED, result.single().status)
    }

    @Test
    fun `no ticks and a task started today give one due today occurrence`() {
        val result = occurrencesOf(task(startDate = wednesday), emptyList(), wednesday)

        assertEquals(listOf(OccurrenceStatus.DUE_TODAY), result.map { it.status })
    }

    @Test
    fun `the due today occurrence becomes late exactly at midnight`() {
        val clock = MutableClock(Instant.parse("2026-11-18T22:59:59.999Z"), warsaw)
        val stays = task(Repeat.Weekdays(setOf(DayOfWeek.WEDNESDAY)), onMiss = OnMiss.STAYS)

        val beforeMidnight = occurrencesOf(stays, emptyList(), LocalDate.now(clock)).single().status
        clock.advanceBy(Duration.ofMillis(1))
        val atMidnight = occurrencesOf(stays, emptyList(), LocalDate.now(clock)).single().status

        assertEquals(listOf(OccurrenceStatus.DUE_TODAY, OccurrenceStatus.LATE), listOf(beforeMidnight, atMidnight))
    }

    private fun statusOnDayAt(
        instant: String,
        day: LocalDate,
    ): OccurrenceStatus {
        val clock = MutableClock(Instant.parse(instant), warsaw)
        return occurrencesOf(task(startDate = day), emptyList(), clock.today()).single().status
    }

    @Test
    fun `on the spring forward day just before 02 00 the occurrence is still due today`() {
        val status = statusOnDayAt("2026-03-29T00:59:59.999Z", LocalDate.of(2026, 3, 29))

        assertEquals(OccurrenceStatus.DUE_TODAY, status)
    }

    @Test
    fun `on the spring forward day just after 03 00 the occurrence is still due today`() {
        val status = statusOnDayAt("2026-03-29T01:00:00Z", LocalDate.of(2026, 3, 29))

        assertEquals(OccurrenceStatus.DUE_TODAY, status)
    }

    @Test
    fun `on the fall back day at the first 02 30 the occurrence is due today`() {
        val status = statusOnDayAt("2026-10-25T00:30:00Z", LocalDate.of(2026, 10, 25))

        assertEquals(OccurrenceStatus.DUE_TODAY, status)
    }

    @Test
    fun `on the fall back day at the second 02 30 the occurrence is still due today`() {
        val status = statusOnDayAt("2026-10-25T01:30:00Z", LocalDate.of(2026, 10, 25))

        assertEquals(OccurrenceStatus.DUE_TODAY, status)
    }

    @Test
    fun `a zone change at the same instant moves today and so the status`() {
        val clock = MutableClock(Instant.parse("2026-11-18T23:30:00Z"), honolulu)
        val stays = task(Repeat.Weekdays(setOf(DayOfWeek.WEDNESDAY)), onMiss = OnMiss.STAYS)

        val inHonolulu = occurrencesOf(stays, emptyList(), LocalDate.now(clock)).single().status
        clock.zone = warsaw
        val inWarsaw = occurrencesOf(stays, emptyList(), LocalDate.now(clock)).single().status

        assertEquals(listOf(OccurrenceStatus.DUE_TODAY, OccurrenceStatus.LATE), listOf(inHonolulu, inWarsaw))
    }
}
