package com.example.signal.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.signal.MainActivity
import com.example.signal.data.local.TaskEntity
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
class TaskBoardScreenTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var repository: TaskRepository

    @Before
    fun init() {
        hiltRule.inject()
        runBlocking { repository.clearAll() }

        // Wait for main UI
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("Inbox").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun screen_initiallyShowsEmptyState() {
        composeTestRule.onNodeWithText("You're all caught up!").assertIsDisplayed()
    }

    @Test
    fun addTask_manually_showsInList() {
        // Open Add Dialog
        composeTestRule.onNodeWithContentDescription("Add task").performClick()
        
        // Fill details
        composeTestRule.onNodeWithText("Task description").performTextInput("Buy Milk")
        composeTestRule.onNodeWithText("Add").performClick()
        
        // Verify in list
        composeTestRule.onNodeWithText("Buy Milk").assertIsDisplayed()
    }

    @Test
    fun filterChips_areClickable_andFilterContent() {
        runBlocking {
            repository.insertManualTask(createTaskEntity("1", "Buy Milk", "MESSAGE"))
            repository.insertManualTask(createTaskEntity("2", "Pay Rent", "DEADLINE"))
        }
        
        // Wait for UI to update
        composeTestRule.waitForIdle()

        // Initially both might be visible (All tab)
        composeTestRule.onNodeWithText("Buy Milk").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pay Rent").assertIsDisplayed()

        // Click Deadlines filter
        composeTestRule.onNodeWithText("Deadlines", substring = true).performClick()

        // Verify only Pay Rent is visible
        composeTestRule.onNodeWithText("Pay Rent").assertIsDisplayed()
        composeTestRule.onNodeWithText("Buy Milk").assertDoesNotExist()
    }

    @Test
    fun searchBar_filtersList() {
        runBlocking {
            repository.insertManualTask(createTaskEntity("1", "Buy Milk"))
            repository.insertManualTask(createTaskEntity("2", "Buy Bread"))
        }
        
        composeTestRule.onNodeWithText("Search...").performTextInput("Milk")
        
        composeTestRule.onNodeWithText("Buy Milk").assertIsDisplayed()
        composeTestRule.onNodeWithText("Buy Bread").assertDoesNotExist()
    }

    @Test
    fun expandingTask_showsDetails_andMarkDone() {
        runBlocking {
            repository.insertManualTask(createTaskEntity("1", "Submit Report"))
        }
        
        // Tap to expand
        composeTestRule.onNodeWithText("Submit Report").performClick()
        
        // Check if details visible (original body is "Body" in helper)
        composeTestRule.onNodeWithText("Original notification:").assertIsDisplayed()
        
        // Mark as Done
        composeTestRule.onNodeWithText("Mark as Done ✅").performClick()
        
        // Verify it moves to completed today section (alpha change or text decoration)
        // Or check if it shows "COMPLETED TODAY" header
        composeTestRule.onNodeWithText("✅ COMPLETED TODAY", substring = true).assertIsDisplayed()
    }

    private fun createTaskEntity(id: String, taskName: String, category: String = "MESSAGE") = TaskEntity(
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
        deadlineTimestamp = null,
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
