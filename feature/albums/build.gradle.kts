plugins {
    id("jellyfinmusic.android.library")
    id("jellyfinmusic.android.compose")
    id("jellyfinmusic.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.github.lorber13.jellyfinmusic.feature.albums"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:data"))
    implementation(project(":core:playback"))

    implementation(libs.navigation3.runtime)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    // Compose UI tests bring in an older Espresso that crashes on Android 16 and newer.
    androidTestImplementation(libs.androidx.test.espresso.core)
    debugImplementation(libs.compose.ui.test.manifest)
}
