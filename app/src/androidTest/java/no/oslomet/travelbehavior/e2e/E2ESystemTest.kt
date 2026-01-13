package no.oslomet.travelbehavior.e2e

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.datastore.preferences.core.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import kotlinx.coroutines.runBlocking
import no.oslomet.travelbehavior.MainActivity
import no.oslomet.travelbehavior.data.consentDataStore
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** End-to-End (E2E) tests for the application.
 * Verifies complete user journeys through the app on an Android device. */

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
        // Clear the DataStore before each test to ensure a clean state
        runBlocking {
            InstrumentationRegistry.getInstrumentation().targetContext.consentDataStore.edit { preferences ->
                preferences.clear()
            }
        }
    }

    /** AC-01: Verifies that the consent screen is shown when the app is launched for the first time. */
    @Test
    fun consentScreenIsShownOnFirstLaunch() {
        // Assert that the specific consent text is visible
        composeTestRule.onNodeWithText("This app collects anonymous travel data to support research on travel behaviour. Data is used only for research purposes and is handled securely and anonymously.", substring = true).assertIsDisplayed()
    }

    /** AC-02: Verifies that a user can accept the terms and navigate to the home screen. */
    @Test
    fun userCanAcceptConsent() {
        // Interact with checkbox and accept button
        composeTestRule.onNodeWithTag("agree_checkbox").performClick()
        composeTestRule.onNodeWithText("Accept & Continue").performClick()

        // Wait for and verify navigation to Home
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Home Screen").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Home Screen").assertIsDisplayed()
    }

    /** AC-03: Verifies that declining consent finishes the activity. */
    @Test
    fun userCanDeclineConsent() {
        // Click decline
        composeTestRule.onNodeWithText("Decline").performClick()
        composeTestRule.waitForIdle()

        // Assert that the activity is finishing
        Assert.assertTrue(composeTestRule.activity.isFinishing)
    }

    /** AC-04: Verifies navigation between main screens using the bottom navigation bar. */
    @Test
    fun canNavigateWithBottomBar() {
        // Bypass consent screen
        composeTestRule.onNodeWithTag("agree_checkbox").performClick()
        composeTestRule.onNodeWithText("Accept & Continue").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Home Screen").fetchSemanticsNodes().isNotEmpty()
        }

        // Navigate to Tracking and verify
        composeTestRule.onNodeWithText("Tracking").performClick()
        composeTestRule.onNodeWithText("Start Tracking Route").assertIsDisplayed()

        // Navigate to Settings and verify
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.onNodeWithText("View Consent Settings").assertIsDisplayed()

        // Navigate back to Home and verify
        composeTestRule.onNodeWithText("Home").performClick()
        composeTestRule.onNodeWithText("Home Screen").assertIsDisplayed()
    }

    /** AC-05: Verifies that a user can start and stop a tracking session. */
    @Test
    fun userCanStartAndStopTracking() {
        // Setup: Bypass consent and navigate to tracking
        composeTestRule.onNodeWithTag("agree_checkbox").performClick()
        composeTestRule.onNodeWithText("Accept & Continue").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Home Screen").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Tracking").performClick()

        // Act: Start tracking
        composeTestRule.onNodeWithText("Start Tracking Route").performClick()
        composeTestRule.onNodeWithText("Stop Tracking").assertIsDisplayed()

        // Act: Stop tracking
        composeTestRule.onNodeWithText("Stop Tracking").performClick()

        // Verify: Save Trip screen appears, then delete to cleanup
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Delete Trip").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Delete Trip").performClick()

        // Verify: Return to tracking screen
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Start Tracking Route").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Start Tracking Route").assertIsDisplayed()
    }

    /** AC-06: Verifies that a user can provide feedback and save a trip. */
    @Test
    fun userCanSaveTripWithFeedback() {
        // Setup: Bypass consent and stop a tracking session
        composeTestRule.onNodeWithTag("agree_checkbox").performClick()
        composeTestRule.onNodeWithText("Accept & Continue").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Home Screen").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Tracking").performClick()
        composeTestRule.onNodeWithText("Start Tracking Route").performClick()
        composeTestRule.onNodeWithText("Stop Tracking").performClick()

        // Wait for Save Trip screen
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Save Trip").fetchSemanticsNodes().isNotEmpty()
        }

        // Act: Provide ratings and save
        composeTestRule.onAllNodesWithContentDescription("Star 4")[0].performClick()
        composeTestRule.onAllNodesWithContentDescription("Star 4")[1].performClick()
        composeTestRule.onNodeWithText("Save Trip").performClick()

        // Verify: Return to tracking screen
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Start Tracking Route").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Start Tracking Route").assertIsDisplayed()
    }
}
