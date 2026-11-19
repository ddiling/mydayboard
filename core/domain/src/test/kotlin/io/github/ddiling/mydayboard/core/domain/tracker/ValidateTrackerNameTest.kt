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

    @Test
    fun `a decomposed name is returned composed`() {
        // "o" + combining acute accent (U+0301) composes to U+00F3.
        val result = validateTrackerName("Zio\u0301\u0142ka")

        assertEquals(TrackerNameValidation.Valid("Zi\u00F3\u0142ka"), result)
    }

    @Test
    fun `forty Polish letters typed decomposed are valid`() {
        // 80 code points as typed, 40 after composing.
        val result = validateTrackerName("z\u0307".repeat(40))

        assertEquals(TrackerNameValidation.Valid("\u017C".repeat(40)), result)
    }

    @Test
    fun `whitespace runs including line breaks and tabs become one space`() {
        val result = validateTrackerName("Coffee\n\tbreak")

        assertEquals(TrackerNameValidation.Valid("Coffee break"), result)
    }

    @Test
    fun `a name of only a zero-width space is blank`() {
        val result = validateTrackerName("\u200B")

        assertEquals(TrackerNameValidation.Blank, result)
    }

    @Test
    fun `an emoji joined by a zero-width joiner is visible and valid`() {
        // Woman (U+1F469) + zero-width joiner (U+200D) + laptop (U+1F4BB) shows as one "woman technologist".
        val name = "\uD83D\uDC69\u200D\uD83D\uDCBB"

        val result = validateTrackerName(name)

        assertEquals(TrackerNameValidation.Valid(name), result)
    }

    @Test
    fun `Unicode whitespace such as a no-break space or a line separator collapses too`() {
        val result = validateTrackerName("Coffee\u00A0\u2028break")

        assertEquals(TrackerNameValidation.Valid("Coffee break"), result)
    }

    @Test
    fun `a name of only a control character is blank`() {
        val result = validateTrackerName("\u0007")

        assertEquals(TrackerNameValidation.Blank, result)
    }

    @Test
    fun `compatibility characters such as a superscript are kept as typed`() {
        val result = validateTrackerName("x\u00B2")

        assertEquals(TrackerNameValidation.Valid("x\u00B2"), result)
    }
}
