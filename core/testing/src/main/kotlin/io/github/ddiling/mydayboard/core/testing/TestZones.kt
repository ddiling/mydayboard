package io.github.ddiling.mydayboard.core.testing

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * The zones tests use: [WARSAW], the default scenario zone, has daylight-saving time; [TOKYO] has none and is
 * 7–8 hours ahead, so "the user travelled" tests show a clear difference; [UTC] is the neutral reference.
 */
object TestZones {
    val WARSAW: ZoneId = ZoneId.of("Europe/Warsaw")
    val TOKYO: ZoneId = ZoneId.of("Asia/Tokyo")
    val UTC: ZoneId = ZoneOffset.UTC
}

/**
 * The instant at which clocks in Warsaw show this date and time, so a test reads like its scenario instead of
 * a UTC time with the offset worked out by hand. Daylight-saving edges follow [ZonedDateTime.of]: a time skipped
 * in spring moves forward by the gap; one that occurs twice in autumn takes the earlier (summer) offset.
 */
fun warsaw(
    year: Int,
    month: Int,
    day: Int,
    hour: Int = 0,
    minute: Int = 0,
    second: Int = 0,
    nano: Int = 0,
): Instant = ZonedDateTime.of(year, month, day, hour, minute, second, nano, TestZones.WARSAW).toInstant()
