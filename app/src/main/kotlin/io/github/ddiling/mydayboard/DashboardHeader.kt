package io.github.ddiling.mydayboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal const val OPEN_SETTINGS_TAG = "open_settings"

/**
 * The top of the dashboard: a large clock, the date with the weekday under it, and the settings entry.
 * The clock is pulled left a little because a large glyph has side bearing, which would otherwise make it
 * look indented against the text below.
 */
@Composable
fun DashboardHeader(
    time: HeaderTime,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = time.time,
            fontSize = 56.sp,
            fontWeight = FontWeight.Light,
            modifier = Modifier.offset(x = (-4).dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = time.date, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Text(
                text = time.weekday,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onOpenSettings, modifier = Modifier.size(48.dp).testTag(OPEN_SETTINGS_TAG)) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = stringResource(R.string.dashboard_open_settings),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
