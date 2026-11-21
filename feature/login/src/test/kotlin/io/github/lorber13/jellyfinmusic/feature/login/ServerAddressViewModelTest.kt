package io.github.lorber13.jellyfinmusic.feature.login

import io.github.lorber13.jellyfinmusic.core.data.server.JellyfinServer
import io.github.lorber13.jellyfinmusic.core.data.server.PublicUser
import io.github.lorber13.jellyfinmusic.core.data.server.ServerLookupFailure
import io.github.lorber13.jellyfinmusic.core.data.server.ServerLookupResult
import io.github.lorber13.jellyfinmusic.core.data.server.ServerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class ServerAddressViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val server = JellyfinServer(address = "http://localhost:8096", name = "Test server", version = "12.4.0")
    private val repository = FakeServerRepository()
    private lateinit var viewModel: ServerAddressViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = ServerAddressViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun foundServer_showsItAsConnected() = runTest(dispatcher) {
        repository.result = ServerLookupResult.Found(server)
        viewModel.onAddressChange("localhost")

        viewModel.connect()
        assertEquals(ServerAddressUiState.Checking, viewModel.uiState)
        runCurrent()

        assertEquals(ServerAddressUiState.Connected(server), viewModel.uiState)
        assertEquals(listOf("localhost"), repository.lookedUpAddresses)
    }

    @Test
    fun failedLookup_returnsToEditingWithTheReason() = runTest(dispatcher) {
        repository.result = ServerLookupResult.Failed(ServerLookupFailure.UNREACHABLE)
        viewModel.onAddressChange("nowhere.example.com")

        viewModel.connect()
        runCurrent()

        assertEquals(ServerAddressUiState.Editing(failure = ServerLookupFailure.UNREACHABLE), viewModel.uiState)
    }

    @Test
    fun connectWithBlankAddress_doesNothing() = runTest(dispatcher) {
        viewModel.onAddressChange("   ")

        viewModel.connect()
        runCurrent()

        assertEquals(ServerAddressUiState.Editing(), viewModel.uiState)
        assertEquals(emptyList<String>(), repository.lookedUpAddresses)
    }

    @Test
    fun connectWhileChecking_isIgnored() = runTest(dispatcher) {
        repository.result = ServerLookupResult.Found(server)
        viewModel.onAddressChange("localhost")

        viewModel.connect()
        viewModel.connect()
        runCurrent()

        assertEquals(1, repository.lookedUpAddresses.size)
    }

    @Test
    fun changeServer_goesBackToEditingAndKeepsTheAddress() = runTest(dispatcher) {
        repository.result = ServerLookupResult.Found(server)
        viewModel.onAddressChange("localhost")
        viewModel.connect()
        runCurrent()

        viewModel.changeServer()

        assertEquals(ServerAddressUiState.Editing(), viewModel.uiState)
        assertEquals("localhost", viewModel.address)
    }

    @Test
    fun comingBackAfterContinue_showsTheAddressFieldWithTheAddress() = runTest(dispatcher) {
        repository.result = ServerLookupResult.Found(server)
        viewModel.onAddressChange("localhost")
        viewModel.connect()
        runCurrent()

        viewModel.continueWithServer()
        assertEquals(ServerAddressUiState.Connected(server), viewModel.uiState)
        viewModel.onShown()

        assertEquals(ServerAddressUiState.Editing(), viewModel.uiState)
        assertEquals("localhost", viewModel.address)
    }

    @Test
    fun shownAgainWithoutContinuing_keepsTheFoundServer() = runTest(dispatcher) {
        repository.result = ServerLookupResult.Found(server)
        viewModel.onAddressChange("localhost")
        viewModel.connect()
        runCurrent()

        // E.g. after rotating the screen.
        viewModel.onShown()

        assertEquals(ServerAddressUiState.Connected(server), viewModel.uiState)
    }

    private class FakeServerRepository : ServerRepository {
        var result: ServerLookupResult = ServerLookupResult.Failed(ServerLookupFailure.UNREACHABLE)
        val lookedUpAddresses = mutableListOf<String>()

        override suspend fun findServer(address: String): ServerLookupResult {
            lookedUpAddresses += address
            return result
        }

        override suspend fun getPublicUsers(serverAddress: String): List<PublicUser> = emptyList()

        override suspend fun isQuickConnectEnabled(serverAddress: String): Boolean = false
    }
}
