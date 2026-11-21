package io.github.ddiling.mydayboard.core.domain.tracker

import org.junit.Assert.assertEquals
import org.junit.Test

class ValidateReasonTextTest {
    @Test
    fun `text is trimmed and valid`() {
        assertEquals(ReasonTextValidation.Valid("Stay hydrated"), validateReasonText("  Stay hydrated \n"))
    }

    @Test
    fun `blank and whitespace-only text is blank`() {
        assertEquals(ReasonTextValidation.Blank, validateReasonText(" \t\n "))
    }

    @Test
    fun `empty text is blank`() {
        assertEquals(ReasonTextValidation.Blank, validateReasonText(""))
    }

    @Test
    fun `text of the maximum length is valid`() {
        val text = "a".repeat(MAX_REASON_LENGTH)

        assertEquals(ReasonTextValidation.Valid(text), validateReasonText(text))
    }

    @Test
    fun `text one char over the maximum is too long`() {
        val text = "a".repeat(MAX_REASON_LENGTH + 1)

        assertEquals(ReasonTextValidation.TooLong(MAX_REASON_LENGTH), validateReasonText(text))
    }
}
