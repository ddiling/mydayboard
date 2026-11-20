package io.github.ddiling.mydayboard.core.domain.tracker

import java.text.Normalizer

/**
 * The longest tracker name, in Unicode code points. Long enough for a short phrase, short enough to fit on a
 * dashboard tile without truncating most names.
 */
const val MAX_TRACKER_NAME_LENGTH = 40

/** Result of [validateTrackerName]. The UI turns each failure into its own localized message. */
sealed interface TrackerNameValidation {
    /** The name is acceptable; [name] is the normalized text to store. */
    data class Valid(
        val name: String,
    ) : TrackerNameValidation

    /** The name has no visible character, so the tracker would show an empty title on the dashboard. */
    data object Blank : TrackerNameValidation

    /** The normalized name has more than [max] code points. [max] is included so the message can show the limit. */
    data class TooLong(
        val max: Int,
    ) : TrackerNameValidation
}

/**
 * Checks a tracker name typed by the user: after normalizing it must have 1 to [MAX_TRACKER_NAME_LENGTH] code
 * points, at least one of them visible. The name is normalized silently instead of rejected, in this order:
 *
 * 1. Unicode NFC. The same letter can be typed as one code point ("ó") or as a base letter plus a combining accent
 *    ("o" + U+0301), depending on the keyboard. Without NFC a decomposed Polish name could be rejected as too long
 *    while the same name typed composed is fine, and two identical-looking names would be different strings.
 * 2. Every run of whitespace, including line breaks and tabs, becomes one space, and the ends are trimmed. A name
 *    is shown on one line, and stray spaces from the keyboard should neither count against the limit nor be stored.
 * 3. A name with no visible character is [TrackerNameValidation.Blank]. Invisible means whitespace, a control
 *    character (Unicode category Cc) or a format character (Cf, such as the zero-width space U+200B). Only the
 *    whole name is checked: an emoji sequence joined by U+200D is visible and stays valid.
 *
 * Duplicates are allowed: two trackers may share a name, and they are still told apart by their ids. The name is
 * the user's own text, so apart from this normalization it is stored as typed and never translated.
 *
 * The length is counted in Unicode code points, not `String.length`: Kotlin strings are UTF-16, so an emoji such
 * as "😀" is two chars but one code point, and counting chars would allow only 20 of them. Code points are not
 * the same as what a user sees as one character (a family emoji joins several code points), but they are simple
 * and predictable, and the limit only has to keep names reasonably short.
 */
fun validateTrackerName(raw: String): TrackerNameValidation {
    val name = collapseWhitespace(Normalizer.normalize(raw, Normalizer.Form.NFC)).trim()
    val codePoints = name.codePointCount(0, name.length)
    // An empty name is Blank too: every one of its zero code points is invisible.
    return when {
        name.codePoints().allMatch(::isInvisible) -> TrackerNameValidation.Blank
        codePoints > MAX_TRACKER_NAME_LENGTH -> TrackerNameValidation.TooLong(MAX_TRACKER_NAME_LENGTH)
        else -> TrackerNameValidation.Valid(name)
    }
}

/**
 * Replaces every run of Unicode whitespace (line breaks, tabs, no-break and other wide spaces) with one space.
 *
 * Plain code, not a regex: the JVM needs the `(?U)` flag for a Unicode `\s`, and Android's ICU regex engine
 * rejects that flag at runtime, so a regex would pass the JVM unit tests and crash on a device.
 */
private fun collapseWhitespace(text: String): String =
    buildString {
        var inRun = false
        text.codePoints().forEach { codePoint ->
            if (isUnicodeWhitespace(codePoint)) {
                if (!inRun) append(' ')
                inRun = true
            } else {
                appendCodePoint(codePoint)
                inRun = false
            }
        }
    }

/** Unicode White_Space: ASCII whitespace, NEL (U+0085), and space (incl. no-break), line and paragraph separators. */
private fun isUnicodeWhitespace(codePoint: Int): Boolean =
    Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint) || codePoint == NEXT_LINE

private const val NEXT_LINE = 0x85

private fun isInvisible(codePoint: Int): Boolean {
    val category = Character.getType(codePoint)
    // After the collapse and trim, the only whitespace left is the plain space, which isWhitespace covers.
    return Character.isWhitespace(codePoint) ||
        category == Character.FORMAT.toInt() ||
        category == Character.CONTROL.toInt()
}
