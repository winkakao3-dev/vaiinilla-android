package com.vaiinilla.app.data.di

import android.content.Context
import com.vaiinilla.app.BuildConfig
import com.vaiinilla.app.core.auth.ActiveSessionRefresher
import com.vaiinilla.app.core.auth.VaiinillaJwtRefreshCoordinator
import com.vaiinilla.app.core.config.AppEnvironment
import com.vaiinilla.app.core.config.DataSourceMode
import com.vaiinilla.app.core.config.EffectiveDataSourceResolver
import com.vaiinilla.app.core.network.HttpVaiinillaApiClient
import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.core.security.AndroidKeyStoreSessionStore
import com.vaiinilla.app.core.security.PickupTokenStore
import com.vaiinilla.app.core.security.SecureSessionStore
import com.vaiinilla.app.core.security.SharedPreferencesPickupTokenStore
import com.vaiinilla.app.data.SwitchingCashSessionRepository
import com.vaiinilla.app.data.SwitchingCatalogRepository
import com.vaiinilla.app.data.SwitchingDeviceHeartbeatRepository
import com.vaiinilla.app.data.SwitchingOrderRepository
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
    fun provideEnvironment(): AppEnvironment =
        AppEnvironment(
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
    fun providePickupTokenStore(store: SharedPreferencesPickupTokenStore): PickupTokenStore = store

    @Provides
    @Singleton
    fun provideActiveSessionRefresher(coordinator: VaiinillaJwtRefreshCoordinator): ActiveSessionRefresher = coordinator

    @Provides
    @Singleton
    fun provideApiClient(client: HttpVaiinillaApiClient): VaiinillaApiClient = client

    @Provides
    @Singleton
    fun provideFixtureCatalogRepository(
        fixtureSource: FixtureSource,
        parser: ContractFixtureParser,
    ): FixtureCatalogRepository = FixtureCatalogRepository(fixtureSource, parser)

    @Provides
    @Singleton
    fun provideRemoteCatalogRepository(
        apiClient: VaiinillaApiClient,
        parser: ContractFixtureParser,
    ): RemoteCatalogRepository = RemoteCatalogRepository(apiClient, parser)

    @Provides
    @Singleton
    fun provideCatalogRepository(
        resolver: EffectiveDataSourceResolver,
        fixture: FixtureCatalogRepository,
        remote: RemoteCatalogRepository,
    ): CatalogRepository = SwitchingCatalogRepository(resolver, fixture, remote)

    @Provides
    @Singleton
    fun provideFixtureOrderRepository(
        fixtureSource: FixtureSource,
        parser: ContractFixtureParser,
    ): FixtureOrderRepository = FixtureOrderRepository(fixtureSource, parser)

    @Provides
    @Singleton
    fun provideRemoteOrderRepository(
        apiClient: VaiinillaApiClient,
        orderContractJson: OrderContractJson,
        pickupTokenStore: PickupTokenStore,
    ): RemoteOrderRepository = RemoteOrderRepository(apiClient, orderContractJson, pickupTokenStore)

    @Provides
    @Singleton
    fun provideOrderRepository(
        resolver: EffectiveDataSourceResolver,
        fixture: FixtureOrderRepository,
        remote: RemoteOrderRepository,
    ): OrderRepository = SwitchingOrderRepository(resolver, fixture, remote)

    @Provides
    @Singleton
    fun provideDeviceHeartbeatRepository(
        resolver: EffectiveDataSourceResolver,
        apiClient: VaiinillaApiClient,
    ): DeviceHeartbeatRepository =
        SwitchingDeviceHeartbeatRepository(
            resolver = resolver,
            noop = NoOpDeviceHeartbeatRepository(),
            remote = RemoteDeviceHeartbeatRepository(apiClient),
        )

    @Provides
    @Singleton
    fun provideCashSessionRepository(
        resolver: EffectiveDataSourceResolver,
        apiClient: VaiinillaApiClient,
        orderContractJson: OrderContractJson,
    ): CashSessionRepository =
        SwitchingCashSessionRepository(
            resolver = resolver,
            noop = NoOpCashSessionRepository(),
            remote = RemoteCashSessionRepository(apiClient, orderContractJson),
        )
}
