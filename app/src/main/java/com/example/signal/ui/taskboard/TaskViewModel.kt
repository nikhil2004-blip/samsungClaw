package com.example.signal.ui.taskboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.signal.data.local.TaskEntity
import com.example.signal.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── Filter chip definitions ────────────────────────────────────────────────────
enum class TaskFilter(val label: String, val emoji: String) {
    ALL(          "All Messages",  "📬"),
    HIGH_PRIORITY("Priority",      "🔴"),
    DEADLINES(    "Deadlines",     "⏰"),
    MEETINGS(     "Meetings",      "🤝"),
    MESSAGES(     "Messages",      "💬"),
    PAYMENTS(     "Payments",      "💳"),
    REMINDERS(    "Reminders",     "🔔"),
    MISSED(       "Missed",        "😶‍🌫️"),
    ADVERTS(      "Adverts",       "🏷️"),
    OTHER(        "Other",         "📌")
}

data class TaskBoardUiState(
    val allMessages:  List<TaskEntity> = emptyList(),
    val highPriority: List<TaskEntity> = emptyList(),
    val deadlines:    List<TaskEntity> = emptyList(),
    val meetings:     List<TaskEntity> = emptyList(),
    val messages:     List<TaskEntity> = emptyList(),
    val payments:     List<TaskEntity> = emptyList(),
    val reminders:    List<TaskEntity> = emptyList(),
    val missed:       List<TaskEntity> = emptyList(),
    val adverts:      List<TaskEntity> = emptyList(),
    val other:        List<TaskEntity> = emptyList(),
    // Board sections shown in the ALL tab
    val overdue:      List<TaskEntity> = emptyList(),
    val pending:      List<TaskEntity> = emptyList(),
    val inProgress:   List<TaskEntity> = emptyList(),
    val done:         List<TaskEntity> = emptyList(),
    val searchQuery:  String           = "",
    val selectedFilter: TaskFilter     = TaskFilter.ALL
) {
    /** Returns the list appropriate for the selected filter tab. */
    fun currentList(): List<TaskEntity> = when (selectedFilter) {
        TaskFilter.ALL           -> allMessages
        TaskFilter.HIGH_PRIORITY -> highPriority
        TaskFilter.DEADLINES     -> deadlines
        TaskFilter.MEETINGS      -> meetings
        TaskFilter.MESSAGES      -> messages
        TaskFilter.PAYMENTS      -> payments
        TaskFilter.REMINDERS     -> reminders
        TaskFilter.MISSED        -> missed
        TaskFilter.ADVERTS       -> adverts
        TaskFilter.OTHER         -> other
    }
}

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val repository: TaskRepository
) : ViewModel() {

    private val _searchQuery    = MutableStateFlow("")
    private val _selectedFilter = MutableStateFlow(TaskFilter.ALL)

    init {
        viewModelScope.launch {
            repository.updateDynamicPriorities()
        }
        // ── Periodic sweep: immediately then every 60 s ───────────────────────
        // Moves past-deadline PENDING/IN_PROGRESS tasks → MISSED right away
        viewModelScope.launch {
            while (true) {
                repository.sweepMissedTasks()
                repository.markOverdueTasks()
                delay(60_000L)
            }
        }
    }

    // ── Combine all sources cleanly ────────────────────────────────────────────
    val uiState: StateFlow<TaskBoardUiState> =
        combine(
            combine(
                repository.getAllTasksSortedByPriority(),
                repository.getOverdueTasks(),
                repository.getPendingTasks(),
                repository.getInProgressTasks()
            ) { all, overdue, pending, inProgress ->
                Triple(all, overdue, Triple(pending, inProgress, Unit))
            },
            combine(
                repository.getDoneTasks(),
                repository.getPromotionalTasks(),
                repository.getMissedTasks(),
                _searchQuery
            ) { done, promos, missed, query ->
                Triple(done, promos, Pair(missed, query))
            },
            _selectedFilter
        ) { left, right, filter ->
            val allActive  = left.first
            val overdue    = left.second
            val pending    = left.third.first
            val inProgress = left.third.second

            val done   = right.first
            val promos = right.second
            val missed = right.third.first
            val query  = right.third.second

            fun List<TaskEntity>.search() = if (query.isBlank()) this else filter { t ->
                t.extractedTask.contains(query, ignoreCase = true) ||
                t.sourceApp.contains(query, ignoreCase = true)    ||
                t.originalTitle.contains(query, ignoreCase = true)
            }

            TaskBoardUiState(
                allMessages   = allActive.search(),
                highPriority  = allActive.filter { it.importance in listOf("CRITICAL", "HIGH") }.search(),
                deadlines     = allActive.filter { it.category == "DEADLINE" }.search(),
                meetings      = allActive.filter { it.category == "MEETING" }.search(),
                messages      = allActive.filter { it.category == "MESSAGE" }.search(),
                payments      = allActive.filter { it.category == "PAYMENT" }.search(),
                reminders     = allActive.filter { it.category == "REMINDER" }.search(),
                missed        = missed.search(),
                adverts       = promos.search(),
                other         = allActive.filter { it.category == "OTHER" }.search(),
                overdue       = overdue.search(),
                pending       = pending.search(),
                inProgress    = inProgress.search(),
                done          = done.search(),
                searchQuery   = query,
                selectedFilter = filter
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaskBoardUiState())

    fun setSearchQuery(q: String)   { _searchQuery.value = q }
    fun setFilter(f: TaskFilter)    { _selectedFilter.value = f }

    fun addManualTask(task: TaskEntity) {
        viewModelScope.launch { repository.insertManualTask(task) }
    }

    fun markDone(taskId: String) {
        viewModelScope.launch { repository.markDone(taskId) }
    }

    /** Called when user taps "I did this" on a missed item. */
    fun markMissedAsDone(taskId: String) {
        viewModelScope.launch { repository.markMissedAsDone(taskId) }
    }

    fun getSuggestedActions(task: TaskEntity) =
        repository.parseSuggestedActions(task.suggestedActions)
}
