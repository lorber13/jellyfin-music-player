plugins {
    id("jellyfinmusic.android.library")
    id("jellyfinmusic.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.github.lorber13.jellyfinmusic.core.data"
}

dependencies {
    // Only this module talks to the SDK; the rest of the app sees the data layer's own models.
    implementation(libs.jellyfin.core)
    // The SDK logs through SLF4J but doesn't ship it; without it the first request crashes.
    runtimeOnly(libs.slf4j.android)

    implementation(libs.androidx.datastore)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore.tink) {
        // Same classes as tink-android, but without the Android Keystore integration.
        exclude(group = "com.google.crypto.tink", module = "tink")
    }
    implementation(libs.tink.android)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
