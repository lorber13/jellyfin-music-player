package io.github.lorber13.jellyfinmusic.feature.login

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.github.lorber13.jellyfinmusic.core.data.server.JellyfinServer
import io.github.lorber13.jellyfinmusic.core.data.server.ServerLookupFailure
import io.github.lorber13.jellyfinmusic.core.designsystem.theme.JellyfinMusicTheme

@Composable
internal fun ServerAddressScreen(onContinue: () -> Unit, viewModel: ServerAddressViewModel = hiltViewModel()) {
    val localNetworkAccess = rememberLocalNetworkAccess(onAnswered = viewModel::connect)
    val uiState = viewModel.uiState
    // Without the permission, a server on the home network looks exactly like one that is down.
    val failure = (uiState as? ServerAddressUiState.Editing)?.failure
    val showLocalNetworkHint = failure == ServerLookupFailure.UNREACHABLE && !localNetworkAccess.isGranted

    ServerAddressContent(
        address = viewModel.address,
        uiState = uiState,
        showLocalNetworkHint = showLocalNetworkHint,
        onAddressChange = viewModel::onAddressChange,
        onConnect = {
            if (localNetworkAccess.shouldAskBeforeConnecting) {
                // Connects once the user answers, whatever the answer: public servers work without it.
                localNetworkAccess.ask()
            } else {
                viewModel.connect()
            }
        },
        onAllowLocalNetworkAccess = localNetworkAccess::askAgainOrOpenSettings,
        onChangeServer = viewModel::changeServer,
        onContinue = onContinue
    )
}

@Composable
internal fun ServerAddressContent(
    address: String,
    uiState: ServerAddressUiState,
    showLocalNetworkHint: Boolean,
    onAddressChange: (String) -> Unit,
    onConnect: () -> Unit,
    onAllowLocalNetworkAccess: () -> Unit,
    onChangeServer: () -> Unit,
    onContinue: () -> Unit,
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
            // A comfortable reading width on tablets and in landscape.
            Column(
                modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.feature_login_server_title),
                    style = MaterialTheme.typography.headlineMedium
                )
                if (uiState is ServerAddressUiState.Connected) {
                    ServerFound(server = uiState.server, onContinue = onContinue, onChangeServer = onChangeServer)
                } else {
                    AddressForm(
                        address = address,
                        isChecking = uiState == ServerAddressUiState.Checking,
                        failure = (uiState as? ServerAddressUiState.Editing)?.failure,
                        showLocalNetworkHint = showLocalNetworkHint,
                        onAddressChange = onAddressChange,
                        onConnect = onConnect,
                        onAllowLocalNetworkAccess = onAllowLocalNetworkAccess
                    )
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.AddressForm(
    address: String,
    isChecking: Boolean,
    failure: ServerLookupFailure?,
    showLocalNetworkHint: Boolean,
    onAddressChange: (String) -> Unit,
    onConnect: () -> Unit,
    onAllowLocalNetworkAccess: () -> Unit
) {
    OutlinedTextField(
        value = address,
        onValueChange = onAddressChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = !isChecking,
        label = { Text(stringResource(R.string.feature_login_server_address_label)) },
        placeholder = { Text(stringResource(R.string.feature_login_server_address_placeholder)) },
        isError = failure != null,
        supportingText = failure?.let { reason ->
            {
                Text(
                    text = stringResource(reason.messageResource),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Go
        ),
        keyboardActions = KeyboardActions(onGo = { onConnect() })
    )
    if (showLocalNetworkHint) {
        Text(
            text = stringResource(R.string.feature_login_local_network_hint),
            style = MaterialTheme.typography.bodyMedium
        )
        OutlinedButton(onClick = onAllowLocalNetworkAccess) {
            Text(stringResource(R.string.feature_login_allow_local_network))
        }
    }
    Button(
        onClick = onConnect,
        modifier = Modifier.align(Alignment.End),
        enabled = address.isNotBlank() && !isChecking
    ) {
        if (isChecking) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = LocalContentColor.current,
                    strokeWidth = 2.dp
                )
                Text(stringResource(R.string.feature_login_checking))
            }
        } else {
            Text(stringResource(R.string.feature_login_connect))
        }
    }
}

@Composable
private fun ColumnScope.ServerFound(server: JellyfinServer, onContinue: () -> Unit, onChangeServer: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.feature_login_server_found),
                style = MaterialTheme.typography.labelLarge
            )
            Text(text = server.name, style = MaterialTheme.typography.titleLarge)
            Text(text = server.address, style = MaterialTheme.typography.bodyMedium)
        }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
        TextButton(onClick = onChangeServer) {
            Text(stringResource(R.string.feature_login_use_another_server))
        }
        Button(onClick = onContinue) {
            Text(stringResource(R.string.feature_login_continue))
        }
    }
}

@get:StringRes
private val ServerLookupFailure.messageResource: Int
    get() = when (this) {
        ServerLookupFailure.INVALID_ADDRESS -> R.string.feature_login_error_invalid_address
        ServerLookupFailure.UNREACHABLE -> R.string.feature_login_error_unreachable
        ServerLookupFailure.SECURE_CONNECTION_FAILED -> R.string.feature_login_error_secure_connection
        ServerLookupFailure.NOT_A_JELLYFIN_SERVER -> R.string.feature_login_error_not_jellyfin
    }

@Preview
@Composable
private fun ServerAddressUnreachablePreview() {
    JellyfinMusicTheme {
        ServerAddressContent(
            address = "192.168.1.10",
            uiState = ServerAddressUiState.Editing(failure = ServerLookupFailure.UNREACHABLE),
            showLocalNetworkHint = true,
            onAddressChange = {},
            onConnect = {},
            onAllowLocalNetworkAccess = {},
            onChangeServer = {},
            onContinue = {}
        )
    }
}

@Preview
@Composable
private fun ServerAddressConnectedPreview() {
    JellyfinMusicTheme {
        ServerAddressContent(
            address = "music.example.com",
            uiState = ServerAddressUiState.Connected(
                JellyfinServer(address = "https://music.example.com", name = "Home music", version = "12.4.0")
            ),
            showLocalNetworkHint = false,
            onAddressChange = {},
            onConnect = {},
            onAllowLocalNetworkAccess = {},
            onChangeServer = {},
            onContinue = {}
        )
    }
}
