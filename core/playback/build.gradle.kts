plugins {
    id("jellyfinmusic.android.library")
    id("jellyfinmusic.hilt")
}

android {
    namespace = "io.github.lorber13.jellyfinmusic.core.playback"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    implementation(project(":core:data"))

    // Only this module uses Media3; features play music through MusicPlayer.
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    // MainActivity's lifecycle tells MediaControllerMusicPlayer when to let go of the service.
    implementation(libs.androidx.lifecycle.common)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
