package com.example.signal.fake

import com.example.signal.data.repository.OnboardingRepositoryInterface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeOnboardingRepository : OnboardingRepositoryInterface {
    private val _onboardingCompleted = MutableStateFlow(true)
    override val onboardingCompleted: Flow<Boolean> = _onboardingCompleted

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        _onboardingCompleted.value = completed
    }
}
