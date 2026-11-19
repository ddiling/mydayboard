package io.github.ddiling.mydayboard.core.domain.tracker

/**
 * The longest tracker name, in Unicode code points. Long enough for a short phrase, short enough to fit on a
 * dashboard tile without truncating most names.
 */
const val MAX_TRACKER_NAME_LENGTH = 40

/** Result of [validateTrackerName]. The UI turns each failure into its own localized message. */
sealed interface TrackerNameValidation {
    /** The name is acceptable; [name] is the trimmed text to store. */
    data class Valid(
        val name: String,
    ) : TrackerNameValidation

    /** The name is empty or only whitespace, so the tracker could not be told apart on the dashboard. */
    data object Blank : TrackerNameValidation

    /** The trimmed name has more than [max] code points. [max] is included so the message can show the limit. */
    data class TooLong(
        val max: Int,
    ) : TrackerNameValidation
}

/**
 * Checks a tracker name typed by the user (decision D8).
 *
 * The name is trimmed first, so stray spaces from the keyboard never count against the limit or end up stored.
 * Duplicates are allowed: two trackers may share a name, and they are still told apart by their ids. The name is
 * the user's own text, so it is stored as typed (apart from trimming) and never translated.
 *
 * The length is counted in Unicode code points, not `String.length`: Kotlin strings are UTF-16, so an emoji such
 * as "😀" is two chars but one code point, and counting chars would allow only 20 of them. Code points are not
 * the same as what a user sees as one character (a family emoji joins several code points), but they are simple
 * and predictable, and the limit only has to keep names reasonably short.
 */
fun validateTrackerName(raw: String): TrackerNameValidation {
    val name = raw.trim()
    val codePoints = name.codePointCount(0, name.length)
    return when {
        name.isEmpty() -> TrackerNameValidation.Blank
        codePoints > MAX_TRACKER_NAME_LENGTH -> TrackerNameValidation.TooLong(MAX_TRACKER_NAME_LENGTH)
        else -> TrackerNameValidation.Valid(name)
    }
}
