package io.github.lorber13.jellyfinmusic.core.data.session

import androidx.datastore.core.DataStore
import dagger.Lazy
import java.io.IOException
import java.security.GeneralSecurityException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionStoreTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val session = StoredSession(
        serverAddress = "http://localhost:8096",
        serverName = "Test server",
        userId = "1",
        userName = "test",
        accessToken = "token"
    )
    private val dataStore = FakeDataStore()

    @Test
    fun read_returnsTheStoredSession() = runTest(dispatcher) {
        dataStore.value = session

        assertEquals(session, store().read())
    }

    @Test
    fun read_withNothingStored_returnsNull() = runTest(dispatcher) {
        assertNull(store().read())
    }

    @Test
    fun read_whenTheFileFails_returnsNull() = runTest(dispatcher) {
        dataStore.value = session
        dataStore.failure = IOException("Disk error")

        assertNull(store().read())
    }

    @Test
    fun read_whenTheKeystoreFails_returnsNull() = runTest(dispatcher) {
        val store = SessionStore(Lazy { throw GeneralSecurityException("Key unusable") }, dispatcher)

        assertNull(store.read())
    }

    @Test
    fun save_replacesTheStoredSession() = runTest(dispatcher) {
        dataStore.value = session.copy(userId = "2")

        store().save(session)

        assertEquals(session, dataStore.value)
    }

    @Test
    fun save_whenTheFileFails_doesNotThrow() = runTest(dispatcher) {
        dataStore.failure = IOException("Disk full")

        store().save(session)

        assertNull(dataStore.value)
    }

    private fun store() = SessionStore(Lazy { dataStore }, dispatcher)

    private class FakeDataStore : DataStore<StoredSession?> {
        var value: StoredSession? = null
        var failure: Exception? = null

        override val data: Flow<StoredSession?> = flow {
            failure?.let { throw it }
            emit(value)
        }

        override suspend fun updateData(transform: suspend (t: StoredSession?) -> StoredSession?): StoredSession? {
            failure?.let { throw it }
            value = transform(value)
            return value
        }
    }
}
