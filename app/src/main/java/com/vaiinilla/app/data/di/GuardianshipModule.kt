package com.vaiinilla.app.data.di

import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.data.guardianship.DependentContextSession
import com.vaiinilla.app.data.guardianship.RemoteDependentAccountRepository
import com.vaiinilla.app.data.guardianship.RemoteGuardianshipRepository
import com.vaiinilla.app.domain.guardianship.DependentAccountFactory
import com.vaiinilla.app.domain.guardianship.GuardianshipRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Padres e hijos, aparte de VaiinillaModule para que el merge con Caja y Escuela no choque. */
@Module
@InstallIn(SingletonComponent::class)
object GuardianshipModule {
    @Provides
    @Singleton
    fun provideGuardianshipRepository(apiClient: VaiinillaApiClient): GuardianshipRepository =
        RemoteGuardianshipRepository(apiClient)

    /** Una sesión de contexto por hijo abierto; se descarta con la ficha. */
    @Provides
    @Singleton
    fun provideDependentAccountFactory(
        apiClient: VaiinillaApiClient,
        guardianship: GuardianshipRepository,
    ): DependentAccountFactory =
        DependentAccountFactory { dependentId ->
            RemoteDependentAccountRepository(
                apiClient = apiClient,
                session = DependentContextSession(dependentId, guardianship::issueContext),
            )
        }
}
