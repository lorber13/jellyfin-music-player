package io.github.lorber13.jellyfinmusic.feature.login

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import io.github.lorber13.jellyfinmusic.core.data.server.JellyfinServer
import io.github.lorber13.jellyfinmusic.core.data.server.PublicUser
import io.github.lorber13.jellyfinmusic.core.data.server.ServerLookupResult
import io.github.lorber13.jellyfinmusic.core.data.server.ServerRepository
import io.github.lorber13.jellyfinmusic.core.data.session.SessionRepository
import io.github.lorber13.jellyfinmusic.core.data.session.SignInFailure
import io.github.lorber13.jellyfinmusic.core.data.session.SignInResult
import io.github.lorber13.jellyfinmusic.core.data.session.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SignInViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val server = JellyfinServer(address = "http://localhost:8096", name = "Test server", version = "12.4.0")
    private val session = UserSession(
        serverAddress = server.address,
        serverName = server.name,
        userId = "1",
        userName = "test"
    )
    private val sessionRepository = FakeSessionRepository()
    private val serverRepository = FakeServerRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsThePublicUsers() = runTest(dispatcher) {
        serverRepository.publicUsers = listOf(PublicUser("1", "test"))

        val viewModel = createViewModel()
        runCurrent()

        assertEquals(listOf(PublicUser("1", "test")), viewModel.publicUsers)
    }

    @Test
    fun pickUser_fillsInTheUsername() = runTest(dispatcher) {
        val viewModel = createViewModel()

        viewModel.pickUser(PublicUser("2", "Bob"))

        assertEquals("Bob", viewModel.username.text.toString())
    }

    @Test
    fun signIn_sendsTheTrimmedUsernameAndThePassword() = runTest(dispatcher) {
        sessionRepository.result = SignInResult.Success(session)
        val viewModel = createViewModel()
        viewModel.username.setTextAndPlaceCursorAtEnd("test ")
        viewModel.password.setTextAndPlaceCursorAtEnd(" secret ")

        viewModel.signIn()
        assertEquals(SignInUiState.SigningIn, viewModel.uiState)
        runCurrent()

        assertEquals(listOf(Attempt(server, "test", " secret ")), sessionRepository.attempts)
        assertEquals(SignInUiState.SignedIn, viewModel.uiState)
    }

    @Test
    fun signInAfterSuccess_isIgnored() = runTest(dispatcher) {
        sessionRepository.result = SignInResult.Success(session)
        val viewModel = createViewModel()
        viewModel.username.setTextAndPlaceCursorAtEnd("test")
        viewModel.signIn()
        runCurrent()

        viewModel.signIn()
        runCurrent()

        assertEquals(1, sessionRepository.attempts.size)
    }

    @Test
    fun emptyPassword_isAllowed() = runTest(dispatcher) {
        sessionRepository.result = SignInResult.Success(session)
        val viewModel = createViewModel()
        viewModel.username.setTextAndPlaceCursorAtEnd("test")

        viewModel.signIn()
        runCurrent()

        assertEquals(listOf(Attempt(server, "test", "")), sessionRepository.attempts)
    }

    @Test
    fun wrongCredentials_returnsToEditingAndKeepsWhatWasTyped() = runTest(dispatcher) {
        sessionRepository.result = SignInResult.Failed(SignInFailure.WRONG_CREDENTIALS)
        val viewModel = createViewModel()
        viewModel.username.setTextAndPlaceCursorAtEnd("test")
        viewModel.password.setTextAndPlaceCursorAtEnd("wrong")

        viewModel.signIn()
        runCurrent()

        assertEquals(SignInUiState.Editing(failure = SignInFailure.WRONG_CREDENTIALS), viewModel.uiState)
        assertEquals("test", viewModel.username.text.toString())
        assertEquals("wrong", viewModel.password.text.toString())
    }

    @Test
    fun blankUsername_doesNothing() = runTest(dispatcher) {
        val viewModel = createViewModel()
        viewModel.username.setTextAndPlaceCursorAtEnd("  ")

        viewModel.signIn()
        runCurrent()

        assertEquals(SignInUiState.Editing(), viewModel.uiState)
        assertEquals(emptyList<Attempt>(), sessionRepository.attempts)
    }

    @Test
    fun signInWhileSigningIn_isIgnored() = runTest(dispatcher) {
        sessionRepository.result = SignInResult.Success(session)
        val viewModel = createViewModel()
        viewModel.username.setTextAndPlaceCursorAtEnd("test")

        viewModel.signIn()
        viewModel.signIn()
        runCurrent()

        assertEquals(1, sessionRepository.attempts.size)
    }

    @Test
    fun togglePasswordVisibility_showsAndHidesThePassword() = runTest(dispatcher) {
        val viewModel = createViewModel()

        viewModel.togglePasswordVisibility()
        assertEquals(true, viewModel.isPasswordVisible)
        viewModel.togglePasswordVisibility()
        assertEquals(false, viewModel.isPasswordVisible)
    }

    private fun createViewModel() = SignInViewModel(server, sessionRepository, serverRepository)

    private data class Attempt(val server: JellyfinServer, val username: String, val password: String)

    private class FakeSessionRepository : SessionRepository {
        var result: SignInResult = SignInResult.Failed(SignInFailure.UNREACHABLE)
        val attempts = mutableListOf<Attempt>()
        override val session: StateFlow<UserSession?> = MutableStateFlow(null)

        override suspend fun signIn(server: JellyfinServer, username: String, password: String): SignInResult {
            attempts += Attempt(server, username, password)
            return result
        }
    }

    private class FakeServerRepository : ServerRepository {
        var publicUsers: List<PublicUser> = emptyList()

        override suspend fun findServer(address: String): ServerLookupResult =
            error("The sign-in screen doesn't look up servers")

        override suspend fun getPublicUsers(serverAddress: String): List<PublicUser> = publicUsers
    }
}
