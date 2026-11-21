package io.github.lorber13.jellyfinmusic.core.data.library

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val albumSortFieldKey = stringPreferencesKey("album_sort_field")
private val albumSortDirectionKey = stringPreferencesKey("album_sort_direction")

/**
 * Keeps the library preferences in the app's preferences file. A file that can't be read or
 * written only costs the user their sort order, so failures fall back to the defaults. Public,
 * with an internal constructor, because the public `LibraryModule` binds it.
 */
class DataStoreLibraryPreferencesRepository @Inject internal constructor(
    private val dataStore: DataStore<Preferences>
) : LibraryPreferencesRepository {
    override val albumSort: Flow<AlbumSort> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map(::albumSortOf)

    override suspend fun setAlbumSort(sort: AlbumSort) {
        try {
            dataStore.edit { preferences ->
                // Stored by name, so reordering the enums doesn't change saved preferences.
                preferences[albumSortFieldKey] = sort.field.name
                preferences[albumSortDirectionKey] = sort.direction.name
            }
        } catch (_: IOException) {
            // The sort still applies until the app closes.
        }
    }
}

/** The stored sort, or the default when nothing or an unknown value (e.g. from a newer app version) is stored. */
private fun albumSortOf(preferences: Preferences): AlbumSort {
    val field = AlbumSortField.entries.firstOrNull { it.name == preferences[albumSortFieldKey] }
    val direction = SortDirection.entries.firstOrNull { it.name == preferences[albumSortDirectionKey] }
    if (field == null || direction == null) return AlbumSort.DEFAULT
    return AlbumSort(field, direction)
}
