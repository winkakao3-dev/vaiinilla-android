package com.vaiinilla.app.data.di

import android.content.Context
import com.vaiinilla.app.data.offline.PreferencesOfflineStaffOrderStore
import com.vaiinilla.app.domain.counter.ConnectivityWatcher
import com.vaiinilla.app.domain.offline.OfflineStaffOrderStore
import com.vaiinilla.app.domain.repository.OrderRepository
import com.vaiinilla.app.ui.operational.OfflineStaffOrderQueue
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Mesero y Caja sin internet: la cola de pedidos guardados en el teléfono vive lo que vive la app. */
@Module
@InstallIn(SingletonComponent::class)
object OfflineStaffModule {
    @Provides
    @Singleton
    fun provideOfflineStaffOrderStore(
        @ApplicationContext context: Context,
    ): OfflineStaffOrderStore = PreferencesOfflineStaffOrderStore(context)

    @Provides
    @Singleton
    fun provideOfflineStaffOrderQueue(
        repository: OrderRepository,
        store: OfflineStaffOrderStore,
        connectivity: ConnectivityWatcher,
    ): OfflineStaffOrderQueue = OfflineStaffOrderQueue(repository, store, connectivity)
}
