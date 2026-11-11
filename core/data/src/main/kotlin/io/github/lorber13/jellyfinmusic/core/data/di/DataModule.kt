package io.github.lorber13.jellyfinmusic.core.data.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt bindings for the data layer. Empty for now: repositories are bound here as the
 * backlog items that need them are implemented.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule
