package com.example.signal.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.signal.MainActivity
import com.example.signal.data.local.TaskEntity
import com.example.signal.data.repository.OnboardingRepositoryInterface
import com.example.signal.data.repository.TaskRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.*
import javax.inject.Inject

@HiltAndroidTest
class DashboardScreenTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var repository: TaskRepository

    @Inject
    lateinit var onboardingRepository: OnboardingRepositoryInterface

    @Before
    fun init() {
        hiltRule.inject()
        runBlocking { 
            repository.clearAll()
            onboardingRepository.setOnboardingCompleted(true)
        }
        
        // Wait for splash to finish and MainNavigation to appear
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("Stats").fetchSemanticsNodes().isNotEmpty()
        }

        // Navigate to Dashboard
        composeTestRule.onNodeWithText("Stats").performClick()

        // Wait for Dashboard content to load
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("Your Week").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun stats_updateWhenTasksAreAdded() {
        runBlocking {
            val now = System.currentTimeMillis()
            repository.insertManualTask(createTaskEntity("1", now, userDecision = "DO_NOW"))
            repository.insertManualTask(createTaskEntity("2", now, userDecision = "DO_NOW"))
            repository.insertManualTask(createTaskEntity("3", now, userDecision = "SCHEDULE"))
        }
        
        // Click Refresh
        composeTestRule.onNodeWithContentDescription("Refresh").performClick()
        
        // Verify stats
        composeTestRule.onNodeWithText("2").assertIsDisplayed() // Actioned
        composeTestRule.onNodeWithText("1").assertIsDisplayed() // Scheduled
        composeTestRule.onNodeWithText("3").assertIsDisplayed() // Captured (Total)
    }

    @Test
    fun overdueWarning_showsWhenOverdueTasksExist() {
        runBlocking {
            val past = System.currentTimeMillis() - 86400000
            repository.insertManualTask(createTaskEntity("1", past, isOverdue = true))
        }
        
        composeTestRule.onNodeWithContentDescription("Refresh").performClick()
        
        composeTestRule.onNodeWithText("1 Overdue Task(s)", substring = true).assertIsDisplayed()
    }

    @Test
    fun streak_updatesCorrectlty() {
        // Wait for any streak text to appear
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("Day Streak", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        
        runBlocking {
            repository.insertManualTask(createTaskEntity("1", System.currentTimeMillis(), userDecision = "IGNORE", importance = "CRITICAL"))
        }
        
        composeTestRule.onNodeWithContentDescription("Refresh").performClick()
        
        // Wait for streak to update to 0
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("0 Day Streak", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        
        composeTestRule.onNodeWithText("0 Day Streak", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun aiInsight_changesBasedOnPerformance() {
        runBlocking {
            // High ignoring today
            repository.insertManualTask(createTaskEntity("1", System.currentTimeMillis(), userDecision = "IGNORE"))
            repository.insertManualTask(createTaskEntity("2", System.currentTimeMillis(), userDecision = "IGNORE"))
        }
        
        composeTestRule.onNodeWithContentDescription("Refresh").performClick()
        
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("You're deferring more than acting today", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        
        composeTestRule.onNodeWithText("You're deferring more than acting today", substring = true).performScrollTo().assertIsDisplayed()
    }

    private fun createTaskEntity(
        id: String,
        capturedAt: Long,
        userDecision: String? = null,
        importance: String = "MEDIUM",
        isOverdue: Boolean = false
    ) = TaskEntity(
        id = id,
        sourceApp = "WhatsApp",
        packageName = "com.whatsapp",
        originalTitle = "Title",
        originalBody = "Body",
        capturedAt = capturedAt,
        extractedTask = "Task",
        importance = importance,
        category = "MESSAGE",
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
        isOverdue = isOverdue,
        rescheduleCount = 0
    )
}
