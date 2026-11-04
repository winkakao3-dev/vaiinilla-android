package com.vaiinilla.app.data.di

import android.content.Context
import com.vaiinilla.app.core.network.HttpVaiinillaApiClient
import com.vaiinilla.app.data.localhub.LocalHubDiscovery
import com.vaiinilla.app.data.localhub.PreferencesLocalBoardStore
import com.vaiinilla.app.data.localhub.PreferencesLocalKeyStore
import com.vaiinilla.app.ui.operational.LocalHubCoordinator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** La Caja como concentrador del local (fase 2 sin internet): vive lo que vive la app, no la pantalla. */
@Module
@InstallIn(SingletonComponent::class)
object LocalHubModule {
    @Provides
    @Singleton
    fun provideLocalHubCoordinator(
        @ApplicationContext context: Context,
        apiClient: HttpVaiinillaApiClient,
    ): LocalHubCoordinator =
        LocalHubCoordinator(
            apiClient = apiClient,
            keyStore = PreferencesLocalKeyStore(context),
            boardStore = PreferencesLocalBoardStore(context),
            discovery = LocalHubDiscovery(context),
        )
}
