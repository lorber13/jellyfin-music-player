package io.github.lorber13.jellyfinmusic.core.designsystem.component

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.github.lorber13.jellyfinmusic.core.designsystem.R

/** The back arrow at the start of a title bar. */
@Composable
fun NavigateBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            painter = painterResource(R.drawable.designsystem_ic_arrow_back),
            contentDescription = stringResource(R.string.designsystem_navigate_back)
        )
    }
}
