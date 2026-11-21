package io.github.lorber13.jellyfinmusic.core.data.session

import androidx.datastore.core.DataStore
import dagger.Lazy
import io.github.lorber13.jellyfinmusic.core.data.di.IoDispatcher
import java.io.IOException
import java.security.GeneralSecurityException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Keeps the signed-in session in an encrypted file on the device. A file that can't be read or
 * written only costs the user a sign-in, so failures are not passed on.
 */
@Singleton
internal class SessionStore @Inject constructor(
    // Lazy, so the Keystore setup behind it runs on the IO dispatcher instead of the injecting thread.
    private val dataStore: Lazy<DataStore<StoredSession?>>,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    /** The stored session, or null if there is none or it can't be read. */
    suspend fun read(): StoredSession? = withContext(ioDispatcher) {
        try {
            dataStore.get().data.first()
        } catch (_: IOException) {
            null
        } catch (_: GeneralSecurityException) {
            null
        }
    }

    /** Stores [session], replacing the previous one. If that fails, the session lasts until the app closes. */
    suspend fun save(session: StoredSession) {
        withContext(ioDispatcher) {
            try {
                dataStore.get().updateData { session }
            } catch (_: IOException) {
            } catch (_: GeneralSecurityException) {
            }
        }
    }
}
