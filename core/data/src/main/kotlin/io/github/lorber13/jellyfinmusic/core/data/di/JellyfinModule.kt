package io.github.lorber13.jellyfinmusic.core.data.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.lorber13.jellyfinmusic.core.data.server.minimumSupportedServerVersion
import javax.inject.Singleton
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.android.androidDevice
import org.jellyfin.sdk.createJellyfin
import org.jellyfin.sdk.model.ClientInfo
import org.jellyfin.sdk.model.DeviceInfo

@Module
@InstallIn(SingletonComponent::class)
internal object JellyfinModule {
    @Provides
    @Singleton
    fun provideJellyfin(@ApplicationContext context: Context): Jellyfin = createJellyfin {
        this.context = context
        clientInfo = ClientInfo(name = "Jellyfin Music Player", version = appVersionName(context))
        deviceInfo = appDevice(context)
        minimumServerVersion = minimumSupportedServerVersion
    }

    /**
     * The SDK's device with the app's package added to its id. The SDK uses ANDROID_ID, which
     * apps signed with the same key share, and the server signs a device out of a user's earlier
     * logins when it signs in again: the debug and release builds would sign each other out.
     */
    private fun appDevice(context: Context): DeviceInfo {
        val device = androidDevice(context)
        return device.copy(id = "${device.id}-${context.packageName}")
    }

    // Library modules have no BuildConfig with the app's version, so it is read from the installed package.
    private fun appVersionName(context: Context): String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
}
