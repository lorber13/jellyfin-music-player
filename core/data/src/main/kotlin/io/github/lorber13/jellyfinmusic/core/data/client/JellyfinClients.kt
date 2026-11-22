package io.github.lorber13.jellyfinmusic.core.data.client

import android.content.Context
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.lorber13.jellyfinmusic.core.data.server.minimumSupportedServerVersion
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.createJellyfin
import org.jellyfin.sdk.model.ClientInfo
import org.jellyfin.sdk.model.DeviceInfo

/**
 * Creates the Jellyfin SDK and its API clients. The SDK takes the device when it is created and
 * would otherwise use ANDROID_ID, so it is created on first use, once [DeviceIdStore] has read
 * the app's own device id from disk.
 */
@Singleton
internal class JellyfinClients @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val deviceIdStore: DeviceIdStore
) {
    private val creation = Mutex()
    private var jellyfin: Jellyfin? = null

    /** The SDK, created on the first call. */
    suspend fun jellyfin(): Jellyfin = creation.withLock {
        jellyfin ?: createSdk().also { jellyfin = it }
    }

    /** A client for the server at [baseUrl], signed in when an [accessToken] is given. */
    suspend fun createClient(baseUrl: String, accessToken: String? = null): ApiClient =
        jellyfin().createApi(baseUrl = baseUrl, accessToken = accessToken)

    private suspend fun createSdk(): Jellyfin {
        val app = ClientInfo(name = "Jellyfin Music Player", version = appVersionName(context))
        val device = DeviceInfo(id = deviceIdStore.deviceId(), name = deviceName(context))
        return createJellyfin {
            this.context = this@JellyfinClients.context
            clientInfo = app
            deviceInfo = device
            minimumServerVersion = minimumSupportedServerVersion
        }
    }
}

/**
 * The name servers show for the phone: the one the user gave it in Settings, else its maker and
 * model. The SDK names devices the same way, but only together with ANDROID_ID.
 */
private fun deviceName(context: Context): String {
    val nameFromSettings = Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
    if (!nameFromSettings.isNullOrBlank()) return nameFromSettings
    val manufacturer = Build.MANUFACTURER
    val model = Build.MODEL
    return if (manufacturer.isBlank() || model.startsWith(manufacturer)) model else "$manufacturer $model"
}

// Library modules have no BuildConfig with the app's version, so it is read from the installed package.
private fun appVersionName(context: Context): String =
    context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
