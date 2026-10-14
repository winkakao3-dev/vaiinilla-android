package com.vaiinilla.app.data.di

import android.content.Context
import com.vaiinilla.app.data.location.FusedDeviceLocationProvider
import com.vaiinilla.app.domain.location.DeviceLocationProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Sensores del teléfono, aparte para que el arnés de la JVM los sustituya. */
@Module
@InstallIn(SingletonComponent::class)
object DeviceModule {
    @Provides
    @Singleton
    fun provideDeviceLocationProvider(
        @ApplicationContext context: Context,
    ): DeviceLocationProvider = FusedDeviceLocationProvider(context)
}
