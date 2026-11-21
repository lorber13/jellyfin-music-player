package io.github.lorber13.jellyfinmusic.core.data.session

/** The outcome of [SessionRepository.signIn]. */
sealed interface SignInResult {
    data class Success(val session: UserSession) : SignInResult

    data class Failed(val reason: SignInFailure) : SignInResult
}

/** Why signing in didn't work. */
enum class SignInFailure {
    /** The server rejected the username or password. */
    WRONG_CREDENTIALS,

    /** The server didn't answer: no network, server down, or local network access denied. */
    UNREACHABLE,

    /** The server answered, but with an error or a response the app couldn't read. */
    SERVER_ERROR
}
