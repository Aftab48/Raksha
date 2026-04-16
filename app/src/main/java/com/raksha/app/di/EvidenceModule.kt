package com.raksha.app.di

import com.raksha.app.BuildConfig
import com.raksha.app.evidence.MockPoliceStreamUrl
import com.raksha.app.evidence.MockPoliceUploader
import com.raksha.app.evidence.MockPoliceUploaderImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EvidenceConfigModule {
    @Provides
    @Singleton
    @MockPoliceStreamUrl
    fun provideMockPoliceStreamUrl(): String = BuildConfig.MOCK_POLICE_STREAM_URL
}

@Module
@InstallIn(SingletonComponent::class)
abstract class EvidenceModule {
    @Binds
    @Singleton
    abstract fun bindMockPoliceUploader(
        impl: MockPoliceUploaderImpl
    ): MockPoliceUploader
}
