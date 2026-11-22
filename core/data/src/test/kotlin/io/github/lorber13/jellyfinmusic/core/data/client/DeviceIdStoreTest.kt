package io.github.lorber13.jellyfinmusic.core.data.client

import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.lorber13.jellyfinmusic.core.data.FakePreferencesDataStore
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DeviceIdStoreTest {
    private val deviceIdKey = stringPreferencesKey("device_id")

    @Test
    fun deviceId_onFirstUse_isARandomUuidThatIsStored() = runTest {
        val dataStore = FakePreferencesDataStore()

        val deviceId = DeviceIdStore(dataStore).deviceId()

        assertEquals(deviceId, UUID.fromString(deviceId).toString())
        assertEquals(deviceId, dataStore.value[deviceIdKey])
    }

    @Test
    fun deviceId_onLaterUse_isTheStoredOne() = runTest {
        val dataStore = FakePreferencesDataStore()
        dataStore.value = preferencesOf(deviceIdKey to "3f1c9a52-7d4e-4b8a-9c21-5e6f0a1b2c3d")

        assertEquals("3f1c9a52-7d4e-4b8a-9c21-5e6f0a1b2c3d", DeviceIdStore(dataStore).deviceId())
    }

    @Test
    fun deviceId_ofAnotherInstallation_isDifferent() = runTest {
        val firstInstallation = DeviceIdStore(FakePreferencesDataStore()).deviceId()
        val secondInstallation = DeviceIdStore(FakePreferencesDataStore()).deviceId()

        assertNotEquals(firstInstallation, secondInstallation)
    }

    @Test
    fun deviceId_whenTheFileFails_isStillARandomUuid() = runTest {
        val dataStore = FakePreferencesDataStore()
        dataStore.failure = IOException("Disk full")

        val deviceId = DeviceIdStore(dataStore).deviceId()

        assertEquals(deviceId, UUID.fromString(deviceId).toString())
    }
}
