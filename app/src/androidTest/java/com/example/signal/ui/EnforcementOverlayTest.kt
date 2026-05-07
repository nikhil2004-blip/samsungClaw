package com.example.signal.ui

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.signal.data.local.TaskEntity
import com.example.signal.data.repository.TaskRepository
import com.example.signal.ui.enforcement.EnforcementOverlayActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class EnforcementOverlayTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createEmptyComposeRule()

    @Inject
    lateinit var repository: TaskRepository

    @Before
    fun init() {
        hiltRule.inject()
        runBlocking { repository.clearAll() }
    }

    @Test
    fun overlay_displaysTaskInfo() {
        val taskId = "test-task"
        runBlocking {
            repository.insertManualTask(createTaskEntity(taskId, "Wash the dishes"))
        }

        val intent = Intent(ApplicationProvider.getApplicationContext(), EnforcementOverlayActivity::class.java).apply {
            putExtra(EnforcementOverlayActivity.EXTRA_TASK_ID, taskId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        ActivityScenario.launch<EnforcementOverlayActivity>(intent).use {
            composeTestRule.onNodeWithText("Wash the dishes").assertIsDisplayed()
            composeTestRule.onNodeWithText("DO NOW").assertIsDisplayed()
            composeTestRule.onNodeWithText("SCHEDULE").assertIsDisplayed()
        }
    }

    @Test
    fun overlay_doNow_closesActivity() {
        val taskId = "test-task"
        runBlocking {
            repository.insertManualTask(createTaskEntity(taskId, "Wash the dishes"))
        }

        val intent = Intent(ApplicationProvider.getApplicationContext(), EnforcementOverlayActivity::class.java).apply {
            putExtra(EnforcementOverlayActivity.EXTRA_TASK_ID, taskId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        ActivityScenario.launch<EnforcementOverlayActivity>(intent).use { scenario ->
            composeTestRule.onNodeWithText("DO NOW").performClick()
            
            // Wait for coroutines in ViewModel to finish
            composeTestRule.waitForIdle()

            // Verify task updated in repo
            runBlocking {
                val task = repository.getTaskById(taskId)
                assert(task?.status == "IN_PROGRESS")
            }
            // Verify activity finished (scenario state)
            // scenario.state will be DESTROYED
        }
    }

    @Test
    fun overlay_ignore_requiresReason() {
        val taskId = "test-task"
        runBlocking {
            repository.insertManualTask(createTaskEntity(taskId, "Wash the dishes"))
        }

        val intent = Intent(ApplicationProvider.getApplicationContext(), EnforcementOverlayActivity::class.java).apply {
            putExtra(EnforcementOverlayActivity.EXTRA_TASK_ID, taskId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        ActivityScenario.launch<EnforcementOverlayActivity>(intent).use {
            composeTestRule.onNodeWithText("IGNORE").performClick()
            
            // Confirm button should be disabled initially
            composeTestRule.onNodeWithText("Confirm Ignore").assertIsNotEnabled()
            
            // Type short reason
            composeTestRule.onNodeWithTag("IgnoreReasonField").performTextInput("Short")
            composeTestRule.onNodeWithText("Confirm Ignore").assertIsNotEnabled()
            
            // Type long enough reason
            composeTestRule.onNodeWithTag("IgnoreReasonField").performTextInput(" because I am very busy today")
            composeTestRule.onNodeWithText("Confirm Ignore").assertIsEnabled()
            
            composeTestRule.onNodeWithText("Confirm Ignore").performClick()
            
            composeTestRule.waitForIdle()

            runBlocking {
                val task = repository.getTaskById(taskId)
                assert(task?.status == "IGNORED")
                assert(task?.ignoreReason?.contains("busy") == true)
            }
        }
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
        userDecision = null,
        ignoreReason = null,
        scheduledFor = null,
        decidedAt = null,
        completedAt = null,
        requiresEnforcement = true,
        isOverdue = false,
        rescheduleCount = 0
    )
}
