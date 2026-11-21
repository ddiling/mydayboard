package io.github.ddiling.mydayboard.feature.trackers.dashboard

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import io.github.ddiling.mydayboard.core.designsystem.theme.IconStrokeColor

/*
 * Drawn here as vectors because the Material icon set is not a dependency, and these three thin line icons are
 * all the card needs. They are black strokes that `Icon` tints.
 */

private const val VIEWPORT = 24f
private const val CENTRE = VIEWPORT / 2

private fun strokeIcon(
    name: String,
    strokeWidth: Float,
    draw: PathBuilder.() -> Unit,
): ImageVector =
    ImageVector
        .Builder(name, VIEWPORT.dp, VIEWPORT.dp, VIEWPORT, VIEWPORT)
        .path(
            stroke = SolidColor(IconStrokeColor),
            strokeLineWidth = strokeWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = draw,
        ).build()

private fun PathBuilder.cross(halfLength: Float) {
    moveTo(CENTRE, CENTRE - halfLength)
    verticalLineToRelative(2 * halfLength)
    moveTo(CENTRE - halfLength, CENTRE)
    horizontalLineToRelative(2 * halfLength)
}

/** A heartbeat line: the section's symbol. */
internal val PulseIcon: ImageVector by lazy {
    strokeIcon("Pulse", 2f) {
        moveTo(3f, CENTRE)
        horizontalLineToRelative(4f)
        lineToRelative(3f, -7f)
        lineToRelative(4f, 14f)
        lineToRelative(3f, -7f)
        horizontalLineToRelative(4f)
    }
}

internal val PlusIcon: ImageVector by lazy { strokeIcon("Plus", 1.75f) { cross(halfLength = 7f) } }

/** A thin circle with a centred plus: the dim "add" place. */
internal val PlusCircleIcon: ImageVector by lazy {
    strokeIcon("PlusCircle", 1f) {
        moveTo(1f, CENTRE)
        arcToRelative(11f, 11f, 0f, isMoreThanHalf = true, isPositiveArc = true, 22f, 0f)
        arcToRelative(11f, 11f, 0f, isMoreThanHalf = true, isPositiveArc = true, -22f, 0f)
        cross(halfLength = 5f)
    }
}
