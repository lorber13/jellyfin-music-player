plugins {
    id("jellyfinmusic.android.library")
    id("jellyfinmusic.android.compose")
}

android {
    namespace = "io.github.lorber13.jellyfinmusic.core.designsystem"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    // The network fetcher is added by :app; covers are loaded from the server's URLs.
    implementation(libs.coil.compose)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
