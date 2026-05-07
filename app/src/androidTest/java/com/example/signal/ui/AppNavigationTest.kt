package com.example.signal.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.signal.MainActivity
import com.example.signal.data.repository.OnboardingRepositoryInterface
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class AppNavigationTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var onboardingRepository: OnboardingRepositoryInterface

    @Before
    fun init() {
        hiltRule.inject()
        runBlocking { onboardingRepository.setOnboardingCompleted(true) }

        // Wait for main UI
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("Inbox").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun bottomNavigation_switchesScreens() {
        // Start on Tasks
        composeTestRule.onNodeWithText("Inbox").assertIsDisplayed()

        // Go to Stats
        composeTestRule.onNodeWithText("Stats").performClick()
        composeTestRule.onNodeWithText("Your Week").assertIsDisplayed()

        // Go to Settings
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.onNodeWithText("Permissions").assertIsDisplayed()

        // Back to Tasks
        composeTestRule.onNodeWithText("Tasks").performClick()
        composeTestRule.onNodeWithText("Inbox").assertIsDisplayed()
    }
}
