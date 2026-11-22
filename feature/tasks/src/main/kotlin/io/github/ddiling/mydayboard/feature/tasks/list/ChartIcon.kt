package io.github.ddiling.mydayboard.feature.tasks.list

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import io.github.ddiling.mydayboard.core.designsystem.theme.IconStrokeColor

private const val VIEWPORT = 24f

/** Three thin bars for "statistics". Drawn here because the core Material icon set has no chart icon. */
internal val ChartIcon: ImageVector by lazy {
    ImageVector
        .Builder("Chart", VIEWPORT.dp, VIEWPORT.dp, VIEWPORT, VIEWPORT)
        .path(
            stroke = SolidColor(IconStrokeColor),
            strokeLineWidth = 1.75f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(6f, 19f)
            verticalLineTo(12f)
            moveTo(12f, 19f)
            verticalLineTo(5f)
            moveTo(18f, 19f)
            verticalLineTo(9f)
        }.build()
}
