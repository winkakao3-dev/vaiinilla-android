package com.vaiinilla.app.data.di

import android.content.Context
import com.vaiinilla.app.BuildConfig
import com.vaiinilla.app.core.config.AppEnvironment
import com.vaiinilla.app.core.config.DataSourceMode
import com.vaiinilla.app.core.network.HttpVaiinillaApiClient
import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.core.security.AndroidKeyStoreSessionStore
import com.vaiinilla.app.core.security.PickupTokenStore
import com.vaiinilla.app.core.security.SecureSessionStore
import com.vaiinilla.app.core.security.SharedPreferencesPickupTokenStore
import com.vaiinilla.app.data.catalog.FixtureCatalogRepository
import com.vaiinilla.app.data.catalog.RemoteCatalogRepository
import com.vaiinilla.app.data.fixture.ContractFixtureParser
import com.vaiinilla.app.data.fixture.FixtureSource
import com.vaiinilla.app.data.operational.NoOpCashSessionRepository
import com.vaiinilla.app.data.operational.NoOpDeviceHeartbeatRepository
import com.vaiinilla.app.data.operational.RemoteCashSessionRepository
import com.vaiinilla.app.data.operational.RemoteDeviceHeartbeatRepository
import com.vaiinilla.app.data.order.FixtureOrderRepository
import com.vaiinilla.app.data.order.OrderContractJson
import com.vaiinilla.app.data.order.RemoteOrderRepository
import com.vaiinilla.app.domain.repository.CashSessionRepository
import com.vaiinilla.app.domain.repository.CatalogRepository
import com.vaiinilla.app.domain.repository.DeviceHeartbeatRepository
import com.vaiinilla.app.domain.repository.OrderRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object VaiinillaModule {
    @Provides
    @Singleton
    fun provideEnvironment(): AppEnvironment = AppEnvironment(
        dataSourceMode = DataSourceMode.from(BuildConfig.DATA_SOURCE_MODE),
        apiBaseUrl = BuildConfig.API_BASE_URL,
    )

    @Provides
    @Singleton
    fun provideSecureSessionStore(
        @ApplicationContext context: Context,
    ): SecureSessionStore {
        val store = AndroidKeyStoreSessionStore(context)
        val bootstrap = BuildConfig.BOOTSTRAP_ACCESS_TOKEN.trim()
        if (bootstrap.isNotEmpty()) {
            val current = store.readAccessToken()
            if (current != bootstrap) {
                store.saveAccessToken(bootstrap)
            }
        }
        return store
    }

    @Provides
    @Singleton
    fun providePickupTokenStore(
        store: SharedPreferencesPickupTokenStore,
    ): PickupTokenStore = store

    @Provides
    @Singleton
    fun provideApiClient(client: HttpVaiinillaApiClient): VaiinillaApiClient = client

    @Provides
    @Singleton
    fun provideCatalogRepository(
        environment: AppEnvironment,
        fixtureSource: FixtureSource,
        parser: ContractFixtureParser,
        apiClient: VaiinillaApiClient,
    ): CatalogRepository = when (environment.dataSourceMode) {
        DataSourceMode.MOCK -> FixtureCatalogRepository(fixtureSource, parser)
        DataSourceMode.REMOTE -> RemoteCatalogRepository(apiClient, parser)
    }

    @Provides
    @Singleton
    fun provideOrderRepository(
        environment: AppEnvironment,
        fixtureSource: FixtureSource,
        parser: ContractFixtureParser,
        apiClient: VaiinillaApiClient,
        orderContractJson: OrderContractJson,
        pickupTokenStore: PickupTokenStore,
    ): OrderRepository = when (environment.dataSourceMode) {
        DataSourceMode.MOCK -> FixtureOrderRepository(fixtureSource, parser)
        DataSourceMode.REMOTE -> RemoteOrderRepository(apiClient, orderContractJson, pickupTokenStore)
    }

    @Provides
    @Singleton
    fun provideDeviceHeartbeatRepository(
        environment: AppEnvironment,
        apiClient: VaiinillaApiClient,
    ): DeviceHeartbeatRepository = when (environment.dataSourceMode) {
        DataSourceMode.MOCK -> NoOpDeviceHeartbeatRepository()
        DataSourceMode.REMOTE -> RemoteDeviceHeartbeatRepository(apiClient)
    }

    @Provides
    @Singleton
    fun provideCashSessionRepository(
        environment: AppEnvironment,
        apiClient: VaiinillaApiClient,
        orderContractJson: OrderContractJson,
    ): CashSessionRepository = when (environment.dataSourceMode) {
        DataSourceMode.MOCK -> NoOpCashSessionRepository()
        DataSourceMode.REMOTE -> RemoteCashSessionRepository(apiClient, orderContractJson)
    }
}
