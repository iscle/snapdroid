package de.badaix.snapcast.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.badaix.snapcast.data.repository.DeviceIdRepositoryImpl
import de.badaix.snapcast.data.repository.PlayerRepositoryImpl
import de.badaix.snapcast.data.repository.SettingsRepositoryImpl
import de.badaix.snapcast.domain.repository.DeviceIdRepository
import de.badaix.snapcast.domain.repository.PlayerRepository
import de.badaix.snapcast.domain.repository.SettingsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PlayerModule {

    @Binds
    @Singleton
    abstract fun bindPlayerRepository(
        playerRepositoryImpl: PlayerRepositoryImpl
    ): PlayerRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindDeviceIdRepository(
        deviceIdRepositoryImpl: DeviceIdRepositoryImpl
    ): DeviceIdRepository
}

