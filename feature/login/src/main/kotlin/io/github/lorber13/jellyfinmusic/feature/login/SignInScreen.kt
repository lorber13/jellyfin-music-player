package io.github.lorber13.jellyfinmusic.feature.login

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.github.lorber13.jellyfinmusic.core.data.server.JellyfinServer
import io.github.lorber13.jellyfinmusic.core.data.server.PublicUser
import io.github.lorber13.jellyfinmusic.core.data.session.SignInFailure
import io.github.lorber13.jellyfinmusic.core.designsystem.theme.JellyfinMusicTheme

@Composable
internal fun SignInScreen(
    route: SignInRoute,
    onSignedIn: () -> Unit,
    onChangeServer: () -> Unit,
    onUseQuickConnect: () -> Unit
) {
    val viewModel = hiltViewModel<SignInViewModel, SignInViewModel.Factory>(
        creationCallback = { factory -> factory.create(route.server) }
    )
    val uiState = viewModel.uiState
    // Signing in is a state, not a one-off event, as the UI events guidance recommends. Navigating
    // away removes this screen from the back stack, so the callback can't run twice.
    LaunchedEffect(uiState) {
        if (uiState == SignInUiState.SignedIn) onSignedIn()
    }
    SignInContent(
        server = viewModel.server,
        username = viewModel.username,
        password = viewModel.password,
        isPasswordVisible = viewModel.isPasswordVisible,
        publicUsers = viewModel.publicUsers,
        isQuickConnectAvailable = viewModel.isQuickConnectAvailable,
        uiState = uiState,
        onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
        onPickUser = viewModel::pickUser,
        onSignIn = viewModel::signIn,
        onChangeServer = onChangeServer,
        onUseQuickConnect = onUseQuickConnect
    )
}

@Composable
internal fun SignInContent(
    server: JellyfinServer,
    username: TextFieldState,
    password: TextFieldState,
    isPasswordVisible: Boolean,
    publicUsers: List<PublicUser>,
    isQuickConnectAvailable: Boolean,
    uiState: SignInUiState,
    onTogglePasswordVisibility: () -> Unit,
    onPickUser: (PublicUser) -> Unit,
    onSignIn: () -> Unit,
    onChangeServer: () -> Unit,
    onUseQuickConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Stays busy after success too, until the app has moved on to the next screen.
    val isSigningIn = uiState !is SignInUiState.Editing
    val failure = (uiState as? SignInUiState.Editing)?.failure
    val passwordFocusRequester = remember { FocusRequester() }

    Surface(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            // The same reading width as the server address screen.
            Column(
                modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.feature_login_sign_in_title),
                    style = MaterialTheme.typography.headlineMedium
                )
                ServerHeader(server = server, isEnabled = !isSigningIn, onChangeServer = onChangeServer)
                if (publicUsers.isNotEmpty()) {
                    PublicUserPicker(
                        users = publicUsers,
                        isEnabled = !isSigningIn,
                        onPickUser = { user ->
                            onPickUser(user)
                            // The username is filled in, so the password is what's left to type.
                            passwordFocusRequester.requestFocus()
                        }
                    )
                }
                UsernameField(state = username, isEnabled = !isSigningIn)
                PasswordField(
                    state = password,
                    isVisible = isPasswordVisible,
                    isEnabled = !isSigningIn,
                    failure = failure,
                    focusRequester = passwordFocusRequester,
                    onToggleVisibility = onTogglePasswordVisibility,
                    onDone = onSignIn
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    ProgressButton(
                        text = stringResource(R.string.feature_login_sign_in),
                        progressText = stringResource(R.string.feature_login_signing_in),
                        isInProgress = isSigningIn,
                        isEnabled = username.text.isNotBlank() && !isSigningIn,
                        onClick = onSignIn
                    )
                }
                if (isQuickConnectAvailable) {
                    HorizontalDivider()
                    // Aligned like the Sign in button, and no wider, so the other way in doesn't
                    // outweigh the main one.
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        OutlinedButton(onClick = onUseQuickConnect, enabled = !isSigningIn) {
                            Text(stringResource(R.string.feature_login_use_quick_connect))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerHeader(server: JellyfinServer, isEnabled: Boolean, onChangeServer: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = server.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = server.address,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = onChangeServer, enabled = isEnabled) {
            Text(stringResource(R.string.feature_login_change_server))
        }
    }
}

@Composable
private fun PublicUserPicker(users: List<PublicUser>, isEnabled: Boolean, onPickUser: (PublicUser) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.feature_login_public_users_label),
            style = MaterialTheme.typography.labelLarge
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (user in users) {
                SuggestionChip(
                    onClick = { onPickUser(user) },
                    enabled = isEnabled,
                    label = { Text(user.name) },
                    icon = { InitialAvatar(name = user.name) }
                )
            }
        }
    }
}

/** A circle with the first letter of [name], until the app loads the users' profile pictures. */
@Composable
private fun InitialAvatar(name: String) {
    Box(
        modifier = Modifier
            .size(SuggestionChipDefaults.IconSize)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
            // The chip's label already says the name.
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initialOf(name),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

/** The first character of [name], uppercased. Whole code points, so names starting with an emoji work. */
private fun initialOf(name: String): String = name.substring(0, name.offsetByCodePoints(0, 1)).uppercase()

@Composable
private fun UsernameField(state: TextFieldState, isEnabled: Boolean) {
    OutlinedTextField(
        state = state,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentType = ContentType.Username },
        enabled = isEnabled,
        label = { Text(stringResource(R.string.feature_login_username_label)) },
        lineLimits = TextFieldLineLimits.SingleLine,
        // Next moves the focus to the password field.
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Next
        )
    )
}

@Composable
private fun PasswordField(
    state: TextFieldState,
    isVisible: Boolean,
    isEnabled: Boolean,
    failure: SignInFailure?,
    focusRequester: FocusRequester,
    onToggleVisibility: () -> Unit,
    onDone: () -> Unit
) {
    OutlinedSecureTextField(
        state = state,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .semantics { contentType = ContentType.Password },
        enabled = isEnabled,
        label = { Text(stringResource(R.string.feature_login_password_label)) },
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                if (isVisible) {
                    Icon(
                        painter = painterResource(R.drawable.feature_login_ic_visibility_off),
                        contentDescription = stringResource(R.string.feature_login_hide_password)
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.feature_login_ic_visibility),
                        contentDescription = stringResource(R.string.feature_login_show_password)
                    )
                }
            }
        },
        supportingText = failure?.let { reason ->
            {
                Text(
                    text = stringResource(reason.messageResource),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
            }
        },
        isError = failure != null,
        textObfuscationMode = if (isVisible) TextObfuscationMode.Visible else TextObfuscationMode.RevealLastTyped,
        // Passing keyboardOptions replaces the secure field's defaults, so the password keyboard
        // (no suggestions, no learning of what is typed) has to be asked for again.
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Go
        ),
        onKeyboardAction = { onDone() }
    )
}

@get:StringRes
private val SignInFailure.messageResource: Int
    get() = when (this) {
        SignInFailure.WRONG_CREDENTIALS -> R.string.feature_login_error_wrong_credentials
        SignInFailure.UNREACHABLE -> R.string.feature_login_error_sign_in_unreachable
        SignInFailure.SERVER_ERROR -> R.string.feature_login_error_sign_in_server
    }

private val previewServer =
    JellyfinServer(address = "https://music.example.com", name = "Home music", version = "12.4.0")

@Preview
@Composable
private fun SignInWithUsersPreview() {
    JellyfinMusicTheme {
        SignInContent(
            server = previewServer,
            username = TextFieldState("alice"),
            password = TextFieldState("secret"),
            isPasswordVisible = false,
            publicUsers = listOf(PublicUser("1", "alice"), PublicUser("2", "Bob")),
            isQuickConnectAvailable = true,
            uiState = SignInUiState.Editing(failure = SignInFailure.WRONG_CREDENTIALS),
            onTogglePasswordVisibility = {},
            onPickUser = {},
            onSignIn = {},
            onChangeServer = {},
            onUseQuickConnect = {}
        )
    }
}

@Preview
@Composable
private fun SignInWithoutUsersPreview() {
    JellyfinMusicTheme {
        SignInContent(
            server = previewServer,
            username = TextFieldState(),
            password = TextFieldState(),
            isPasswordVisible = false,
            publicUsers = emptyList(),
            isQuickConnectAvailable = false,
            uiState = SignInUiState.Editing(),
            onTogglePasswordVisibility = {},
            onPickUser = {},
            onSignIn = {},
            onChangeServer = {},
            onUseQuickConnect = {}
        )
    }
}
