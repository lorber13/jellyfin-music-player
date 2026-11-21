package io.github.lorber13.jellyfinmusic.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue

/** Lists the project's own Lint checks. Lint finds it through META-INF/services. */
class JellyfinMusicIssueRegistry : IssueRegistry() {
    override val issues: List<Issue> = listOf(HardcodedComposeTextDetector.ISSUE)

    override val api: Int = CURRENT_API

    // The checks always run with the Lint version they were built against, the one matching AGP.
    override val minApi: Int = CURRENT_API

    override val vendor: Vendor = Vendor(
        vendorName = "Jellyfin Music Player",
        feedbackUrl = "https://github.com/lorber13/jellyfin-music-player/issues"
    )
}
