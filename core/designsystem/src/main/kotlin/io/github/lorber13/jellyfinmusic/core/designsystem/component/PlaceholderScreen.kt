package io.github.lorber13.jellyfinmusic.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.lorber13.jellyfinmusic.core.designsystem.R
import io.github.lorber13.jellyfinmusic.core.designsystem.theme.JellyfinMusicTheme

/**
 * A screen with a title bar and a "not built yet" message. It stands in for screens whose
 * backlog item has not been implemented.
 *
 * @param onNavigateBack shows a back arrow in the title bar when not null.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderScreen(title: String, modifier: Modifier = Modifier, onNavigateBack: (() -> Unit)? = null) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        NavigateBackButton(onClick = onNavigateBack)
                    }
                }
            )
        }
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.designsystem_placeholder_message),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Preview
@Composable
private fun PlaceholderScreenPreview() {
    JellyfinMusicTheme {
        PlaceholderScreen(title = "Title", onNavigateBack = {})
    }
}
