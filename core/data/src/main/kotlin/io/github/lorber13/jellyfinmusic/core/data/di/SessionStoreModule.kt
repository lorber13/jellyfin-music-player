package io.github.lorber13.jellyfinmusic.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.FileStorage
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import androidx.datastore.tink.AeadSerializer
import com.google.crypto.tink.Aead
import com.google.crypto.tink.integration.android.AndroidKeystore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.lorber13.jellyfinmusic.core.data.session.StoredSession
import io.github.lorber13.jellyfinmusic.core.data.session.StoredSessionSerializer
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

    // The Android Keystore key that encrypts the session file. It never leaves the device's
    // secure hardware.
    private const val SESSION_KEY_ALIAS = "session_key"

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

    /**
     * Encrypts with the Keystore key directly, as Tink's documentation recommends over a Tink
     * keyset that a Keystore key encrypts, and as the deprecation of Jetpack Security points to.
     */
    private fun sessionAead(context: Context): Aead =
        // Tink passes on the Keystore's ProviderException as it is, next to the exceptions it declares.
        try {
            sessionKeyAead()
        } catch (_: GeneralSecurityException) {
            sessionAeadAfterReset(context)
        } catch (_: ProviderException) {
            sessionAeadAfterReset(context)
        }

    /** The session key, created the first time. */
    private fun sessionKeyAead(): Aead {
        if (!AndroidKeystore.hasKey(SESSION_KEY_ALIAS)) AndroidKeystore.generateNewAes256GcmKey(SESSION_KEY_ALIAS)
        return AndroidKeystore.getAead(SESSION_KEY_ALIAS)
    }

    /**
     * A key that can't be loaded can't be repaired, and what it encrypted can't be read any more,
     * so both go and the user signs in again. Without this, the user would sign in again on every
     * launch. If the new key fails as well, the error reaches [SessionStore], which treats it as
     * signed out.
     */
    private fun sessionAeadAfterReset(context: Context): Aead {
        context.dataStoreFile(SESSION_FILE_NAME).delete()
        AndroidKeystore.deleteKey(SESSION_KEY_ALIAS)
        return sessionKeyAead()
    }
}
