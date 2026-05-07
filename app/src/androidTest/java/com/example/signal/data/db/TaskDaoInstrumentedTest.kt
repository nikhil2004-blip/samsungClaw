package com.example.signal.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.signal.data.local.AppDatabase
import com.example.signal.data.local.TaskDao
import com.example.signal.data.local.TaskEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class TaskDaoInstrumentedTest {

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
    fun insertAndRead_isSuccessful() = runBlocking<Unit> {
        val task = createTaskEntity("1", "Real DB Test")
        dao.insertTask(task)
        val retrieved = dao.getTaskById("1")
        assertThat(retrieved?.extractedTask).isEqualTo("Real DB Test")
    }

    @Test
    fun updateDynamicPriorities_worksOnRealDb() = runBlocking<Unit> {
        val now = System.currentTimeMillis()
        // Task with deadline in 1 hour -> should become CRITICAL
        val task = createTaskEntity("1", "Soon", deadlineTimestamp = now + 3600000)
        dao.insertTask(task)
        
        dao.updateDynamicPriorities(now)
        
        val updated = dao.getTaskById("1")
        assertThat(updated?.importance).isEqualTo("CRITICAL")
    }

    private fun createTaskEntity(id: String, taskName: String, deadlineTimestamp: Long? = null) = TaskEntity(
        id = id,
        sourceApp = "App",
        packageName = "pkg",
        originalTitle = "Title",
        originalBody = "Body",
        capturedAt = System.currentTimeMillis(),
        extractedTask = taskName,
        importance = "LOW",
        category = "OTHER",
        deadline = null,
        deadlineTimestamp = deadlineTimestamp,
        suggestedActions = "[]",
        status = "PENDING",
        userDecision = null,
        ignoreReason = null,
        scheduledFor = null,
        decidedAt = null,
        completedAt = null,
        requiresEnforcement = false,
        isOverdue = false,
        rescheduleCount = 0
    )
}
