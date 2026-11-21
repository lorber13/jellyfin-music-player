package io.github.lorber13.jellyfinmusic.feature.login

import io.github.lorber13.jellyfinmusic.core.data.server.JellyfinServer
import io.github.lorber13.jellyfinmusic.core.data.session.QuickConnectFailure
import io.github.lorber13.jellyfinmusic.core.data.session.QuickConnectRequest
import io.github.lorber13.jellyfinmusic.core.data.session.QuickConnectStartResult
import io.github.lorber13.jellyfinmusic.core.data.session.QuickConnectStatus
import io.github.lorber13.jellyfinmusic.core.data.session.SessionRepository
import io.github.lorber13.jellyfinmusic.core.data.session.SessionState
import io.github.lorber13.jellyfinmusic.core.data.session.SignInFailure
import io.github.lorber13.jellyfinmusic.core.data.session.SignInResult
import io.github.lorber13.jellyfinmusic.core.data.session.UserSession
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuickConnectViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val server = JellyfinServer(address = "http://localhost:8096", name = "Test server", version = "12.4.0")
    private val signedInSession = UserSession(
        serverAddress = server.address,
        serverName = server.name,
        userId = "1",
        userName = "test"
    )
    private val firstRequest = QuickConnectRequest(code = "111111", secret = "first")
    private val secondRequest = QuickConnectRequest(code = "222222", secret = "second")
    private val sessionRepository = FakeSessionRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        sessionRepository.startResults += QuickConnectStartResult.Started(firstRequest)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun showsTheCode() = runTest(dispatcher) {
        val viewModel = createViewModel()
        collectState(viewModel)
        assertEquals(QuickConnectUiState.RequestingCode, viewModel.uiState.value)

        runCurrent()

        assertEquals(QuickConnectUiState.WaitingForApproval("111111"), viewModel.uiState.value)
    }

    @Test
    fun checksRightAwayAndThenEveryFiveSeconds() = runTest(dispatcher) {
        val viewModel = createViewModel()
        collectState(viewModel)

        runCurrent()
        assertEquals(1, sessionRepository.checkedRequests.size)
        advanceTimeBy(QuickConnectPollInterval)
        runCurrent()
        assertEquals(2, sessionRepository.checkedRequests.size)
        advanceTimeBy(QuickConnectPollInterval)
        runCurrent()

        assertEquals(listOf(firstRequest, firstRequest, firstRequest), sessionRepository.checkedRequests)
    }

    @Test
    fun approval_signsIn() = runTest(dispatcher) {
        sessionRepository.statuses += listOf(QuickConnectStatus.WAITING, QuickConnectStatus.APPROVED)
        val viewModel = createViewModel()
        collectState(viewModel)

        runCurrent()
        advanceTimeBy(QuickConnectPollInterval)
        runCurrent()

        assertEquals(listOf(firstRequest), sessionRepository.signedInRequests)
        assertEquals(QuickConnectUiState.SignedIn, viewModel.uiState.value)
    }

    @Test
    fun failedCheck_keepsWaiting() = runTest(dispatcher) {
        sessionRepository.statuses += listOf(QuickConnectStatus.CHECK_FAILED, QuickConnectStatus.APPROVED)
        val viewModel = createViewModel()
        collectState(viewModel)

        runCurrent()
        assertEquals(QuickConnectUiState.WaitingForApproval("111111"), viewModel.uiState.value)
        advanceTimeBy(QuickConnectPollInterval)
        runCurrent()

        assertEquals(QuickConnectUiState.SignedIn, viewModel.uiState.value)
    }

    @Test
    fun expiredCode_stopsChecking() = runTest(dispatcher) {
        sessionRepository.statuses += QuickConnectStatus.EXPIRED
        val viewModel = createViewModel()
        collectState(viewModel)

        runCurrent()
        advanceTimeBy(60.seconds)
        runCurrent()

        assertEquals(QuickConnectUiState.CodeExpired, viewModel.uiState.value)
        assertEquals(1, sessionRepository.checkedRequests.size)
    }

    @Test
    fun requestNewCode_afterExpiry_showsAndChecksTheNewCode() = runTest(dispatcher) {
        sessionRepository.statuses += QuickConnectStatus.EXPIRED
        sessionRepository.startResults += QuickConnectStartResult.Started(secondRequest)
        val viewModel = createViewModel()
        collectState(viewModel)
        runCurrent()

        viewModel.requestNewCode()
        runCurrent()

        assertEquals(QuickConnectUiState.WaitingForApproval("222222"), viewModel.uiState.value)
        assertEquals(listOf(firstRequest, secondRequest), sessionRepository.checkedRequests)
    }

    @Test
    fun requestNewCode_whileWaiting_isIgnored() = runTest(dispatcher) {
        val viewModel = createViewModel()
        collectState(viewModel)
        runCurrent()

        viewModel.requestNewCode()
        runCurrent()

        assertEquals(1, sessionRepository.startCount)
        assertEquals(QuickConnectUiState.WaitingForApproval("111111"), viewModel.uiState.value)
    }

    @Test
    fun startFailure_showsTheReason() = runTest(dispatcher) {
        sessionRepository.startResults.clear()
        sessionRepository.startResults += QuickConnectStartResult.Failed(QuickConnectFailure.DISABLED)
        val viewModel = createViewModel()
        collectState(viewModel)

        runCurrent()

        assertEquals(QuickConnectUiState.Failed(QuickConnectFailure.DISABLED), viewModel.uiState.value)
    }

    @Test
    fun signInFailure_showsTheReason() = runTest(dispatcher) {
        sessionRepository.statuses += QuickConnectStatus.APPROVED
        sessionRepository.signInResult = SignInResult.Failed(SignInFailure.UNREACHABLE)
        val viewModel = createViewModel()
        collectState(viewModel)

        runCurrent()

        assertEquals(QuickConnectUiState.Failed(QuickConnectFailure.UNREACHABLE), viewModel.uiState.value)
    }

    @Test
    fun withoutAScreen_doesNotCheck() = runTest(dispatcher) {
        val viewModel = createViewModel()

        runCurrent()
        advanceTimeBy(60.seconds)
        runCurrent()
        assertEquals(0, sessionRepository.checkedRequests.size)

        // Coming back checks right away.
        collectState(viewModel)
        runCurrent()
        assertEquals(1, sessionRepository.checkedRequests.size)
    }

    @Test
    fun screenInTheBackground_stopsChecking() = runTest(dispatcher) {
        val viewModel = createViewModel()
        val screen = collectState(viewModel)
        runCurrent()

        screen.cancel()
        advanceTimeBy(10.seconds)
        val checksWhenStopped = sessionRepository.checkedRequests.size
        advanceTimeBy(60.seconds)
        runCurrent()

        assertEquals(checksWhenStopped, sessionRepository.checkedRequests.size)
    }

    private fun createViewModel() = QuickConnectViewModel(server, sessionRepository)

    /** Collects the state the way the visible screen does, which is what starts the checks. */
    private fun TestScope.collectState(viewModel: QuickConnectViewModel): Job =
        backgroundScope.launch { viewModel.uiState.collect {} }

    private inner class FakeSessionRepository : SessionRepository {
        val startResults = ArrayDeque<QuickConnectStartResult>()
        var startCount = 0

        /** The answers to the checks, in order. Once used up, every check answers WAITING. */
        val statuses = ArrayDeque<QuickConnectStatus>()
        val checkedRequests = mutableListOf<QuickConnectRequest>()

        var signInResult: SignInResult = SignInResult.Success(signedInSession)
        val signedInRequests = mutableListOf<QuickConnectRequest>()

        override val session: StateFlow<SessionState> = MutableStateFlow(SessionState.SignedOut)

        override suspend fun signIn(server: JellyfinServer, username: String, password: String): SignInResult =
            error("The Quick Connect screen doesn't sign in with a password")

        override suspend fun startQuickConnect(server: JellyfinServer): QuickConnectStartResult {
            startCount++
            return startResults.removeFirst()
        }

        override suspend fun checkQuickConnect(
            server: JellyfinServer,
            request: QuickConnectRequest
        ): QuickConnectStatus {
            checkedRequests += request
            return statuses.removeFirstOrNull() ?: QuickConnectStatus.WAITING
        }

        override suspend fun signInWithQuickConnect(
            server: JellyfinServer,
            request: QuickConnectRequest
        ): SignInResult {
            signedInRequests += request
            return signInResult
        }
    }
}
