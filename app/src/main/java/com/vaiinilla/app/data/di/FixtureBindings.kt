package com.vaiinilla.app.data.di

import com.vaiinilla.app.data.fixture.AndroidAssetFixtureSource
import com.vaiinilla.app.data.fixture.FixtureSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FixtureBindings {
    @Binds
    @Singleton
    abstract fun bindFixtureSource(implementation: AndroidAssetFixtureSource): FixtureSource
}
