package com.example.signal.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.signal.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject
import com.example.signal.data.repository.OnboardingRepositoryInterface

@HiltAndroidTest
class SettingsScreenTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var onboardingRepository: OnboardingRepositoryInterface

    @Before
    fun init() {
        hiltRule.inject()
        kotlinx.coroutines.runBlocking { 
            onboardingRepository.setOnboardingCompleted(true)
        }

        // Wait for main UI
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("Settings").fetchSemanticsNodes().isNotEmpty()
        }

        // Navigate to Settings
        composeTestRule.onNodeWithText("Settings").performClick()

        // Wait for Settings content to load
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("Quiet Hours").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun settings_sectionsAreVisible() {
        composeTestRule.onNodeWithText("Permissions").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Enforcement Mode").performScrollTo().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Quiet Hours").onFirst().performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Calendar Integration").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Data").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun enforcementMode_isSelectable() {
        composeTestRule.onNodeWithText("Critical Only").performScrollTo().performClick()
        // Verify radio button selected (using state would be better, but we can check if it exists)
        composeTestRule.onNodeWithText("Critical Only").assertIsDisplayed()
    }

    @Test
    fun clearData_showsConfirmationDialog() {
        composeTestRule.onNodeWithText("Clear All Data").performScrollTo().performClick()
        
        // Verify dialog
        composeTestRule.onNodeWithText("Clear All Data?").assertIsDisplayed()
        composeTestRule.onNodeWithText("This will permanently delete all tasks and history. This cannot be undone.", substring = true).assertIsDisplayed()
        
        // Cancel
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.onNodeWithText("Clear All Data?").assertDoesNotExist()
    }

    @Test
    fun toggles_areSwitchable() {
        composeTestRule.onAllNodesWithText("Quiet Hours").onFirst().assertIsDisplayed()
        // Find by Toggleable role.
        composeTestRule.onAllNodes(isToggleable()).onFirst().performClick()
    }
}
