package com.vaiinilla.app.jvmroles

import com.vaiinilla.app.core.security.SecureSessionStore
import com.vaiinilla.app.data.di.AuthModule
import com.vaiinilla.app.data.di.DeviceModule
import com.vaiinilla.app.domain.auth.student.StudentAuthRepository
import com.vaiinilla.app.domain.location.DeviceLocationProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [AuthModule::class])
object HarnessAuthModule {
    @Provides
    @Singleton
    fun provideSecureSessionStore(): SecureSessionStore = RoleSessionStore()

    @Provides
    @Singleton
    fun provideStudentAuthRepository(repository: SandboxStudentAuthRepository): StudentAuthRepository = repository
}

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DeviceModule::class])
object HarnessDeviceModule {
    @Provides
    @Singleton
    fun provideDeviceLocationProvider(provider: FakeLocationProvider): DeviceLocationProvider = provider
}
