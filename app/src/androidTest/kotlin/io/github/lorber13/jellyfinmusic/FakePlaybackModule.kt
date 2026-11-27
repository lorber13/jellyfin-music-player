package io.github.lorber13.jellyfinmusic

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.github.lorber13.jellyfinmusic.core.playback.MusicPlayer
import io.github.lorber13.jellyfinmusic.core.playback.PlaybackPosition
import io.github.lorber13.jellyfinmusic.core.playback.PlayerState
import io.github.lorber13.jellyfinmusic.core.playback.QueueItem
import io.github.lorber13.jellyfinmusic.core.playback.di.PlaybackModule
import javax.inject.Singleton
import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Replaces the music player in every instrumented test, so screens show the player controls
 * without playing anything. Tests inject [FakeMusicPlayer] to say what is playing.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [PlaybackModule::class])
object FakePlaybackModule {
    @Provides
    @Singleton
    fun provideFakeMusicPlayer(): FakeMusicPlayer = FakeMusicPlayer()

    @Provides
    fun provideMusicPlayer(player: FakeMusicPlayer): MusicPlayer = player
}

/** Plays nothing. Nothing is queued until a test sets [state]. */
class FakeMusicPlayer : MusicPlayer {
    override val state = MutableStateFlow<PlayerState?>(null)
    override val positions: Flow<PlaybackPosition> = emptyFlow()
    override val errorMessages: Flow<String> = emptyFlow()

    override fun play(queue: List<QueueItem>, startIndex: Int) = Unit

    override fun playOrPause() = Unit

    override fun skipToPrevious() = Unit

    override fun skipToNext() = Unit

    override fun seekTo(position: Duration) = Unit
}
