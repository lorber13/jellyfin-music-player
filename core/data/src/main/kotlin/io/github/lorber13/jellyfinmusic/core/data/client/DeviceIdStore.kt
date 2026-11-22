package io.github.lorber13.jellyfinmusic.core.data.client

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

private val deviceIdKey = stringPreferencesKey("device_id")

/**
 * The random id the app gives Jellyfin servers as its device id. It is created on first use and
 * kept in the preferences file until the app is uninstalled or its data is cleared, so it
 * identifies this installation of the app, not the phone. Android's guide to identifiers
 * recommends this over ANDROID_ID, which survives reinstalls.
 */
internal class DeviceIdStore @Inject constructor(private val dataStore: DataStore<Preferences>) {
    suspend fun deviceId(): String {
        val newId = UUID.randomUUID().toString()
        return try {
            // One transaction, so two callers can't each store a different id.
            val preferences = dataStore.edit { preferences ->
                if (preferences[deviceIdKey] == null) preferences[deviceIdKey] = newId
            }
            preferences[deviceIdKey] ?: newId
        } catch (_: IOException) {
            // The id then lasts until the app closes; the next sign-in shows up on the server as a new device.
            newId
        }
    }
}
