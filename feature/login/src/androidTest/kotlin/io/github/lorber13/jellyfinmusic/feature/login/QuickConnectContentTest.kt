package io.github.lorber13.jellyfinmusic.feature.login

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.lorber13.jellyfinmusic.core.data.server.JellyfinServer
import io.github.lorber13.jellyfinmusic.core.data.session.QuickConnectFailure
import io.github.lorber13.jellyfinmusic.core.designsystem.theme.JellyfinMusicTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuickConnectContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val server = JellyfinServer(address = "http://localhost:8096", name = "Test server", version = "12.4.0")

    @Test
    fun waiting_showsTheCodeAndTheServer() {
        showContent(uiState = QuickConnectUiState.WaitingForApproval(code = "482193"))

        composeRule.onNode(hasText("482193")).assertExists()
        composeRule.onNode(hasText("Test server")).assertExists()
        composeRule.onNode(hasText(string(R.string.feature_login_quick_connect_waiting))).assertExists()
    }

    @Test
    fun expired_offersANewCode() {
        var requested = false
        showContent(uiState = QuickConnectUiState.CodeExpired, onRequestNewCode = { requested = true })

        composeRule.onNode(hasText(string(R.string.feature_login_quick_connect_expired))).assertExists()
        button(R.string.feature_login_quick_connect_new_code).performClick()

        assertEquals(true, requested)
    }

    @Test
    fun unreachable_offersToTryAgain() {
        showContent(uiState = QuickConnectUiState.Failed(QuickConnectFailure.UNREACHABLE))

        composeRule.onNode(hasText(string(R.string.feature_login_error_sign_in_unreachable))).assertExists()
        button(R.string.feature_login_try_again).assertExists()
    }

    @Test
    fun disabled_doesNotOfferToTryAgain() {
        showContent(uiState = QuickConnectUiState.Failed(QuickConnectFailure.DISABLED))

        composeRule.onNode(hasText(string(R.string.feature_login_quick_connect_disabled))).assertExists()
        button(R.string.feature_login_try_again).assertDoesNotExist()
    }

    @Test
    fun passwordButton_goesBackToThePasswordForm() {
        var usedPassword = false
        showContent(onUsePassword = { usedPassword = true })

        button(R.string.feature_login_use_password).assertIsEnabled().performClick()

        assertEquals(true, usedPassword)
    }

    @Test
    fun signingIn_disablesThePasswordButton() {
        showContent(uiState = QuickConnectUiState.SigningIn)

        button(R.string.feature_login_use_password).assertIsNotEnabled()
    }

    private fun button(resource: Int) = composeRule.onNode(hasText(string(resource)) and hasClickAction())

    private fun showContent(
        uiState: QuickConnectUiState = QuickConnectUiState.WaitingForApproval(code = "482193"),
        onRequestNewCode: () -> Unit = {},
        onUsePassword: () -> Unit = {}
    ) {
        composeRule.setContent {
            JellyfinMusicTheme {
                QuickConnectContent(
                    server = server,
                    uiState = uiState,
                    onRequestNewCode = onRequestNewCode,
                    onUsePassword = onUsePassword
                )
            }
        }
    }

    private fun string(resource: Int): String = composeRule.activity.getString(resource)
}
