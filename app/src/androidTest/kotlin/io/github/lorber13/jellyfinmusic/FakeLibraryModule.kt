package io.github.lorber13.jellyfinmusic

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.github.lorber13.jellyfinmusic.core.data.di.LibraryModule
import io.github.lorber13.jellyfinmusic.core.data.library.Album
import io.github.lorber13.jellyfinmusic.core.data.library.AlbumDetail
import io.github.lorber13.jellyfinmusic.core.data.library.AlbumDetailResult
import io.github.lorber13.jellyfinmusic.core.data.library.AlbumRepository
import io.github.lorber13.jellyfinmusic.core.data.library.AlbumSort
import io.github.lorber13.jellyfinmusic.core.data.library.AlbumsResult
import io.github.lorber13.jellyfinmusic.core.data.library.LibraryPreferencesRepository
import io.github.lorber13.jellyfinmusic.core.data.library.Track
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes
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

        // Every album has the same tracks, named after it so tests can tell which one opened.
        override suspend fun getAlbumDetail(albumId: String): AlbumDetailResult {
            val album = albums.first { it.id == albumId }
            return AlbumDetailResult.Success(
                AlbumDetail(
                    id = album.id,
                    name = album.name,
                    artistName = album.artistName,
                    year = 2021,
                    coverImageUrl = null,
                    tracks = listOf(
                        Track(
                            id = "${album.id}-1",
                            name = "${album.name} Overture",
                            artistNames = listOfNotNull(album.artistName),
                            trackNumber = 1,
                            discNumber = 1,
                            duration = 3.minutes
                        )
                    )
                )
            )
        }
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
