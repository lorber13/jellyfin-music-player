package io.github.lorber13.jellyfinmusic.feature.login

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.lorber13.jellyfinmusic.core.data.server.JellyfinServer
import io.github.lorber13.jellyfinmusic.core.data.server.ServerLookupFailure
import io.github.lorber13.jellyfinmusic.core.designsystem.theme.JellyfinMusicTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ServerAddressContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun emptyAddress_disablesConnect() {
        showContent(address = "", uiState = ServerAddressUiState.Editing())

        composeRule.onNode(hasText(string(R.string.feature_login_connect))).assertIsNotEnabled()
    }

    @Test
    fun unreachableWithoutPermission_showsTheErrorAndTheLocalNetworkButton() {
        showContent(
            uiState = ServerAddressUiState.Editing(failure = ServerLookupFailure.UNREACHABLE),
            showLocalNetworkHint = true
        )

        composeRule.onNode(hasText(string(R.string.feature_login_error_unreachable))).assertExists()
        composeRule.onNode(hasText(string(R.string.feature_login_allow_local_network)) and hasClickAction())
            .assertExists()
    }

    @Test
    fun foundServer_showsItsNameAndContinuesWithIt() {
        var continuedWith: JellyfinServer? = null
        val server = JellyfinServer(address = "http://localhost:8096", name = "Test server", version = "12.4.0")
        showContent(uiState = ServerAddressUiState.Connected(server), onContinue = { continuedWith = it })

        composeRule.onNode(hasText("Test server")).assertExists()
        composeRule.onNode(hasText(string(R.string.feature_login_continue))).performClick()

        assertEquals(server, continuedWith)
    }

    private fun showContent(
        uiState: ServerAddressUiState,
        address: String = "localhost",
        showLocalNetworkHint: Boolean = false,
        onContinue: (JellyfinServer) -> Unit = {}
    ) {
        composeRule.setContent {
            JellyfinMusicTheme {
                ServerAddressContent(
                    address = address,
                    uiState = uiState,
                    showLocalNetworkHint = showLocalNetworkHint,
                    onAddressChange = {},
                    onConnect = {},
                    onAllowLocalNetworkAccess = {},
                    onChangeServer = {},
                    onContinue = onContinue
                )
            }
        }
    }

    private fun string(resource: Int): String = composeRule.activity.getString(resource)
}
