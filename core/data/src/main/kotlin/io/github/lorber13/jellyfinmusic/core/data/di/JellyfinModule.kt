package io.github.lorber13.jellyfinmusic.core.data.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.createJellyfin
import org.jellyfin.sdk.model.ClientInfo
import org.jellyfin.sdk.model.ServerVersion

@Module
@InstallIn(SingletonComponent::class)
internal object JellyfinModule {
    // The product supports Jellyfin 10.9 and newer. The SDK itself targets newer servers and
    // would otherwise rank 10.9 to 11.x as unsupported.
    private val minimumSupportedServerVersion = ServerVersion(major = 10, minor = 9, patch = 0)

    @Provides
    @Singleton
    fun provideJellyfin(@ApplicationContext context: Context): Jellyfin = createJellyfin {
        this.context = context
        clientInfo = ClientInfo(name = "Jellyfin Music Player", version = appVersionName(context))
        minimumServerVersion = minimumSupportedServerVersion
    }

    // Library modules have no BuildConfig with the app's version, so it is read from the installed package.
    private fun appVersionName(context: Context): String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
}
