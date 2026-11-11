plugins {
    id("jellyfinmusic.android.library")
    id("jellyfinmusic.android.compose")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.github.lorber13.jellyfinmusic.feature.albums"
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:data"))

    implementation(libs.navigation3.runtime)
    implementation(libs.kotlinx.serialization.core)
}
