package com.example.signal.ui.enforcement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.signal.data.local.TaskEntity
import com.example.signal.data.model.UserDecision
import com.example.signal.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EnforcementViewModel @Inject constructor(
    private val repository: TaskRepository
) : ViewModel() {

    private val _task = MutableStateFlow<TaskEntity?>(null)
    val task: StateFlow<TaskEntity?> = _task

    private val _decisionComplete = MutableStateFlow(false)
    val decisionComplete: StateFlow<Boolean> = _decisionComplete

    fun loadTask(taskId: String) {
        viewModelScope.launch {
            _task.value = repository.getTaskById(taskId)
        }
    }

    fun doNow(taskId: String) {
        viewModelScope.launch {
            repository.applyDecision(taskId, UserDecision.DO_NOW)
            _decisionComplete.value = true
        }
    }

    fun schedule(taskId: String, scheduledForMs: Long) {
        viewModelScope.launch {
            repository.applyDecision(taskId, UserDecision.SCHEDULE)
            repository.scheduleTask(taskId, scheduledForMs)
            _decisionComplete.value = true
        }
    }

    fun delegate(taskId: String) {
        viewModelScope.launch {
            repository.applyDecision(taskId, UserDecision.DELEGATE)
            _decisionComplete.value = true
        }
    }

    fun ignore(taskId: String, reason: String) {
        viewModelScope.launch {
            repository.applyDecision(taskId, UserDecision.IGNORE)
            repository.setIgnoreReason(taskId, reason)
            _decisionComplete.value = true
        }
    }

    fun getSuggestedActions(task: TaskEntity): List<String> =
        repository.parseSuggestedActions(task.suggestedActions)
}
