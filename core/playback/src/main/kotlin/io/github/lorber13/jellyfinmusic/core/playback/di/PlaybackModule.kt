package io.github.lorber13.jellyfinmusic.core.playback.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.lorber13.jellyfinmusic.core.playback.MediaControllerMusicPlayer
import io.github.lorber13.jellyfinmusic.core.playback.MusicPlayer

/**
 * Hilt binding for the music player. Public so the app's instrumented tests can replace it with a
 * fake (`@TestInstallIn`) and show the player controls without playing anything.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PlaybackModule {
    @Binds
    abstract fun bindMusicPlayer(player: MediaControllerMusicPlayer): MusicPlayer
}
