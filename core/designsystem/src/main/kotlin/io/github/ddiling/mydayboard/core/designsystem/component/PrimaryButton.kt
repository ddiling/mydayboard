package io.github.ddiling.mydayboard.core.designsystem.component

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import io.github.ddiling.mydayboard.core.designsystem.preview.PreviewSurface

/** The main call-to-action button. Enforces the 48dp minimum touch target. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
    ) {
        Text(text = text)
    }
}

// Rendered in light and dark; each becomes a screenshot test.
@PreviewLightDark
@Composable
private fun PrimaryButtonPreview() {
    PreviewSurface {
        PrimaryButton(text = "Continue", onClick = {})
    }
}

@PreviewLightDark
@Composable
private fun PrimaryButtonDisabledPreview() {
    PreviewSurface {
        PrimaryButton(text = "Continue", onClick = {}, enabled = false)
    }
}
