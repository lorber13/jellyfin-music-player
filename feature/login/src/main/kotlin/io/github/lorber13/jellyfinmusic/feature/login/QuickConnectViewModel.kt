package io.github.lorber13.jellyfinmusic.feature.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.lorber13.jellyfinmusic.core.data.server.JellyfinServer
import io.github.lorber13.jellyfinmusic.core.data.session.QuickConnectFailure
import io.github.lorber13.jellyfinmusic.core.data.session.QuickConnectRequest
import io.github.lorber13.jellyfinmusic.core.data.session.QuickConnectStartResult
import io.github.lorber13.jellyfinmusic.core.data.session.QuickConnectStatus
import io.github.lorber13.jellyfinmusic.core.data.session.SessionRepository
import io.github.lorber13.jellyfinmusic.core.data.session.SignInFailure
import io.github.lorber13.jellyfinmusic.core.data.session.SignInResult
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch

internal sealed interface QuickConnectUiState {
    data object RequestingCode : QuickConnectUiState

    /** [code] is on screen for the user to enter in another Jellyfin app. */
    data class WaitingForApproval(val code: String) : QuickConnectUiState

    data object SigningIn : QuickConnectUiState

    /** The session is set up; the screen hands over to the app. */
    data object SignedIn : QuickConnectUiState

    /** Nobody approved the code in time; the user can ask for a new one. */
    data object CodeExpired : QuickConnectUiState

    data class Failed(val reason: QuickConnectFailure) : QuickConnectUiState
}

/** How often to ask the server whether the code was approved. Jellyfin's own apps also use 5 seconds. */
internal val QuickConnectPollInterval = 5.seconds

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = QuickConnectViewModel.Factory::class)
internal class QuickConnectViewModel @AssistedInject constructor(
    @Assisted val server: JellyfinServer,
    private val sessionRepository: SessionRepository
) : ViewModel() {
    private val state = MutableStateFlow<QuickConnectUiState>(QuickConnectUiState.RequestingCode)

    // The request behind the code on screen. Its secret stays here, out of the UI state.
    private var request: QuickConnectRequest? = null

    /**
     * The screen's state. While a code waits for approval, this also asks the server about it,
     * but only while the screen collects it: a code left on screen in the background doesn't
     * keep calling the server. Coming back checks right away, so a code approved in another app
     * on this phone signs in as soon as the user returns.
     */
    val uiState: StateFlow<QuickConnectUiState> = state
        .transformLatest { current ->
            emit(current)
            val waitingRequest = request
            if (current is QuickConnectUiState.WaitingForApproval && waitingRequest != null) {
                waitForApproval(waitingRequest)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), state.value)

    init {
        startRequest()
    }

    /** Asks for a new code after the last one expired or couldn't be fetched. */
    fun requestNewCode() {
        val current = state.value
        if (current is QuickConnectUiState.CodeExpired || current is QuickConnectUiState.Failed) startRequest()
    }

    private fun startRequest() {
        request = null
        state.value = QuickConnectUiState.RequestingCode
        viewModelScope.launch {
            state.value = when (val result = sessionRepository.startQuickConnect(server)) {
                is QuickConnectStartResult.Started -> {
                    request = result.request
                    QuickConnectUiState.WaitingForApproval(result.request.code)
                }

                is QuickConnectStartResult.Failed -> QuickConnectUiState.Failed(result.reason)
            }
        }
    }

    private suspend fun waitForApproval(request: QuickConnectRequest) {
        while (true) {
            when (sessionRepository.checkQuickConnect(server, request)) {
                // A failed check is often a brief network drop; the next one may work.
                QuickConnectStatus.WAITING, QuickConnectStatus.CHECK_FAILED -> delay(QuickConnectPollInterval)

                QuickConnectStatus.APPROVED -> {
                    finishSignIn(request)
                    return
                }

                QuickConnectStatus.EXPIRED -> {
                    state.value = QuickConnectUiState.CodeExpired
                    return
                }
            }
        }
    }

    // Runs in viewModelScope rather than in the polling, which stops when the state changes or
    // the screen goes to the background: signing in must finish either way.
    private fun finishSignIn(request: QuickConnectRequest) {
        viewModelScope.launch {
            state.value = QuickConnectUiState.SigningIn
            state.value = when (val result = sessionRepository.signInWithQuickConnect(server, request)) {
                is SignInResult.Success -> QuickConnectUiState.SignedIn
                is SignInResult.Failed -> QuickConnectUiState.Failed(quickConnectFailureOf(result.reason))
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(server: JellyfinServer): QuickConnectViewModel
    }
}

/** The server turned down an approved code; to the user that is a server error like any other. */
private fun quickConnectFailureOf(reason: SignInFailure): QuickConnectFailure = when (reason) {
    SignInFailure.UNREACHABLE -> QuickConnectFailure.UNREACHABLE
    SignInFailure.WRONG_CREDENTIALS, SignInFailure.SERVER_ERROR -> QuickConnectFailure.SERVER_ERROR
}
