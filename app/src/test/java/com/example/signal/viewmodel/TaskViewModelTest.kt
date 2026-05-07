package com.example.signal.viewmodel

import com.example.signal.data.local.TaskEntity
import com.example.signal.fake.FakeTaskRepository
import com.example.signal.ui.taskboard.TaskFilter
import com.example.signal.ui.taskboard.TaskViewModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModelTest {

    private lateinit var viewModel: TaskViewModel
    private val repository = FakeTaskRepository()
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = TaskViewModel(repository)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        repository.reset()
    }

    @Test
    fun pendingTasks_initialState_isEmptyList() = runTest {
        val job = launch(testDispatcher) { viewModel.uiState.collect() }
        assertThat(viewModel.uiState.value.pending).isEmpty()
        job.cancel()
    }

    @Test
    fun pendingTasks_afterInsert_emitsNewTask() = runTest {
        val job = launch(testDispatcher) { viewModel.uiState.collect() }
        val task = createTaskEntity("1", "Submit report", status = "PENDING")
        repository.insertManualTask(task)
        
        assertThat(viewModel.uiState.value.pending).hasSize(1)
        assertThat(viewModel.uiState.value.pending[0].extractedTask).isEqualTo("Submit report")
        job.cancel()
    }

    @Test
    fun searchQuery_filtersTasksByName() = runTest {
        val job = launch(testDispatcher) { viewModel.uiState.collect() }
        repository.insertManualTask(createTaskEntity("1", "Submit report"))
        repository.insertManualTask(createTaskEntity("2", "Pay electricity bill"))
        repository.insertManualTask(createTaskEntity("3", "Reply to email"))

        viewModel.setSearchQuery("report")
        
        assertThat(viewModel.uiState.value.allMessages).hasSize(1)
        assertThat(viewModel.uiState.value.allMessages[0].extractedTask).isEqualTo("Submit report")
        job.cancel()
    }

    @Test
    fun filterByCategory_showsOnlyMatchingCategory() = runTest {
        val job = launch(testDispatcher) { viewModel.uiState.collect() }
        repository.insertManualTask(createTaskEntity("1", "Deadline", category = "DEADLINE"))
        repository.insertManualTask(createTaskEntity("2", "Meeting", category = "MEETING"))
        repository.insertManualTask(createTaskEntity("3", "Payment", category = "PAYMENT"))

        viewModel.setFilter(TaskFilter.MEETINGS)
        
        assertThat(viewModel.uiState.value.currentList()).hasSize(1)
        assertThat(viewModel.uiState.value.currentList()[0].category).isEqualTo("MEETING")
        job.cancel()
    }

    @Test
    fun makeDecision_doNow_updatesTaskInRepo() = runTest {
        val job = launch(testDispatcher) { viewModel.uiState.collect() }
        repository.insertManualTask(createTaskEntity("1", "Task 1", status = "PENDING"))
        
        viewModel.markDone("1")
        
        val task = repository.getTaskById("1")
        assertThat(task?.status).isEqualTo("DONE")
        job.cancel()
    }

    @Test
    fun overdueTasks_flaggedCorrectly() = runTest {
        val job = launch(testDispatcher) { viewModel.uiState.collect() }
        val past = System.currentTimeMillis() - 3600000
        repository.insertManualTask(createTaskEntity("1", "Overdue", deadlineTimestamp = past, isOverdue = true))
        
        assertThat(viewModel.uiState.value.overdue).hasSize(1)
        assertThat(viewModel.uiState.value.overdue[0].extractedTask).isEqualTo("Overdue")
        job.cancel()
    }

    private fun createTaskEntity(
        id: String,
        taskName: String,
        status: String = "PENDING",
        category: String = "MESSAGE",
        deadlineTimestamp: Long? = null,
        isOverdue: Boolean = false
    ) = TaskEntity(
        id = id,
        sourceApp = "WhatsApp",
        packageName = "com.whatsapp",
        originalTitle = "Title",
        originalBody = "Body",
        capturedAt = System.currentTimeMillis(),
        extractedTask = taskName,
        importance = "MEDIUM",
        category = category,
        deadline = null,
        deadlineTimestamp = deadlineTimestamp,
        suggestedActions = "[]",
        status = status,
        userDecision = null,
        ignoreReason = null,
        scheduledFor = null,
        decidedAt = null,
        completedAt = null,
        requiresEnforcement = false,
        isOverdue = isOverdue,
        rescheduleCount = 0
    )
}
