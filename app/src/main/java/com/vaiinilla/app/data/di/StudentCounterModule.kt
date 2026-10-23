package com.vaiinilla.app.data.di

import android.content.Context
import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.data.counter.AndroidConnectivityWatcher
import com.vaiinilla.app.data.counter.PreferencesOfflineCounterStore
import com.vaiinilla.app.data.counter.RemoteStudentCounterRepository
import com.vaiinilla.app.data.order.OrderContractJson
import com.vaiinilla.app.domain.counter.ConnectivityWatcher
import com.vaiinilla.app.domain.counter.OfflineCounterStore
import com.vaiinilla.app.domain.counter.StudentCounterRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Caja con alumnos sin teléfono y "Para mañana" de Cocina (padres e hijos), aparte de VaiinillaModule. */
@Module
@InstallIn(SingletonComponent::class)
object StudentCounterModule {
    @Provides
    @Singleton
    fun provideStudentCounterRepository(
        apiClient: VaiinillaApiClient,
        orderContractJson: OrderContractJson,
    ): StudentCounterRepository = RemoteStudentCounterRepository(apiClient, orderContractJson)

    @Provides
    @Singleton
    fun provideOfflineCounterStore(
        @ApplicationContext context: Context,
    ): OfflineCounterStore = PreferencesOfflineCounterStore(context)

    @Provides
    @Singleton
    fun provideConnectivityWatcher(
        @ApplicationContext context: Context,
    ): ConnectivityWatcher = AndroidConnectivityWatcher(context)
}
