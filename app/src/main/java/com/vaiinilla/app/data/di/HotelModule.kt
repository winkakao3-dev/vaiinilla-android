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
    /** Punto de extensión: [HotelBackend.useFake] elige entre el repositorio en memoria y el servidor por la URL de la APK. */
    @Provides
    @Singleton
    fun provideHotelRepository(apiClient: VaiinillaApiClient): HotelRepository =
        if (HotelBackend.useFake(BuildConfig.API_BASE_URL)) FakeHotelRepository() else RemoteHotelRepository(apiClient)
}
