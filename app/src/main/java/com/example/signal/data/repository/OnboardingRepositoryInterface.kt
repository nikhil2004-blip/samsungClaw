package com.example.signal.data.repository

import kotlinx.coroutines.flow.Flow

interface OnboardingRepositoryInterface {
    val onboardingCompleted: Flow<Boolean>
    suspend fun setOnboardingCompleted(completed: Boolean)
}
