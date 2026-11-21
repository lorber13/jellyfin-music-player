package io.github.lorber13.jellyfinmusic.feature.login

import androidx.activity.ComponentActivity
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.lorber13.jellyfinmusic.core.data.server.JellyfinServer
import io.github.lorber13.jellyfinmusic.core.data.server.PublicUser
import io.github.lorber13.jellyfinmusic.core.data.session.SignInFailure
import io.github.lorber13.jellyfinmusic.core.designsystem.theme.JellyfinMusicTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SignInContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val server = JellyfinServer(address = "http://localhost:8096", name = "Test server", version = "12.4.0")

    @Test
    fun showsTheServer() {
        showContent()

        composeRule.onNode(hasText("Test server")).assertExists()
        composeRule.onNode(hasText("http://localhost:8096")).assertExists()
    }

    @Test
    fun blankUsername_disablesSignIn() {
        showContent(username = TextFieldState(""))

        signInButton().assertIsNotEnabled()
    }

    @Test
    fun username_enablesSignIn() {
        showContent(username = TextFieldState("test"))

        signInButton().assertIsEnabled()
    }

    @Test
    fun wrongCredentials_showsTheError() {
        showContent(uiState = SignInUiState.Editing(failure = SignInFailure.WRONG_CREDENTIALS))

        composeRule.onNode(hasText(string(R.string.feature_login_error_wrong_credentials))).assertExists()
    }

    @Test
    fun tappingAPublicUser_picksThem() {
        var picked: PublicUser? = null
        val bob = PublicUser("2", "Bob")
        showContent(publicUsers = listOf(PublicUser("1", "test"), bob), onPickUser = { picked = it })

        composeRule.onNode(hasText("Bob") and hasClickAction()).performClick()

        assertEquals(bob, picked)
    }

    @Test
    fun quickConnectUnavailable_hidesTheButton() {
        showContent(isQuickConnectAvailable = false)

        quickConnectButton().assertDoesNotExist()
    }

    @Test
    fun quickConnectAvailable_buttonOpensQuickConnect() {
        var opened = false
        showContent(isQuickConnectAvailable = true, onUseQuickConnect = { opened = true })

        quickConnectButton().performClick()

        assertEquals(true, opened)
    }

    @Test
    fun eyeButton_switchesBetweenShowAndHide() {
        composeRule.setContent {
            var isVisible by remember { mutableStateOf(false) }
            JellyfinMusicTheme {
                SignInContent(
                    server = server,
                    username = TextFieldState("test"),
                    password = TextFieldState("secret"),
                    isPasswordVisible = isVisible,
                    publicUsers = emptyList(),
                    isQuickConnectAvailable = false,
                    uiState = SignInUiState.Editing(),
                    onTogglePasswordVisibility = { isVisible = !isVisible },
                    onPickUser = {},
                    onSignIn = {},
                    onChangeServer = {},
                    onUseQuickConnect = {}
                )
            }
        }

        composeRule.onNode(hasContentDescription(string(R.string.feature_login_show_password))).performClick()

        composeRule.onNode(hasContentDescription(string(R.string.feature_login_hide_password))).assertExists()
    }

    private fun quickConnectButton() =
        composeRule.onNode(hasText(string(R.string.feature_login_use_quick_connect)) and hasClickAction())

    private fun signInButton() =
        composeRule.onNode(hasText(string(R.string.feature_login_sign_in)) and hasClickAction())

    private fun showContent(
        username: TextFieldState = TextFieldState("test"),
        publicUsers: List<PublicUser> = emptyList(),
        isQuickConnectAvailable: Boolean = false,
        uiState: SignInUiState = SignInUiState.Editing(),
        onPickUser: (PublicUser) -> Unit = {},
        onUseQuickConnect: () -> Unit = {}
    ) {
        composeRule.setContent {
            JellyfinMusicTheme {
                SignInContent(
                    server = server,
                    username = username,
                    password = TextFieldState(),
                    isPasswordVisible = false,
                    publicUsers = publicUsers,
                    isQuickConnectAvailable = isQuickConnectAvailable,
                    uiState = uiState,
                    onTogglePasswordVisibility = {},
                    onPickUser = onPickUser,
                    onSignIn = {},
                    onChangeServer = {},
                    onUseQuickConnect = onUseQuickConnect
                )
            }
        }
    }

    private fun string(resource: Int): String = composeRule.activity.getString(resource)
}
