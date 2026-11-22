package io.github.ddiling.mydayboard.core.domain.text

import java.text.Normalizer

/**
 * Normalizes a name typed by the user, shared by every name validator so trackers, tasks and categories treat
 * text the same way: Unicode NFC (one letter has one code point sequence however the keyboard typed it), every
 * run of whitespace becomes one space, and the ends are trimmed.
 */
internal fun normalizeName(raw: String): String =
    collapseWhitespace(Normalizer.normalize(raw, Normalizer.Form.NFC)).trim()

/**
 * True when no code point of the normalized [name] is visible: whitespace, a control character (Cc) or a format
 * character (Cf, such as the zero-width space). An empty name is blank too.
 */
internal fun hasNoVisibleCharacter(name: String): Boolean = name.codePoints().allMatch(::isInvisible)

/** Number of Unicode code points, the unit every name limit is counted in (an emoji is 2 chars but 1 code point). */
internal fun String.codePointLength(): Int = codePointCount(0, length)

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
