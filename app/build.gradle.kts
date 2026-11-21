plugins {
    id("jellyfinmusic.android.application")
    id("jellyfinmusic.android.compose")
    id("jellyfinmusic.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.github.lorber13.jellyfinmusic"

    defaultConfig {
        applicationId = "io.github.lorber13.jellyfinmusic"
        versionCode = 1
        versionName = "0.1.0"
        // Runs the instrumented tests with Hilt's test application, so tests can replace bindings.
        testInstrumentationRunner = "io.github.lorber13.jellyfinmusic.HiltTestRunner"
    }

    buildTypes {
        debug {
            // Adds the en-XA (accented, longer text) and ar-XB (right-to-left) test languages,
            // to spot untranslated text, truncation and layout mirroring problems.
            isPseudoLocalesEnabled = true
        }
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:data"))
    implementation(project(":feature:albums"))
    implementation(project(":feature:login"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.material3.adaptive.navigation.suite)
    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.serialization.core)
    // Lets Coil load album covers from the server over the same HTTP client as the Jellyfin SDK.
    runtimeOnly(libs.coil.network.okhttp)

    testImplementation(libs.junit)

    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    // Compose UI tests bring in an older Espresso that crashes on Android 16 and newer.
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    debugImplementation(libs.compose.ui.test.manifest)
}
