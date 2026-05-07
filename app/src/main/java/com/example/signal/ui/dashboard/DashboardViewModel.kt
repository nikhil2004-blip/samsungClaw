package com.example.signal.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.signal.data.local.CategoryCount
import com.example.signal.data.local.TaskEntity
import com.example.signal.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class DayStats(
    val label: String,     // "Mon"
    val actioned: Int,
    val scheduled: Int,
    val ignored: Int
)

data class DashboardUiState(
    val todayTotal: Int       = 0,
    val todayActioned: Int    = 0,
    val todayScheduled: Int   = 0,
    val todayIgnored: Int     = 0,
    val overdueCount: Int     = 0,
    val streakDays: Int       = 0,
    val weeklyData: List<DayStats> = emptyList(),
    val topIgnoredCategories: List<CategoryCount> = emptyList(),
    val isLoading: Boolean    = true
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState

    init { loadStats() }

    fun loadStats() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val now        = System.currentTimeMillis()
            val startOfDay = startOfDay()
            val startWeek  = startOfWeek()

            val todayTotal    = repository.getTodayCount(startOfDay)
            val todayActioned = repository.getTodayActionedCount(startOfDay)
            val todayScheduled = repository.getTodayScheduledCount(startOfDay)
            val todayIgnored  = repository.getTodayIgnoredCount(startOfDay)
            val overdueCount  = repository.getOverdueCount()
            val weekTasks     = repository.getTasksThisWeek(startWeek)
            val topIgnored    = repository.getTopIgnoredCategories()

            val weeklyData = buildWeeklyData(weekTasks, startWeek)
            val streak     = computeStreak(weekTasks)

            _uiState.value = DashboardUiState(
                todayTotal    = todayTotal,
                todayActioned = todayActioned,
                todayScheduled = todayScheduled,
                todayIgnored  = todayIgnored,
                overdueCount  = overdueCount,
                streakDays    = streak,
                weeklyData    = weeklyData,
                topIgnoredCategories = topIgnored,
                isLoading     = false
            )
        }
    }

    private fun buildWeeklyData(tasks: List<TaskEntity>, startWeek: Long): List<DayStats> {
        val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        return labels.mapIndexed { index, label ->
            val dayStart = startWeek + index * 86_400_000L
            val dayEnd   = dayStart + 86_400_000L
            val dayTasks = tasks.filter { it.capturedAt in dayStart until dayEnd }
            DayStats(
                label     = label,
                actioned  = dayTasks.count { it.userDecision == "DO_NOW" },
                scheduled = dayTasks.count { it.userDecision == "SCHEDULE" },
                ignored   = dayTasks.count { it.userDecision == "IGNORE" }
            )
        }
    }

    private fun computeStreak(tasks: List<TaskEntity>): Int {
        // Count consecutive days (ending today) with 0 ignored critical tasks
        var streak = 0
        val today = startOfDay()
        for (i in 0..6) {
            val dayStart = today - i * 86_400_000L
            val dayEnd   = dayStart + 86_400_000L
            val ignoredCritical = tasks.count { t ->
                t.capturedAt in dayStart until dayEnd &&
                t.userDecision == "IGNORE" &&
                t.importance == "CRITICAL"
            }
            if (ignoredCritical == 0) streak++ else break
        }
        return streak
    }

    private fun startOfDay(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun startOfWeek(): Long {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
