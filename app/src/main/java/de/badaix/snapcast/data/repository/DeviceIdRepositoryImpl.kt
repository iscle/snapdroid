package de.badaix.snapcast.data.repository

import de.badaix.snapcast.data.datasource.SharedPreferencesDataSource
import de.badaix.snapcast.domain.repository.DeviceIdRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceIdRepositoryImpl @Inject constructor(
    private val prefsDataSource: SharedPreferencesDataSource
) : DeviceIdRepository {

    override suspend fun getDeviceId(): String {
        return prefsDataSource.getDeviceId()
    }
}

