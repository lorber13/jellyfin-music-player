package io.github.lorber13.jellyfinmusic.core.playback.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.lorber13.jellyfinmusic.core.playback.MediaControllerMusicPlayer
import io.github.lorber13.jellyfinmusic.core.playback.MusicPlayer

@Module
@InstallIn(SingletonComponent::class)
internal abstract class PlaybackModule {
    @Binds
    abstract fun bindMusicPlayer(player: MediaControllerMusicPlayer): MusicPlayer
}
