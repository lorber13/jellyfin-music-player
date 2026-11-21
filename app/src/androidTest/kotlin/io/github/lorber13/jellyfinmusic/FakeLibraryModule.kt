package io.github.lorber13.jellyfinmusic

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.github.lorber13.jellyfinmusic.core.data.di.LibraryModule
import io.github.lorber13.jellyfinmusic.core.data.library.Album
import io.github.lorber13.jellyfinmusic.core.data.library.AlbumRepository
import io.github.lorber13.jellyfinmusic.core.data.library.AlbumSort
import io.github.lorber13.jellyfinmusic.core.data.library.AlbumsResult
import io.github.lorber13.jellyfinmusic.core.data.library.LibraryPreferencesRepository
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Replaces the library's data in every instrumented test, so screens show without a server. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [LibraryModule::class])
object FakeLibraryModule {
    private val albums = listOf(
        Album(id = "1", name = "Morning Light", artistName = "Aurora Fields", coverImageUrl = null),
        Album(id = "2", name = "Steam & Rust", artistName = "The Copper Kettles", coverImageUrl = null)
    )

    @Provides
    fun provideAlbumRepository(): AlbumRepository = object : AlbumRepository {
        override suspend fun getAlbums(sort: AlbumSort): AlbumsResult = AlbumsResult.Success(albums)
    }

    @Provides
    @Singleton
    fun provideLibraryPreferencesRepository(): LibraryPreferencesRepository = object : LibraryPreferencesRepository {
        private val storedSort = MutableStateFlow(AlbumSort.DEFAULT)
        override val albumSort: Flow<AlbumSort> = storedSort

        override suspend fun setAlbumSort(sort: AlbumSort) {
            storedSort.value = sort
        }
    }
}
