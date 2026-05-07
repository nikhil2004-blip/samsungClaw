package com.example.signal.viewmodel

import com.example.signal.data.local.CategoryCount
import com.example.signal.data.local.TaskEntity
import com.example.signal.fake.FakeTaskRepository
import com.example.signal.ui.dashboard.DashboardViewModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private lateinit var viewModel: DashboardViewModel
    private val repository = FakeTaskRepository()
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = DashboardViewModel(repository)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        repository.reset()
    }

    @Test
    fun todaySummary_correctCounts() = runTest {
        val todayStart = startOfDay()
        
        // 2 DO_NOW, 1 SCHEDULE, 1 IGNORE, 1 PENDING
        repository.insertManualTask(createTaskEntity("1", todayStart + 100, userDecision = "DO_NOW"))
        repository.insertManualTask(createTaskEntity("2", todayStart + 200, userDecision = "DO_NOW"))
        repository.insertManualTask(createTaskEntity("3", todayStart + 300, userDecision = "SCHEDULE"))
        repository.insertManualTask(createTaskEntity("4", todayStart + 400, userDecision = "IGNORE"))
        repository.insertManualTask(createTaskEntity("5", todayStart + 500, userDecision = null))

        viewModel.loadStats()
        
        val state = viewModel.uiState.value
        assertThat(state.todayActioned).isEqualTo(2)
        assertThat(state.todayScheduled).isEqualTo(1)
        assertThat(state.todayIgnored).isEqualTo(1)
        assertThat(state.todayTotal).isEqualTo(5)
    }

    @Test
    fun streakCounter_incrementsOnDayWithNoCriticalIgnored() = runTest {
        val today = startOfDay()
        
        // 3 consecutive days with no ignored-critical tasks
        // Day 0 (Today), Day 1 (Yesterday), Day 2 (Day before)
        // We don't need to insert anything, as 0 ignored critical = streak continues
        
        viewModel.loadStats()
        
        // Streak logic in ViewModel counts backward from today.
        // If 0 tasks exist, ignoredCritical is 0, so streak increments.
        assertThat(viewModel.uiState.value.streakDays).isAtLeast(1)
    }

    @Test
    fun streakCounter_resetsOnCriticalIgnoredDay() = runTest {
        val today = startOfDay()
        
        // Insert a critical ignored task today
        repository.insertManualTask(createTaskEntity("1", today + 100, userDecision = "IGNORE", importance = "CRITICAL"))
        
        viewModel.loadStats()
        
        // Streak should be 0 because today has an ignored critical task
        // Wait, the logic in computeStreak is: 
        // if (ignoredCritical == 0) streak++ else break
        // If today has 1, streak++ is skipped and it breaks. So streak = 0.
        assertThat(viewModel.uiState.value.streakDays).isEqualTo(0)
    }

    @Test
    fun topIgnoredCategories_returnsTop3() = runTest {
        // Seed: 5 ignored PAYMENT tasks, 3 ignored DEADLINE, 1 ignored MESSAGE
        repeat(5) { repository.insertManualTask(createTaskEntity("P$it", System.currentTimeMillis(), userDecision = "IGNORE", category = "PAYMENT")) }
        repeat(3) { repository.insertManualTask(createTaskEntity("D$it", System.currentTimeMillis(), userDecision = "IGNORE", category = "DEADLINE")) }
        repeat(1) { repository.insertManualTask(createTaskEntity("M$it", System.currentTimeMillis(), userDecision = "IGNORE", category = "MESSAGE")) }

        viewModel.loadStats()
        
        val top = viewModel.uiState.value.topIgnoredCategories
        assertThat(top).hasSize(3)
        assertThat(top[0].category).isEqualTo("PAYMENT")
        assertThat(top[1].category).isEqualTo("DEADLINE")
        assertThat(top[2].category).isEqualTo("MESSAGE")
    }

    @Test
    fun weeklyBarData_hasSevenEntries() = runTest {
        viewModel.loadStats()
        assertThat(viewModel.uiState.value.weeklyData).hasSize(7)
    }

    private fun startOfDay(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun createTaskEntity(
        id: String,
        capturedAt: Long,
        userDecision: String? = null,
        importance: String = "MEDIUM",
        category: String = "MESSAGE"
    ) = TaskEntity(
        id = id,
        sourceApp = "WhatsApp",
        packageName = "com.whatsapp",
        originalTitle = "Title",
        originalBody = "Body",
        capturedAt = capturedAt,
        extractedTask = "Task",
        importance = importance,
        category = category,
        deadline = null,
        deadlineTimestamp = null,
        suggestedActions = "[]",
        status = if (userDecision == "IGNORE") "IGNORED" else if (userDecision != null) "IN_PROGRESS" else "PENDING",
        userDecision = userDecision,
        ignoreReason = if (userDecision == "IGNORE") "Reason" else null,
        scheduledFor = null,
        decidedAt = if (userDecision != null) capturedAt else null,
        completedAt = null,
        requiresEnforcement = false,
        isOverdue = false,
        rescheduleCount = 0
    )
}
