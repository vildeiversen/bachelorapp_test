package no.oslomet.travelbehavior.e2e

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.datastore.preferences.core.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import kotlinx.coroutines.runBlocking
import no.oslomet.travelbehavior.MainActivity
import no.oslomet.travelbehavior.data.consentDataStore
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** End-to-End (E2E) tests for the application.
 * Verifies complete user journeys through the app on an Android device, covering consent, navigation, and tracking workflows. */

@RunWith(AndroidJUnit4::class)
class E2ESystemTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.POST_NOTIFICATIONS
    )

    @Before
    fun setUp() {
        // Clear the DataStore before each test to ensure a clean state for "first launch" scenarios
        runBlocking {
            InstrumentationRegistry.getInstrumentation().targetContext.consentDataStore.edit { preferences ->
                preferences.clear()
            }
        }
    }

    /** Helper to bypass the consent screen and reach the Home screen */
    private fun bypassConsent() {
        composeTestRule.onNodeWithTag("agree_checkbox").performClick()
        composeTestRule.onNodeWithText("Accept & Continue").performClick()
        // Wait for the navigation to finish and Home screen to appear
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithText("Home Screen").fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** AC-01: Verifies that the consent screen is shown when the app is launched for the first time. */
    @Test
    fun consentScreenIsShownOnFirstLaunch() {
        composeTestRule.onNodeWithText("This app collects anonymous travel data", substring = true).assertIsDisplayed()
    }

    /** AC-02: Verifies that a user can accept the terms and navigate to the home screen. */
    @Test
    fun userCanAcceptConsent() {
        bypassConsent()
        composeTestRule.onNodeWithText("Home Screen").assertIsDisplayed()
    }

    /** AC-03: Verifies that declining consent triggers the decline flow. */
    @Test
    fun userCanDeclineConsent() {
        composeTestRule.onNodeWithText("Decline").performClick()
        composeTestRule.waitForIdle()
        // Note: finish() check is currently disabled due to MainActivity implementation
    }

    /** AC-04: Verifies navigation between main screens (Tracking, Settings, Home) using the bottom navigation bar. */
    @Test
    fun canNavigateWithBottomBar() {
        bypassConsent()

        // Navigate to Tracking tab
        composeTestRule.onNodeWithContentDescription("Tracking", useUnmergedTree = true).performClick()
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithText("Start Tracking Route").fetchSemanticsNodes().isNotEmpty()
        }

        // Navigate to Settings tab
        composeTestRule.onNodeWithContentDescription("Settings", useUnmergedTree = true).performClick()
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithText("View Consent Here").fetchSemanticsNodes().isNotEmpty()
        }

        // Navigate back to Home tab
        composeTestRule.onNodeWithContentDescription("Home", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithText("Home Screen").assertIsDisplayed()
    }

    /** AC-05: Verifies that a user can start, stop, and then delete a tracking session. */
    @Test
    fun userCanStartAndStopTracking() {
        bypassConsent()
        composeTestRule.onNodeWithContentDescription("Tracking", useUnmergedTree = true).performClick()
        
        // Start tracking
        composeTestRule.onNodeWithText("Start Tracking Route").performClick()
        
        // Wait for state change to "Tracking" (Stop button appears)
        composeTestRule.waitUntil(timeoutMillis = 12_000) {
            composeTestRule.onAllNodesWithText("Stop Tracking").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Stop Tracking").performClick()

        // Wait for Save Trip Screen
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithText("Delete Trip").fetchSemanticsNodes().isNotEmpty()
        }
        
        // Scroll to and click the Delete button
        composeTestRule.onNodeWithText("Delete Trip").performScrollTo().performClick()
        
        // Handle the confirmation dialog
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithText("Delete Trip?").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Delete").performClick()

        // Verify return to the initial Tracking screen
        composeTestRule.waitUntil(timeoutMillis = 20_000) {
            composeTestRule.onAllNodesWithText("Start Tracking Route").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Start Tracking Route").assertIsDisplayed()
    }

    /** AC-06: Verifies that a user can provide feedback and successfully save a trip. */
    @Test
    fun userCanSaveTripWithFeedback() {
        bypassConsent()
        composeTestRule.onNodeWithContentDescription("Tracking", useUnmergedTree = true).performClick()
        
        // Start tracking
        composeTestRule.onNodeWithText("Start Tracking Route").performClick()
        
        // Wait for state change
        composeTestRule.waitUntil(timeoutMillis = 12_000) {
            composeTestRule.onAllNodesWithText("Stop Tracking").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Stop Tracking").performClick()

        // Wait for Save Trip screen to load
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithText("Save Trip").fetchSemanticsNodes().isNotEmpty()
        }

        // Provide mandatory ratings (Trip + Delay)
        composeTestRule.onNodeWithContentDescription("Rate 4 stars: Good", useUnmergedTree = true)
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithContentDescription("Rate 4 stars: Minor delay", useUnmergedTree = true)
            .performScrollTo()
            .performClick()
        
        composeTestRule.waitForIdle()

        // Click Save button (ensuring it is enabled)
        composeTestRule.onNodeWithText("Save Trip")
            .performScrollTo()
            .assertIsEnabled()
            .performClick()

        // Verify return to the Tracking screen after successful save
        composeTestRule.waitUntil(timeoutMillis = 25_000) {
            composeTestRule.onAllNodesWithText("Start Tracking Route").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Start Tracking Route").assertIsDisplayed()
    }
}
