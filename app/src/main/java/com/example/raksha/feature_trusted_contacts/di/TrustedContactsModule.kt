package com.example.raksha.feature_trusted_contacts.di

import com.example.raksha.feature_trusted_contacts.data.remote.api.UserTrustedContactsAPI
import com.example.raksha.feature_trusted_contacts.data.repository.TrustedContactsRepositoryImpl
import com.example.raksha.feature_trusted_contacts.domain.repository.TrustedContactsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object TrustedContactsNetworkModule {
    @Provides
    @Singleton
    fun provideTrustedContactsApi(
        retrofit: Retrofit
    ): UserTrustedContactsAPI {
        return retrofit.create(UserTrustedContactsAPI::class.java)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class TrustedContactsRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindTrustedContactsRepository(
        repositoryImpl: TrustedContactsRepositoryImpl
    ): TrustedContactsRepository
}
