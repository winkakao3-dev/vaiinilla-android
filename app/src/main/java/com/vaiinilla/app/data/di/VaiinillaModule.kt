package com.vaiinilla.app.data.di

import android.content.Context
import com.vaiinilla.app.BuildConfig
import com.vaiinilla.app.core.auth.ActiveSessionRefresher
import com.vaiinilla.app.core.auth.VaiinillaJwtRefreshCoordinator
import com.vaiinilla.app.core.config.AppEnvironment
import com.vaiinilla.app.core.network.HttpVaiinillaApiClient
import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.core.security.AndroidKeyStoreSessionStore
import com.vaiinilla.app.core.security.PickupTokenStore
import com.vaiinilla.app.core.security.SecureSessionStore
import com.vaiinilla.app.core.security.SharedPreferencesPickupTokenStore
import com.vaiinilla.app.data.account.RemoteAccountDeletionRepository
import com.vaiinilla.app.data.auth.ContextoExchanger
import com.vaiinilla.app.data.auth.SesionesContextoExchange
import com.vaiinilla.app.data.auth.student.AccessEmailApi
import com.vaiinilla.app.data.auth.student.FirebaseStudentAuthRepository
import com.vaiinilla.app.data.auth.student.RemoteAccessEmailApi
import com.vaiinilla.app.data.auth.student.RemoteStudentEnrollmentApi
import com.vaiinilla.app.data.auth.student.RemoteStudentEnrollmentRepository
import com.vaiinilla.app.data.auth.student.StudentEnrollmentApi
import com.vaiinilla.app.data.catalog.RemoteCatalogRepository
import com.vaiinilla.app.data.contract.ContractResponseParser
import com.vaiinilla.app.data.discovery.RemoteDiscoveryRepository
import com.vaiinilla.app.data.mode.AuthorizedAccessApi
import com.vaiinilla.app.data.mode.RemoteAuthorizedAccessApi
import com.vaiinilla.app.data.mode.RemoteAuthorizedAccessRepository
import com.vaiinilla.app.data.operational.AndroidDeviceIdentity
import com.vaiinilla.app.data.operational.RemoteCashSessionRepository
import com.vaiinilla.app.data.operational.RemoteDeviceHeartbeatRepository
import com.vaiinilla.app.data.order.OrderContractJson
import com.vaiinilla.app.data.order.RemoteOrderRepository
import com.vaiinilla.app.data.wallet.RemoteWalletRepository
import com.vaiinilla.app.domain.account.AccountDeletionRepository
import com.vaiinilla.app.domain.auth.student.StudentAuthRepository
import com.vaiinilla.app.domain.auth.student.StudentEnrollmentRepository
import com.vaiinilla.app.domain.repository.AuthorizedAccessRepository
import com.vaiinilla.app.domain.repository.CashSessionRepository
import com.vaiinilla.app.domain.repository.CatalogRepository
import com.vaiinilla.app.domain.repository.DeviceHeartbeatRepository
import com.vaiinilla.app.domain.repository.DeviceIdentity
import com.vaiinilla.app.domain.repository.DiscoveryRepository
import com.vaiinilla.app.domain.repository.OrderRepository
import com.vaiinilla.app.domain.repository.WalletRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Production graph. The app has one data path: Firebase identity plus Railway.
 * Preview/test doubles stay outside this graph so they cannot leak into a real APK session.
 */
@Module
@InstallIn(SingletonComponent::class)
object VaiinillaModule {
    @Provides
    @Singleton
    fun provideEnvironment(): AppEnvironment =
        AppEnvironment(
            environmentName = BuildConfig.ENVIRONMENT_NAME,
            apiBaseUrl = BuildConfig.API_BASE_URL,
            webUrl = BuildConfig.WEB_URL,
            firebaseProjectId = BuildConfig.FIREBASE_PROJECT_ID,
            isProduction = BuildConfig.IS_PRODUCTION,
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE,
        )

    @Provides
    @Singleton
    fun provideSecureSessionStore(
        @ApplicationContext context: Context,
    ): SecureSessionStore = AndroidKeyStoreSessionStore(context)

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
    fun provideCatalogRepository(
        apiClient: VaiinillaApiClient,
        parser: ContractResponseParser,
    ): CatalogRepository = RemoteCatalogRepository(apiClient, parser)

    @Provides
    @Singleton
    fun provideOrderRepository(
        apiClient: VaiinillaApiClient,
        orderContractJson: OrderContractJson,
        pickupTokenStore: PickupTokenStore,
    ): OrderRepository = RemoteOrderRepository(apiClient, orderContractJson, pickupTokenStore)

    @Provides
    @Singleton
    fun provideWalletRepository(apiClient: VaiinillaApiClient): WalletRepository = RemoteWalletRepository(apiClient)

    @Provides
    @Singleton
    fun provideDeviceHeartbeatRepository(apiClient: VaiinillaApiClient): DeviceHeartbeatRepository =
        RemoteDeviceHeartbeatRepository(apiClient)

    @Provides
    @Singleton
    fun provideDeviceIdentity(identity: AndroidDeviceIdentity): DeviceIdentity = identity

    @Provides
    @Singleton
    fun provideCashSessionRepository(
        apiClient: VaiinillaApiClient,
        orderContractJson: OrderContractJson,
    ): CashSessionRepository = RemoteCashSessionRepository(apiClient, orderContractJson)

    @Provides
    @Singleton
    fun provideDiscoveryRepository(
        apiClient: VaiinillaApiClient,
        parser: ContractResponseParser,
    ): DiscoveryRepository = RemoteDiscoveryRepository(apiClient, parser)

    @Provides
    @Singleton
    fun provideAuthorizedAccessApi(api: RemoteAuthorizedAccessApi): AuthorizedAccessApi = api

    @Provides
    @Singleton
    fun provideAuthorizedAccess(
        api: AuthorizedAccessApi,
        authRepository: StudentAuthRepository,
        enrollmentRepository: StudentEnrollmentRepository,
    ): AuthorizedAccessRepository = RemoteAuthorizedAccessRepository(api, authRepository, enrollmentRepository)

    @Provides
    @Singleton
    fun provideContextoExchanger(exchange: SesionesContextoExchange): ContextoExchanger = exchange

    @Provides
    @Singleton
    fun provideStudentAuthRepository(repository: FirebaseStudentAuthRepository): StudentAuthRepository = repository

    @Provides
    @Singleton
    fun provideStudentEnrollmentRepository(api: StudentEnrollmentApi): StudentEnrollmentRepository =
        RemoteStudentEnrollmentRepository(api)

    @Provides
    @Singleton
    fun provideStudentEnrollmentApi(api: RemoteStudentEnrollmentApi): StudentEnrollmentApi = api

    @Provides
    @Singleton
    fun provideAccessEmailApi(api: RemoteAccessEmailApi): AccessEmailApi = api

    @Provides
    @Singleton
    fun provideAccountDeletionRepository(repository: RemoteAccountDeletionRepository): AccountDeletionRepository =
        repository
}
