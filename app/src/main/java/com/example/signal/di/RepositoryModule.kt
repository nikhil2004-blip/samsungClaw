package com.example.signal.di

import com.example.signal.data.repository.OnboardingRepository
import com.example.signal.data.repository.OnboardingRepositoryInterface
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindOnboardingRepository(
        onboardingRepository: OnboardingRepository
    ): OnboardingRepositoryInterface
}
