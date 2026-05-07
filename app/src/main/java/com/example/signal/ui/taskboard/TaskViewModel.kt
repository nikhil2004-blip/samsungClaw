package com.example.signal.ui.taskboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.signal.data.local.TaskEntity
import com.example.signal.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskBoardUiState(
    val overdue: List<TaskEntity>     = emptyList(),
    val pending: List<TaskEntity>     = emptyList(),
    val inProgress: List<TaskEntity>  = emptyList(),
    val done: List<TaskEntity>        = emptyList(),
    val searchQuery: String           = "",
    val selectedFilter: String        = "All"
)

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val repository: TaskRepository
) : ViewModel() {

    private val _searchQuery    = MutableStateFlow("")
    private val _selectedFilter = MutableStateFlow("All")

    val uiState: StateFlow<TaskBoardUiState> = combine(
        repository.getOverdueTasks(),
        repository.getPendingTasks(),
        repository.getInProgressTasks(),
        repository.getDoneTasks(),
        _searchQuery,
        _selectedFilter
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val overdue  = args[0] as List<TaskEntity>
        val pending  = args[1] as List<TaskEntity>
        val progress = args[2] as List<TaskEntity>
        val done     = args[3] as List<TaskEntity>
        val query    = args[4] as String
        val filter   = args[5] as String

        fun List<TaskEntity>.applyFilters(): List<TaskEntity> = this
            .filter { task ->
                filter == "All" || task.category.equals(
                    when (filter) {
                        "Deadlines" -> "DEADLINE"
                        "Meetings"  -> "MEETING"
                        "Payments"  -> "PAYMENT"
                        "Messages"  -> "MESSAGE"
                        else        -> filter.uppercase()
                    }, ignoreCase = true
                )
            }
            .filter { task ->
                query.isBlank() ||
                task.extractedTask.contains(query, ignoreCase = true) ||
                task.sourceApp.contains(query, ignoreCase = true)
            }

        TaskBoardUiState(
            overdue       = overdue.applyFilters(),
            pending       = pending.applyFilters(),
            inProgress    = progress.applyFilters(),
            done          = done.applyFilters(),
            searchQuery   = query,
            selectedFilter = filter
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaskBoardUiState())

    fun setSearchQuery(q: String)  { _searchQuery.value = q }
    fun setFilter(filter: String)  { _selectedFilter.value = filter }

    fun addManualTask(task: TaskEntity) {
        viewModelScope.launch { repository.insertManualTask(task) }
    }

    fun markDone(taskId: String) {
        viewModelScope.launch { repository.markDone(taskId) }
    }

    fun getSuggestedActions(task: TaskEntity) = repository.parseSuggestedActions(task.suggestedActions)
}
