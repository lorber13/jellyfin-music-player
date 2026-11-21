plugins {
    id("jellyfinmusic.android.library")
    id("jellyfinmusic.hilt")
}

android {
    namespace = "io.github.lorber13.jellyfinmusic.core.data"
}

dependencies {
    // Only this module talks to the SDK; the rest of the app sees the data layer's own models.
    implementation(libs.jellyfin.core)
    // The SDK logs through SLF4J but doesn't ship it; without it the first request crashes.
    runtimeOnly(libs.slf4j.android)

    testImplementation(libs.junit)
}
