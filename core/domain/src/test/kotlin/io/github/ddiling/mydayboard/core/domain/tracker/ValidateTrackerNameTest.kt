package io.github.ddiling.mydayboard.core.domain.tracker

import org.junit.Assert.assertEquals
import org.junit.Test

class ValidateTrackerNameTest {
    @Test
    fun `an empty name is blank`() {
        val result = validateTrackerName("")

        assertEquals(TrackerNameValidation.Blank, result)
    }

    @Test
    fun `a name of only whitespace is blank`() {
        val result = validateTrackerName(" \t\n ")

        assertEquals(TrackerNameValidation.Blank, result)
    }

    @Test
    fun `a valid name is returned trimmed`() {
        val result = validateTrackerName("  Coffee \n")

        assertEquals(TrackerNameValidation.Valid("Coffee"), result)
    }

    @Test
    fun `a name of exactly the maximum length is valid`() {
        val name = "a".repeat(40)

        val result = validateTrackerName(name)

        assertEquals(TrackerNameValidation.Valid(name), result)
    }

    @Test
    fun `a name one character over the maximum is too long`() {
        val result = validateTrackerName("a".repeat(41))

        assertEquals(TrackerNameValidation.TooLong(40), result)
    }

    @Test
    fun `surrounding whitespace does not count against the limit`() {
        val name = "a".repeat(40)

        val result = validateTrackerName("   $name   ")

        assertEquals(TrackerNameValidation.Valid(name), result)
    }

    @Test
    fun `emoji count as one character each even though they are two UTF-16 chars`() {
        val name = "😀".repeat(40)

        val result = validateTrackerName(name)

        assertEquals(80, name.length)
        assertEquals(TrackerNameValidation.Valid(name), result)
    }

    @Test
    fun `forty one emoji are too long`() {
        val result = validateTrackerName("😀".repeat(41))

        assertEquals(TrackerNameValidation.TooLong(40), result)
    }

    @Test
    fun `Polish diacritics are kept unchanged`() {
        val result = validateTrackerName("Zażółć gęślą jaźń")

        assertEquals(TrackerNameValidation.Valid("Zażółć gęślą jaźń"), result)
    }
}
