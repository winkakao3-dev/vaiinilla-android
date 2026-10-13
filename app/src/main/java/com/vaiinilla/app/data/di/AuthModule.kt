package com.vaiinilla.app.data.di

import android.content.Context
import com.vaiinilla.app.core.security.AndroidKeyStoreSessionStore
import com.vaiinilla.app.core.security.SecureSessionStore
import com.vaiinilla.app.data.auth.student.FirebaseStudentAuthRepository
import com.vaiinilla.app.domain.auth.student.StudentAuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Identidad y sesión, aparte del grafo de datos para que el arnés de la JVM pueda cambiarlas solas. */
@Module
@InstallIn(SingletonComponent::class)
object AuthModule {
    @Provides
    @Singleton
    fun provideSecureSessionStore(
        @ApplicationContext context: Context,
    ): SecureSessionStore = AndroidKeyStoreSessionStore(context)

    @Provides
    @Singleton
    fun provideStudentAuthRepository(repository: FirebaseStudentAuthRepository): StudentAuthRepository = repository
}
