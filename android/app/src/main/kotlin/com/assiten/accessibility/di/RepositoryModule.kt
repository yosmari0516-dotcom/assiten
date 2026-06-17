package com.assiten.accessibility.di

import com.assiten.accessibility.data.remote.ApiClient
import com.assiten.accessibility.data.repository.AuthorizationRepositoryImpl
import com.assiten.accessibility.domain.repository.AuthorizationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * RepositoryModule: Inyección de dependencias para repositorios
 * Usa Dagger Hilt para gestionar las dependencias
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAuthorizationRepository(): AuthorizationRepository {
        return AuthorizationRepositoryImpl(ApiClient)
    }
}
