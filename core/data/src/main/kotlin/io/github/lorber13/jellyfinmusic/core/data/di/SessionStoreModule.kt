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
import com.google.crypto.tink.integration.android.AndroidKeystore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.lorber13.jellyfinmusic.core.data.session.StoredSession
import io.github.lorber13.jellyfinmusic.core.data.session.StoredSessionSerializer
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.ProviderException
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
    private const val MASTER_KEY_ALIAS = "session_master_key"
    private const val MASTER_KEY_URI = "android-keystore://$MASTER_KEY_ALIAS"

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
        // Tink passes on the Keystore's ProviderException as it is, next to the exceptions it declares.
        val keysetManager = try {
            sessionKeysetManager(context)
        } catch (_: GeneralSecurityException) {
            sessionKeysetManagerAfterReset(context)
        } catch (_: IOException) {
            sessionKeysetManagerAfterReset(context)
        } catch (_: ProviderException) {
            sessionKeysetManagerAfterReset(context)
        }
        return keysetManager.keysetHandle.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }

    private fun sessionKeysetManager(context: Context): AndroidKeysetManager = AndroidKeysetManager.Builder()
        .withSharedPref(context, KEYSET_NAME, KEYSET_PREFERENCES_FILE)
        .withKeyTemplate(KeyTemplate.createFrom(PredefinedAeadParameters.AES256_GCM))
        .withMasterKeyUri(MASTER_KEY_URI)
        .build()

    /**
     * Tink's documentation calls a keyset it can't read an irrecoverable error and says to delete
     * it and the data encrypted with it, here only the session. The master key goes too: Tink
     * won't replace a key that exists but fails, so without this the user would sign in again on
     * every launch. If the new key fails as well, the error reaches [SessionStore], which treats
     * it as signed out.
     */
    private fun sessionKeysetManagerAfterReset(context: Context): AndroidKeysetManager {
        // The file holds only this keyset.
        context.deleteSharedPreferences(KEYSET_PREFERENCES_FILE)
        context.dataStoreFile(SESSION_FILE_NAME).delete()
        AndroidKeystore.deleteKey(MASTER_KEY_ALIAS)
        return sessionKeysetManager(context)
    }
}
