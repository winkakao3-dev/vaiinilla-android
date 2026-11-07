package com.vaiinilla.app.data.di

import com.vaiinilla.app.BuildConfig
import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.data.hotel.FakeHotelRepository
import com.vaiinilla.app.data.hotel.HotelBackend
import com.vaiinilla.app.data.hotel.RemoteHotelRepository
import com.vaiinilla.app.domain.hotel.HotelRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HotelModule {
    /** El servidor real, salvo una demo explícita fuera de producción ([HotelBackend.forceFake]). */
    @Provides
    @Singleton
    fun provideHotelRepository(apiClient: VaiinillaApiClient): HotelRepository =
        if (HotelBackend.useFake(BuildConfig.IS_PRODUCTION)) FakeHotelRepository() else RemoteHotelRepository(apiClient)
}
