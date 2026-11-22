package io.github.lorber13.jellyfinmusic.core.data.library

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.lorber13.jellyfinmusic.core.data.FakePreferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryPreferencesRepositoryTest {
    private val dataStore = FakePreferencesDataStore()
    private val repository = DataStoreLibraryPreferencesRepository(dataStore)

    @Test
    fun albumSort_withNothingStored_isTheDefault() = runTest {
        assertEquals(AlbumSort.DEFAULT, repository.albumSort.first())
    }

    @Test
    fun setAlbumSort_isReadBack() = runTest {
        val sort = AlbumSort(AlbumSortField.YEAR, SortDirection.ASCENDING)

        repository.setAlbumSort(sort)

        assertEquals(sort, repository.albumSort.first())
    }

    @Test
    fun albumSort_withUnknownValues_isTheDefault() = runTest {
        dataStore.value = preferencesOf(
            stringPreferencesKey("album_sort_field") to "PLAY_COUNT",
            stringPreferencesKey("album_sort_direction") to "ASCENDING"
        )

        assertEquals(AlbumSort.DEFAULT, repository.albumSort.first())
    }

    @Test
    fun albumSort_whenTheFileFails_isTheDefault() = runTest {
        dataStore.failure = IOException("Disk error")

        assertEquals(AlbumSort.DEFAULT, repository.albumSort.first())
    }

    @Test
    fun setAlbumSort_whenTheFileFails_doesNotThrow() = runTest {
        dataStore.failure = IOException("Disk full")

        repository.setAlbumSort(AlbumSort(AlbumSortField.YEAR, SortDirection.DESCENDING))

        assertEquals(emptyPreferences(), dataStore.value)
    }
}
