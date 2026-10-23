package com.vaiinilla.app.data.di

import android.content.Context
import com.vaiinilla.app.core.notifications.DeviceTokenRegistrar
import com.vaiinilla.app.core.security.AndroidKeyStoreSessionStore
import com.vaiinilla.app.core.security.ContextAwareSessionStore
import com.vaiinilla.app.core.security.PickupTokenStore
import com.vaiinilla.app.core.security.SecureSessionStore
import com.vaiinilla.app.core.security.SharedPreferencesPickupTokenStore
import com.vaiinilla.app.data.auth.student.FirebaseStudentAuthRepository
import com.vaiinilla.app.domain.auth.student.StudentAuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

/** Identidad, sesión y sus secretos (cifrados con AndroidKeyStore), aparte del grafo de datos para que el arnés de la JVM pueda cambiarlas solas. */
@Module
@InstallIn(SingletonComponent::class)
object AuthModule {
    @Provides
    @Singleton
    fun provideSecureSessionStore(
        @ApplicationContext context: Context,
        deviceTokenRegistrar: Provider<DeviceTokenRegistrar>,
    ): SecureSessionStore =
        // Provider: el registrar también lee la sesión; se resuelve hasta el primer contexto guardado.
        ContextAwareSessionStore(AndroidKeyStoreSessionStore(context)) { deviceTokenRegistrar.get().register() }

    @Provides
    @Singleton
    fun providePickupTokenStore(store: SharedPreferencesPickupTokenStore): PickupTokenStore = store

    @Provides
    @Singleton
    fun provideStudentAuthRepository(repository: FirebaseStudentAuthRepository): StudentAuthRepository = repository
}
