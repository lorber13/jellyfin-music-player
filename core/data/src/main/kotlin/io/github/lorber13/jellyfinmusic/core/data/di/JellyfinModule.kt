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
import org.jellyfin.sdk.createJellyfin
import org.jellyfin.sdk.model.ClientInfo

@Module
@InstallIn(SingletonComponent::class)
internal object JellyfinModule {
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
