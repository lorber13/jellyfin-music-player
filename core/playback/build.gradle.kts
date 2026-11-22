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
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.session)
    // The session callback looks up streams with the data layer's coroutines and returns a future.
    implementation(libs.kotlinx.coroutines.guava)
    // MainActivity's lifecycle tells MediaControllerMusicPlayer when to let go of the service.
    implementation(libs.androidx.lifecycle.common)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
