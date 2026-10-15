package com.vaiinilla.app.data.di

import com.vaiinilla.app.data.auth.ContextoExchanger
import com.vaiinilla.app.data.auth.SesionesContextoExchange
import com.vaiinilla.app.data.auth.student.RemoteStudentEnrollmentRepository
import com.vaiinilla.app.data.auth.student.StudentEnrollmentApi
import com.vaiinilla.app.domain.auth.student.StudentEnrollmentRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Alta y contexto de cliente, aparte de VaiinillaModule para que el arnés jvm-roles los reemplace: sus personas
 * entran por custom token, sin correo verificado, y el backend les niega identidad/alta y sesiones/contexto-cliente.
 */
@Module
@InstallIn(SingletonComponent::class)
object StudentContextModule {
    @Provides
    @Singleton
    fun provideContextoExchanger(exchange: SesionesContextoExchange): ContextoExchanger = exchange

    @Provides
    @Singleton
    fun provideStudentEnrollmentRepository(api: StudentEnrollmentApi): StudentEnrollmentRepository =
        RemoteStudentEnrollmentRepository(api)
}
