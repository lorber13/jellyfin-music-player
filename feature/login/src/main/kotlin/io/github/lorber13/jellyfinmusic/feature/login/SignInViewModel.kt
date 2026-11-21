package io.github.lorber13.jellyfinmusic.feature.login

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.lorber13.jellyfinmusic.core.data.server.JellyfinServer
import io.github.lorber13.jellyfinmusic.core.data.server.PublicUser
import io.github.lorber13.jellyfinmusic.core.data.server.ServerRepository
import io.github.lorber13.jellyfinmusic.core.data.session.SessionRepository
import io.github.lorber13.jellyfinmusic.core.data.session.SignInFailure
import io.github.lorber13.jellyfinmusic.core.data.session.SignInResult
import kotlinx.coroutines.launch

internal sealed interface SignInUiState {
    /** The user is filling in the form. [failure] explains why the last attempt failed, if it did. */
    data class Editing(val failure: SignInFailure? = null) : SignInUiState

    /** Also the state after success, until the app replaces the sign-in flow with the shell. */
    data object SigningIn : SignInUiState
}

@HiltViewModel(assistedFactory = SignInViewModel.Factory::class)
internal class SignInViewModel @AssistedInject constructor(
    @Assisted val server: JellyfinServer,
    private val sessionRepository: SessionRepository,
    serverRepository: ServerRepository
) : ViewModel() {
    val username = TextFieldState()
    val password = TextFieldState()

    var isPasswordVisible by mutableStateOf(false)
        private set

    var publicUsers: List<PublicUser> by mutableStateOf(emptyList())
        private set

    var uiState: SignInUiState by mutableStateOf(SignInUiState.Editing())
        private set

    init {
        viewModelScope.launch {
            publicUsers = serverRepository.getPublicUsers(server.address)
        }
    }

    fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
    }

    fun pickUser(user: PublicUser) {
        username.setTextAndPlaceCursorAtEnd(user.name)
    }

    /**
     * Signs in with the entered username and password. Does nothing while signing in or when the
     * username is blank. The password may be empty: Jellyfin allows users without one.
     */
    fun signIn() {
        // Keyboards and password managers sometimes add a trailing space; Jellyfin names never have one.
        val name = username.text.toString().trim()
        if (uiState == SignInUiState.SigningIn || name.isEmpty()) return
        uiState = SignInUiState.SigningIn
        viewModelScope.launch {
            val result = sessionRepository.signIn(server, name, password.text.toString())
            if (result is SignInResult.Failed) {
                uiState = SignInUiState.Editing(failure = result.reason)
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(server: JellyfinServer): SignInViewModel
    }
}
