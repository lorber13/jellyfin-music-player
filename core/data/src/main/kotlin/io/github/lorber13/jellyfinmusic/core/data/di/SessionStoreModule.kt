package io.github.lorber13.jellyfinmusic.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.FileStorage
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import androidx.datastore.tink.AeadSerializer
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplate
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.PredefinedAeadParameters
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.lorber13.jellyfinmusic.core.data.session.StoredSession
import io.github.lorber13.jellyfinmusic.core.data.session.StoredSessionSerializer
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.SupervisorJob

/**
 * The encrypted file that holds the signed-in session. Setting it up touches the Android
 * Keystore, so inject it as `dagger.Lazy` and get it on the IO dispatcher, not the main thread.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object SessionStoreModule {
    private const val SESSION_FILE_NAME = "session.json"
    private const val KEYSET_NAME = "session_keyset"
    private const val KEYSET_PREFERENCES_FILE = "session_keyset_prefs"

    // The Keystore key that encrypts the Tink keyset. It never leaves the device's secure hardware.
    private const val MASTER_KEY_URI = "android-keystore://session_master_key"

    @Provides
    @Singleton
    fun provideSessionDataStore(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): DataStore<StoredSession?> {
        val serializer = AeadSerializer(
            aead = sessionAead(context),
            wrappedSerializer = StoredSessionSerializer,
            // Ties the encrypted data to this file, so it can't be swapped with another file encrypted with the same key.
            associatedData = SESSION_FILE_NAME.encodeToByteArray()
        )
        val storage = FileStorage(serializer) { context.dataStoreFile(SESSION_FILE_NAME) }
        return DataStore.Builder(storage, SupervisorJob() + ioDispatcher)
            // A file that can't be decrypted (e.g. the Keystore key is gone) or parsed means the
            // user signs in again, instead of the app failing to start.
            .setCorruptionHandler(ReplaceFileCorruptionHandler { null })
            .build()
    }

    private fun sessionAead(context: Context): Aead {
        AeadConfig.register()
        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, KEYSET_PREFERENCES_FILE)
            .withKeyTemplate(KeyTemplate.createFrom(PredefinedAeadParameters.AES256_GCM))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
        return keysetHandle.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }
}
