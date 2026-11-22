package io.github.lorber13.jellyfinmusic.core.data.library

import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Test

class AlbumRequestTest {
    @Test
    fun albumRequest_asksForTheOneAlbumWithoutUserData() {
        val albumId = UUID.fromString("5d1a7f2c-3b6e-4c8a-9f0d-2e4b6a8c0d1f")

        val request = albumRequestOf(albumId, userId = UUID.fromString("0f1e2d3c-4b5a-6978-8a9b-acbdcedfe0f1"))

        assertEquals(listOf(albumId), request.ids)
        // Jellyfin 10.9 sends user data in a form the SDK can't read.
        assertEquals(false, request.enableUserData)
    }
}
