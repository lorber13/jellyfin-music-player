package io.github.lorber13.jellyfinmusic.core.data.session

import androidx.datastore.core.CorruptionException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class StoredSessionSerializerTest {
    private val session = StoredSession(
        serverAddress = "http://localhost:8096",
        serverName = "Test server",
        userId = "1",
        userName = "test",
        accessToken = "secret-token"
    )

    @Test
    fun aWrittenSession_readsBackTheSame() = runTest {
        assertEquals(session, readBack(session))
    }

    @Test
    fun noSession_readsBackAsNoSession() = runTest {
        assertNull(readBack(null))
    }

    @Test
    fun anEmptyFile_isNoSession() = runTest {
        assertNull(StoredSessionSerializer.readFrom(ByteArrayInputStream(byteArrayOf())))
    }

    @Test(expected = CorruptionException::class)
    fun unreadableContent_isCorruption() = runTest {
        StoredSessionSerializer.readFrom(ByteArrayInputStream("not json".encodeToByteArray()))
    }

    @Test(expected = CorruptionException::class)
    fun aSessionWithMissingFields_isCorruption() = runTest {
        StoredSessionSerializer.readFrom(ByteArrayInputStream("""{"userId":"1"}""".encodeToByteArray()))
    }

    @Test
    fun toString_leavesOutTheAccessToken() {
        assertFalse(session.toString().contains("secret-token"))
    }

    private suspend fun readBack(session: StoredSession?): StoredSession? {
        val output = ByteArrayOutputStream()
        StoredSessionSerializer.writeTo(session, output)
        return StoredSessionSerializer.readFrom(ByteArrayInputStream(output.toByteArray()))
    }
}
