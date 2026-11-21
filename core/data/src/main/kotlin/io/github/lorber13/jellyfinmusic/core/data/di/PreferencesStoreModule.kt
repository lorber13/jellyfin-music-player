package io.github.lorber13.jellyfinmusic.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.FileStorage
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesFileSerializer
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.SupervisorJob

/**
 * The file that holds the user's preferences, such as the albums sort order. It holds nothing
 * sensitive, so unlike the session it is not encrypted.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object PreferencesStoreModule {
    private const val PREFERENCES_FILE_NAME = "preferences"

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): DataStore<Preferences> {
        val storage = FileStorage(PreferencesFileSerializer) { context.preferencesDataStoreFile(PREFERENCES_FILE_NAME) }
        return DataStore.Builder(storage, SupervisorJob() + ioDispatcher)
            // A damaged file resets the preferences to their defaults instead of failing every read.
            .setCorruptionHandler(ReplaceFileCorruptionHandler { emptyPreferences() })
            .build()
    }
}
