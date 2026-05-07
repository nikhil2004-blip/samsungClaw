package com.example.signal.data.repository

import android.content.Context
import com.example.signal.data.local.AppDatabase
import com.example.signal.data.local.TaskDao
import com.example.signal.data.local.TaskEntity
import com.example.signal.data.model.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class TaskRepositoryTest {

    private lateinit var repository: TaskRepositoryImpl
    private val dao = mockk<TaskDao>(relaxed = true)
    private val db = mockk<AppDatabase>()
    private val context = mockk<Context>(relaxed = true)

    @Before
    fun setup() {
        every { db.taskDao() } returns dao
        repository = TaskRepositoryImpl(db, context)
    }

    @Test
    fun insertClassifiedTask_callsDaoInsert_withCorrectEntity() = runTest {
        val notificationData = NotificationData("1", "WhatsApp", "com.whatsapp", "Title", "Body", 123L)
        val classified = ClassifiedTask(
            notificationId = "1",
            importance = ImportanceLevel.CRITICAL,
            category = TaskCategory.DEADLINE,
            task = "Submit IEEE paper",
            deadline = "Tonight",
            deadlineTimestamp = 456L,
            suggestedActions = listOf("Action1"),
            requiresEnforcement = true
        )

        repository.insertFromClassified(notificationData, classified)

        coVerify { dao.insertTask(any()) }
    }

    @Test
    fun updateDecision_doNow_setsInProgress() = runTest {
        repository.applyDecision("1", UserDecision.DO_NOW)
        coVerify { dao.updateDecision("1", "IN_PROGRESS", "DO_NOW", any()) }
    }

    @Test
    fun updateDecision_ignore_savesReason() = runTest {
        repository.applyDecision("1", UserDecision.IGNORE)
        repository.setIgnoreReason("1", "Too busy right now")
        
        coVerify { dao.updateDecision("1", "IGNORED", "IGNORE", any()) }
        coVerify { dao.updateIgnoreReason("1", "Too busy right now") }
    }

    @Test
    fun updateDecision_schedule_savesScheduledFor() = runTest {
        repository.scheduleTask("1", 789L)
        coVerify { dao.updateSchedule("1", 789L) }
    }

    @Test
    fun getPendingTasks_delegatesToDao() = runTest {
        val flow = flowOf(emptyList<TaskEntity>())
        every { dao.getPendingTasks() } returns flow
        
        val result = repository.getPendingTasks()
        assert(result == flow)
    }

    @Test
    fun getOverdueTasks_delegatesToDao() = runTest {
        val flow = flowOf(emptyList<TaskEntity>())
        every { dao.getOverdueTasks() } returns flow
        
        val result = repository.getOverdueTasks()
        assert(result == flow)
    }

    @Test
    fun markTaskComplete_setsStatusDone() = runTest {
        repository.markDone("1")
        coVerify { dao.markDone("1", any()) }
    }
}
