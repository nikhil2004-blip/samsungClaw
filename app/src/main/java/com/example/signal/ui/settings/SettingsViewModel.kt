package com.example.signal.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.signal.data.repository.OnboardingRepository
import com.example.signal.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: TaskRepository,
    private val onboardingRepository: OnboardingRepository
) : ViewModel() {

    /** Reflects the stored dark-mode preference. */
    val isDarkTheme: StateFlow<Boolean> = onboardingRepository.isDarkTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun toggleDarkTheme(dark: Boolean) {
        viewModelScope.launch { onboardingRepository.setDarkTheme(dark) }
    }

    fun clearAll() = viewModelScope.launch { repository.clearAll() }
}
