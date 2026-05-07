package com.example.signal.data.db

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.signal.data.local.AppDatabase
import com.example.signal.data.local.TaskDao
import com.example.signal.data.local.TaskEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TaskDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: AppDatabase
    private lateinit var dao: TaskDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.taskDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertTask_and_retrieveById() = runTest {
        val task = createTaskEntity("1", "Task 1")
        dao.insertTask(task)
        val retrieved = dao.getTaskById("1")
        assertThat(retrieved).isEqualTo(task)
    }

    @Test
    fun getPendingTasks_returnsOnlyPendingStatus() = runTest {
        val task1 = createTaskEntity("1", "Task 1", status = "PENDING")
        val task2 = createTaskEntity("2", "Task 2", status = "DONE")
        val task3 = createTaskEntity("3", "Task 3", status = "IGNORED")
        dao.insertTask(task1)
        dao.insertTask(task2)
        dao.insertTask(task3)

        val pending = dao.getPendingTasks().first()
        assertThat(pending).hasSize(1)
        assertThat(pending[0].id).isEqualTo("1")
    }

    @Test
    fun getOverdueTasks_returnsTasksWithPassedDeadline() = runTest {
        val now = System.currentTimeMillis()
        val pastTask = createTaskEntity("1", "Past", deadlineTimestamp = now - 1000, isOverdue = true)
        val futureTask = createTaskEntity("2", "Future", deadlineTimestamp = now + 1000)
        dao.insertTask(pastTask)
        dao.insertTask(futureTask)

        val overdue = dao.getOverdueTasks().first()
        assertThat(overdue).hasSize(1)
        assertThat(overdue[0].id).isEqualTo("1")
    }

    @Test
    fun updateDecision_changesStatusAndDecisionField() = runTest {
        val task = createTaskEntity("1", "Task 1", status = "PENDING")
        dao.insertTask(task)
        val now = System.currentTimeMillis()
        dao.updateDecision("1", "IN_PROGRESS", "DO_NOW", now)

        val updated = dao.getTaskById("1")
        assertThat(updated?.status).isEqualTo("IN_PROGRESS")
        assertThat(updated?.userDecision).isEqualTo("DO_NOW")
        assertThat(updated?.decidedAt).isEqualTo(now)
    }

    @Test
    fun getTodayCount_returnsCorrectCountForToday() = runTest {
        val todayStart = System.currentTimeMillis() - 10000
        val yesterday = todayStart - 86400000
        dao.insertTask(createTaskEntity("1", "Today 1", capturedAt = todayStart + 100))
        dao.insertTask(createTaskEntity("2", "Today 2", capturedAt = todayStart + 200))
        dao.insertTask(createTaskEntity("3", "Today 3", capturedAt = todayStart + 300))
        dao.insertTask(createTaskEntity("4", "Yesterday 1", capturedAt = yesterday))
        dao.insertTask(createTaskEntity("5", "Yesterday 2", capturedAt = yesterday + 1000))

        val count = dao.getTodayCount(todayStart)
        assertThat(count).isEqualTo(3)
    }

    @Test
    fun getIgnoredTasks_withReason() = runTest {
        val task = createTaskEntity("1", "Ignored", status = "IGNORED", ignoreReason = "Too busy")
        dao.insertTask(task)
        val ignored = dao.getIgnoredTasks().first()
        assertThat(ignored).hasSize(1)
        assertThat(ignored[0].ignoreReason).isEqualTo("Too busy")
    }

    @Test
    fun deleteTask_removesFromDb() = runTest {
        val task = createTaskEntity("1", "Task 1")
        dao.insertTask(task)
        dao.deleteTask("1")
        val retrieved = dao.getTaskById("1")
        assertThat(retrieved).isNull()
    }

    @Test
    fun getPendingTasks_orderedByDeadlineAscending() = runTest {
        val t1 = createTaskEntity("1", "Later", status = "PENDING", deadlineTimestamp = 3000)
        val t2 = createTaskEntity("2", "Sooner", status = "PENDING", deadlineTimestamp = 1000)
        val t3 = createTaskEntity("3", "Middle", status = "PENDING", deadlineTimestamp = 2000)
        dao.insertTask(t1)
        dao.insertTask(t2)
        dao.insertTask(t3)

        val pending = dao.getPendingTasks().first()
        assertThat(pending[0].id).isEqualTo("2")
        assertThat(pending[1].id).isEqualTo("3")
        assertThat(pending[2].id).isEqualTo("1")
    }

    @Test
    fun flowEmitsUpdate_onInsert() = runTest {
        val allTasksFlow = dao.getAllTasks()
        val firstEmission = allTasksFlow.first()
        assertThat(firstEmission).isEmpty()

        val task = createTaskEntity("1", "Task 1")
        dao.insertTask(task)
        val secondEmission = allTasksFlow.first()
        assertThat(secondEmission).hasSize(1)
    }

    private fun createTaskEntity(
        id: String,
        taskName: String,
        status: String = "PENDING",
        deadlineTimestamp: Long? = null,
        capturedAt: Long = System.currentTimeMillis(),
        ignoreReason: String? = null,
        isOverdue: Boolean = false
    ) = TaskEntity(
        id = id,
        sourceApp = "WhatsApp",
        packageName = "com.whatsapp",
        originalTitle = "Title",
        originalBody = "Body",
        capturedAt = capturedAt,
        extractedTask = taskName,
        importance = "MEDIUM",
        category = "MESSAGE",
        deadline = null,
        deadlineTimestamp = deadlineTimestamp,
        suggestedActions = "[]",
        status = status,
        userDecision = null,
        ignoreReason = ignoreReason,
        scheduledFor = null,
        decidedAt = null,
        completedAt = null,
        requiresEnforcement = false,
        isOverdue = isOverdue,
        rescheduleCount = 0
    )
}
