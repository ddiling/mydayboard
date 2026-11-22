package io.github.ddiling.mydayboard.feature.tasks.form

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.ddiling.mydayboard.core.designsystem.theme.DashedSeparatorColor

private val DashLength = 4.dp
private val UnderlineWidth = 1.dp

/**
 * The form's field frame: the label above, the [content], a dashed underline and an optional [error] below.
 * Material's outlined box is a card border, which the app's look avoids; the underline matches its dashed dividers.
 */
@Composable
internal fun DashedField(
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    content: @Composable () -> Unit,
) {
    val lineColor = if (error == null) DashedSeparatorColor else MaterialTheme.colorScheme.error
    Column(modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    val dashes = PathEffect.dashPathEffect(floatArrayOf(DashLength.toPx(), DashLength.toPx()))
                    drawLine(
                        lineColor,
                        Offset(0f, size.height),
                        Offset(size.width, size.height),
                        UnderlineWidth.toPx(),
                        pathEffect = dashes,
                    )
                }.padding(vertical = 8.dp),
        ) { content() }
        if (error != null) {
            Text(
                error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/**
 * A single-line text input in a [DashedField]; [modifier] goes on the input itself so tests can find it.
 * The visible label is a separate `Text`, so the input gets the label as its accessible name and the [error]
 * as its error semantics; otherwise a screen reader announces an unnamed edit box without the problem.
 */
@Composable
internal fun DashedTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    DashedField(label, error = error) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = keyboardOptions,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier =
                modifier.fillMaxWidth().semantics {
                    contentDescription = label
                    if (error != null) error(error)
                },
        )
    }
}
