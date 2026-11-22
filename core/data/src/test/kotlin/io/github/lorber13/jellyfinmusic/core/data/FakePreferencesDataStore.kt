package io.github.lorber13.jellyfinmusic.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** A preferences file in memory. Setting [failure] makes every read and write throw it. */
internal class FakePreferencesDataStore : DataStore<Preferences> {
    var value: Preferences = emptyPreferences()
    var failure: Exception? = null

    override val data: Flow<Preferences> = flow {
        failure?.let { throw it }
        emit(value)
    }

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        failure?.let { throw it }
        value = transform(value)
        return value
    }
}
