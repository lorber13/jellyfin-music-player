package io.github.lorber13.jellyfinmusic.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.lorber13.jellyfinmusic.core.data.server.JellyfinServerRepository
import io.github.lorber13.jellyfinmusic.core.data.server.ServerRepository

/** Hilt bindings for the data layer's repositories. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {
    @Binds
    abstract fun bindServerRepository(repository: JellyfinServerRepository): ServerRepository
}
