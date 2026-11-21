package io.github.lorber13.jellyfinmusic.feature.login

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.VerbatimTtsAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withAnnotation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.lorber13.jellyfinmusic.core.data.server.JellyfinServer
import io.github.lorber13.jellyfinmusic.core.data.session.QuickConnectFailure
import io.github.lorber13.jellyfinmusic.core.designsystem.theme.JellyfinMusicTheme

@Composable
internal fun QuickConnectScreen(route: QuickConnectRoute, onSignedIn: () -> Unit, onUsePassword: () -> Unit) {
    val viewModel = hiltViewModel<QuickConnectViewModel, QuickConnectViewModel.Factory>(
        creationCallback = { factory -> factory.create(route.server) }
    )
    // Collecting with the lifecycle is what pauses the approval checks while the app is in the background.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // As on the sign-in screen: navigating away removes this screen, so this runs once.
    LaunchedEffect(uiState) {
        if (uiState == QuickConnectUiState.SignedIn) onSignedIn()
    }
    QuickConnectContent(
        server = viewModel.server,
        uiState = uiState,
        onRequestNewCode = viewModel::requestNewCode,
        onUsePassword = onUsePassword
    )
}

@Composable
internal fun QuickConnectContent(
    server: JellyfinServer,
    uiState: QuickConnectUiState,
    onRequestNewCode: () -> Unit,
    onUsePassword: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            // The same reading width as the other sign-in screens.
            Column(
                modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.feature_login_quick_connect_title),
                    style = MaterialTheme.typography.headlineMedium
                )
                Column {
                    Text(text = server.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = server.address,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = stringResource(R.string.feature_login_quick_connect_instructions),
                    style = MaterialTheme.typography.bodyLarge
                )
                if (uiState is QuickConnectUiState.WaitingForApproval) {
                    QuickConnectCode(code = uiState.code)
                }
                CodeStatus(uiState = uiState, onRequestNewCode = onRequestNewCode)
                HorizontalDivider()
                OutlinedButton(
                    onClick = onUsePassword,
                    modifier = Modifier.fillMaxWidth(),
                    // Once the code is approved, leaving would abandon a sign-in that is about to finish.
                    enabled = uiState != QuickConnectUiState.SigningIn && uiState != QuickConnectUiState.SignedIn
                ) {
                    Text(stringResource(R.string.feature_login_use_password))
                }
            }
        }
    }
}

@Composable
private fun QuickConnectCode(code: String) {
    // Screen readers would read "482193" as a large number; verbatim reads it digit by digit.
    val text = buildAnnotatedString {
        withAnnotation(VerbatimTtsAnnotation(code)) { append(code) }
    }
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.displayMedium,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
        // Spaced out so it is easy to copy one digit at a time.
        letterSpacing = 8.sp
    )
}

@Composable
private fun CodeStatus(uiState: QuickConnectUiState, onRequestNewCode: () -> Unit) {
    when (uiState) {
        QuickConnectUiState.RequestingCode ->
            ProgressStatus(stringResource(R.string.feature_login_quick_connect_requesting))

        is QuickConnectUiState.WaitingForApproval ->
            ProgressStatus(stringResource(R.string.feature_login_quick_connect_waiting))

        // Stays busy after success too, until the app has moved on to the next screen.
        QuickConnectUiState.SigningIn, QuickConnectUiState.SignedIn ->
            ProgressStatus(stringResource(R.string.feature_login_signing_in))

        QuickConnectUiState.CodeExpired -> ErrorStatus(
            message = stringResource(R.string.feature_login_quick_connect_expired),
            actionText = stringResource(R.string.feature_login_quick_connect_new_code),
            onAction = onRequestNewCode
        )

        is QuickConnectUiState.Failed -> ErrorStatus(
            message = stringResource(uiState.reason.messageResource),
            // A new code won't help while Quick Connect is off; the password button is the way on.
            actionText = if (uiState.reason == QuickConnectFailure.DISABLED) {
                null
            } else {
                stringResource(R.string.feature_login_try_again)
            },
            onAction = onRequestNewCode
        )
    }
}

@Composable
private fun ProgressStatus(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        Text(
            text = text,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ErrorStatus(message: String, actionText: String?, onAction: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = message,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        if (actionText != null) {
            Button(onClick = onAction) {
                Text(actionText)
            }
        }
    }
}

@get:StringRes
private val QuickConnectFailure.messageResource: Int
    get() = when (this) {
        QuickConnectFailure.DISABLED -> R.string.feature_login_quick_connect_disabled
        QuickConnectFailure.UNREACHABLE -> R.string.feature_login_error_sign_in_unreachable
        QuickConnectFailure.SERVER_ERROR -> R.string.feature_login_error_sign_in_server
    }

private val previewServer =
    JellyfinServer(address = "https://music.example.com", name = "Home music", version = "12.4.0")

@Preview
@Composable
private fun QuickConnectWaitingPreview() {
    JellyfinMusicTheme {
        QuickConnectContent(
            server = previewServer,
            uiState = QuickConnectUiState.WaitingForApproval(code = "482193"),
            onRequestNewCode = {},
            onUsePassword = {}
        )
    }
}

@Preview
@Composable
private fun QuickConnectExpiredPreview() {
    JellyfinMusicTheme {
        QuickConnectContent(
            server = previewServer,
            uiState = QuickConnectUiState.CodeExpired,
            onRequestNewCode = {},
            onUsePassword = {}
        )
    }
}
