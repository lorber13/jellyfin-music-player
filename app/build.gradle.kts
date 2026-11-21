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
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.material3.adaptive.navigation.suite)
    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.kotlinx.serialization.core)

    testImplementation(libs.junit)

    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    // Compose UI tests bring in an older Espresso that crashes on Android 16 and newer.
    androidTestImplementation(libs.androidx.test.espresso.core)
    debugImplementation(libs.compose.ui.test.manifest)
}
