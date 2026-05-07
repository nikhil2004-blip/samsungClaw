package com.example.signal.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.example.signal.data.local.TaskEntity
import com.example.signal.data.repository.TaskRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TaskRescheduleWorkerTest {

    private lateinit var context: Context
    private val repository = mockk<TaskRepository>()

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun worker_loadsTask_andReturnsSuccess() = runTest {
        val taskId = "task-123"
        val task = createTaskEntity(taskId, "Test Task")
        coEvery { repository.getTaskById(taskId) } returns task

        val worker = TestListenableWorkerBuilder<TaskRescheduleWorker>(context)
            .setInputData(workDataOf(TaskRescheduleWorker.KEY_TASK_ID to taskId))
            .setWorkerFactory(object : androidx.work.WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker {
                    return TaskRescheduleWorker(appContext, workerParameters, repository)
                }
            })
            .build()

        val result = worker.doWork()
        assertThat(result).isEqualTo(ListenableWorker.Result.success())
    }

    @Test
    fun worker_taskNotFound_returnsFailure() = runTest {
        val taskId = "non-existent"
        coEvery { repository.getTaskById(taskId) } returns null

        val worker = TestListenableWorkerBuilder<TaskRescheduleWorker>(context)
            .setInputData(workDataOf(TaskRescheduleWorker.KEY_TASK_ID to taskId))
            .setWorkerFactory(object : androidx.work.WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker {
                    return TaskRescheduleWorker(appContext, workerParameters, repository)
                }
            })
            .build()

        val result = worker.doWork()
        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
    }

    private fun createTaskEntity(id: String, taskName: String) = TaskEntity(
        id = id,
        sourceApp = "WhatsApp",
        packageName = "com.whatsapp",
        originalTitle = "Title",
        originalBody = "Body",
        capturedAt = System.currentTimeMillis(),
        extractedTask = taskName,
        importance = "MEDIUM",
        category = "MESSAGE",
        deadline = null,
        deadlineTimestamp = null,
        suggestedActions = "[]",
        status = "PENDING",
        userDecision = "SCHEDULE",
        ignoreReason = null,
        scheduledFor = System.currentTimeMillis(),
        decidedAt = System.currentTimeMillis(),
        completedAt = null,
        requiresEnforcement = false,
        isOverdue = false,
        rescheduleCount = 0
    )
}
