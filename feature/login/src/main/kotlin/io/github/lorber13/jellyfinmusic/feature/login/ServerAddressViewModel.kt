package io.github.lorber13.jellyfinmusic.feature.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.lorber13.jellyfinmusic.core.data.server.JellyfinServer
import io.github.lorber13.jellyfinmusic.core.data.server.ServerLookupFailure
import io.github.lorber13.jellyfinmusic.core.data.server.ServerLookupResult
import io.github.lorber13.jellyfinmusic.core.data.server.ServerRepository
import javax.inject.Inject
import kotlinx.coroutines.launch

internal sealed interface ServerAddressUiState {
    /** The user is typing an address. [failure] explains why the last check failed, if it did. */
    data class Editing(val failure: ServerLookupFailure? = null) : ServerAddressUiState

    data object Checking : ServerAddressUiState

    data class Connected(val server: JellyfinServer) : ServerAddressUiState
}

@HiltViewModel
internal class ServerAddressViewModel @Inject constructor(private val serverRepository: ServerRepository) :
    ViewModel() {
    var address by mutableStateOf("")
        private set

    var uiState: ServerAddressUiState by mutableStateOf(ServerAddressUiState.Editing())
        private set

    private var hasContinued = false

    fun onAddressChange(newAddress: String) {
        address = newAddress
    }

    /** Checks the entered address. Does nothing while a check is running or the address is blank. */
    fun connect() {
        if (uiState == ServerAddressUiState.Checking || address.isBlank()) return
        uiState = ServerAddressUiState.Checking
        viewModelScope.launch {
            uiState = when (val result = serverRepository.findServer(address)) {
                is ServerLookupResult.Found -> ServerAddressUiState.Connected(result.server)
                is ServerLookupResult.Failed -> ServerAddressUiState.Editing(failure = result.reason)
            }
        }
    }

    /** Goes back to editing the address after a server was found. */
    fun changeServer() {
        uiState = ServerAddressUiState.Editing()
    }

    /** The user moved on to sign in to the found server. */
    fun continueWithServer() {
        hasContinued = true
    }

    /**
     * Called each time the screen is shown. Coming back from signing in, by Back or "Change
     * server", means the user wants another server, so the address field is shown again instead
     * of the found server. The state isn't reset on Continue itself: the screen would visibly
     * switch to the address field while it fades out.
     */
    fun onShown() {
        if (!hasContinued) return
        hasContinued = false
        changeServer()
    }
}
