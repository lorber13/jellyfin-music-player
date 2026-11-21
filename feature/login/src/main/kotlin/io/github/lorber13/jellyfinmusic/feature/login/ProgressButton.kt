package io.github.lorber13.jellyfinmusic.feature.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** A button that shows a spinner and [progressText] while its action runs. */
@Composable
internal fun ProgressButton(
    text: String,
    progressText: String,
    isInProgress: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    Button(onClick = onClick, enabled = isEnabled) {
        if (isInProgress) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = LocalContentColor.current,
                    strokeWidth = 2.dp
                )
                Text(progressText)
            }
        } else {
            Text(text)
        }
    }
}
