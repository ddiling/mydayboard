package io.github.ddiling.mydayboard.core.domain.tracker

/** The longest reason, in chars. Short enough to read in one glance when the reason is shown before logging. */
const val MAX_REASON_LENGTH = 120

/** Result of [validateReasonText]. The UI turns each failure into its own localized message. */
sealed interface ReasonTextValidation {
    /** The text is acceptable; [text] is the trimmed text to store. */
    data class Valid(
        val text: String,
    ) : ReasonTextValidation

    /** The text has no visible character, so the reason would show as an empty line. */
    data object Blank : ReasonTextValidation

    /** The trimmed text has more than [max] chars. [max] is included so the message can show the limit. */
    data class TooLong(
        val max: Int,
    ) : ReasonTextValidation
}

/**
 * Checks a reason typed by the user. The ends are trimmed silently, because stray spaces from the keyboard
 * should neither count against the limit nor be stored. The repository does not validate, so callers use this
 * first.
 */
fun validateReasonText(raw: String): ReasonTextValidation {
    val text = raw.trim()
    return when {
        text.isEmpty() -> ReasonTextValidation.Blank
        text.length > MAX_REASON_LENGTH -> ReasonTextValidation.TooLong(MAX_REASON_LENGTH)
        else -> ReasonTextValidation.Valid(text)
    }
}
