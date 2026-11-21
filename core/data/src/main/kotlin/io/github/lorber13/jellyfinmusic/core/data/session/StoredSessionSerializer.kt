package io.github.lorber13.jellyfinmusic.core.data.session

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import java.io.InputStream
import java.io.OutputStream
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.json.Json

/** Reads and writes the stored session as JSON. Null means nobody is signed in. */
internal object StoredSessionSerializer : Serializer<StoredSession?> {
    private val jsonSerializer = StoredSession.serializer().nullable

    // Lets a newer app version add fields without older files becoming unreadable, and the other way round.
    private val json = Json { ignoreUnknownKeys = true }

    override val defaultValue: StoredSession? = null

    override suspend fun readFrom(input: InputStream): StoredSession? {
        val text = input.readBytes().decodeToString()
        if (text.isEmpty()) return null
        return try {
            json.decodeFromString(jsonSerializer, text)
        } catch (error: IllegalArgumentException) {
            // Also catches SerializationException, which extends it.
            throw CorruptionException("The stored session can't be read", error)
        }
    }

    override suspend fun writeTo(t: StoredSession?, output: OutputStream) {
        output.write(json.encodeToString(jsonSerializer, t).encodeToByteArray())
    }
}
