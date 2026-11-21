package io.github.lorber13.jellyfinmusic.core.data.session

/**
 * A Quick Connect sign-in waiting for approval. The user enters [code] in another Jellyfin
 * client; [secret] identifies the request to the server and is never shown.
 */
data class QuickConnectRequest(val code: String, val secret: String) {
    // Keeps the secret out of logs and crash output.
    override fun toString(): String = "QuickConnectRequest(code=$code)"
}

/** The outcome of [SessionRepository.startQuickConnect]. */
sealed interface QuickConnectStartResult {
    data class Started(val request: QuickConnectRequest) : QuickConnectStartResult

    data class Failed(val reason: QuickConnectFailure) : QuickConnectStartResult
}

/** Why the server didn't hand out a Quick Connect code. */
enum class QuickConnectFailure {
    /** Quick Connect is turned off on the server. */
    DISABLED,

    /** The server didn't answer: no network, server down, or local network access denied. */
    UNREACHABLE,

    /** The server answered, but with an error or a response the app couldn't read. */
    SERVER_ERROR
}

/** Where a [QuickConnectRequest] stands, from [SessionRepository.checkQuickConnect]. */
enum class QuickConnectStatus {
    /** Nobody approved the code yet. */
    WAITING,

    /** The code was approved; [SessionRepository.signInWithQuickConnect] can finish signing in. */
    APPROVED,

    /** The server no longer knows the request, usually because the code timed out. */
    EXPIRED,

    /** This check didn't get an answer. The request may still be waiting. */
    CHECK_FAILED
}
