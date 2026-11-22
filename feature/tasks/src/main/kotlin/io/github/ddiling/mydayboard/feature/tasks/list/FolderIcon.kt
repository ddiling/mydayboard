package io.github.ddiling.mydayboard.feature.tasks.list

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import io.github.ddiling.mydayboard.core.designsystem.theme.IconStrokeColor

private const val VIEWPORT = 24f

/** A thin folder outline for a category title. Drawn here because the Material icon set is not a dependency. */
internal val FolderIcon: ImageVector by lazy {
    ImageVector
        .Builder("Folder", VIEWPORT.dp, VIEWPORT.dp, VIEWPORT, VIEWPORT)
        .path(
            stroke = SolidColor(IconStrokeColor),
            strokeLineWidth = 1.75f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(3f, 6f)
            verticalLineToRelative(12f)
            horizontalLineToRelative(18f)
            verticalLineTo(8f)
            horizontalLineTo(11f)
            lineTo(9f, 6f)
            close()
        }.build()
}
