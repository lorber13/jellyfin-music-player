package io.github.lorber13.jellyfinmusic.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.lorber13.jellyfinmusic.core.data.library.AlbumRepository
import io.github.lorber13.jellyfinmusic.core.data.library.DataStoreLibraryPreferencesRepository
import io.github.lorber13.jellyfinmusic.core.data.library.JellyfinAlbumRepository
import io.github.lorber13.jellyfinmusic.core.data.library.LibraryPreferencesRepository

/**
 * Hilt bindings for the library's repositories. Unlike [DataModule] it is public, so the app's
 * instrumented tests can replace it with fakes (`@TestInstallIn`) and show the screens without a
 * server.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class LibraryModule {
    @Binds
    abstract fun bindAlbumRepository(repository: JellyfinAlbumRepository): AlbumRepository

    @Binds
    abstract fun bindLibraryPreferencesRepository(
        repository: DataStoreLibraryPreferencesRepository
    ): LibraryPreferencesRepository
}
