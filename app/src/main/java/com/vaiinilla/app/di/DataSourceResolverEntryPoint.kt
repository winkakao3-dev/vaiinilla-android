package com.vaiinilla.app.di

import com.vaiinilla.app.core.config.EffectiveDataSourceResolver
import com.vaiinilla.app.ui.demo.DemoGallerySeeder
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DataSourceResolverEntryPoint {
    fun effectiveDataSourceResolver(): EffectiveDataSourceResolver
    fun demoGallerySeeder(): DemoGallerySeeder
}
