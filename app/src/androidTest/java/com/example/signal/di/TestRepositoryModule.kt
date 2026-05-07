package com.example.signal.di

import com.example.signal.data.repository.OnboardingRepositoryInterface
import com.example.signal.data.repository.TaskRepository
import com.example.signal.fake.FakeOnboardingRepository
import com.example.signal.fake.FakeTaskRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DatabaseModule::class, RepositoryModule::class]
)
object TestRepositoryModule {

    @Provides
    @Singleton
    fun provideFakeTaskRepository(): TaskRepository = FakeTaskRepository()

    @Provides
    @Singleton
    fun provideFakeOnboardingRepository(): OnboardingRepositoryInterface = FakeOnboardingRepository()
}
