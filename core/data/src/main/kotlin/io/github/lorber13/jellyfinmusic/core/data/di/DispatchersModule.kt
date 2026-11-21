package io.github.lorber13.jellyfinmusic.core.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** The dispatcher for network and disk work, injected so tests can replace it. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class IoDispatcher

/** A scope for work that outlives any screen, such as restoring the session at startup. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
internal object DispatchersModule {
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(@IoDispatcher ioDispatcher: CoroutineDispatcher): CoroutineScope =
        // One failing job must not cancel the others.
        CoroutineScope(SupervisorJob() + ioDispatcher)
}
