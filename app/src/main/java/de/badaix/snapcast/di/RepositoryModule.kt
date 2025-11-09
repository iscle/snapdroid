package de.badaix.snapcast.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.badaix.snapcast.data.repository.MdnsRepositoryImpl
import de.badaix.snapcast.data.repository.SnapcastRepositoryImpl
import de.badaix.snapcast.domain.repository.MdnsRepository
import de.badaix.snapcast.domain.repository.SnapcastRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSnapcastRepository(
        snapcastRepositoryImpl: SnapcastRepositoryImpl
    ): SnapcastRepository

    @Binds
    @Singleton
    abstract fun bindMdnsRepository(
        mdnsRepositoryImpl: MdnsRepositoryImpl
    ): MdnsRepository
}

