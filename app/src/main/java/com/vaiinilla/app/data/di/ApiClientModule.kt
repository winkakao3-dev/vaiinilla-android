package com.vaiinilla.app.data.di

import com.vaiinilla.app.core.network.HttpVaiinillaApiClient
import com.vaiinilla.app.core.network.VaiinillaApiClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Aparte del resto para que el arnés jvm-roles pueda envolver el cliente (red lenta, fallas) sin copiar VaiinillaModule. */
@Module
@InstallIn(SingletonComponent::class)
object ApiClientModule {
    @Provides
    @Singleton
    fun provideApiClient(client: HttpVaiinillaApiClient): VaiinillaApiClient = client
}
