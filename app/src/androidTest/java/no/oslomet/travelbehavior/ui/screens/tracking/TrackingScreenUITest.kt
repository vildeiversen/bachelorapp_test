package no.oslomet.travelbehavior.ui.screens.tracking

import android.Manifest
import android.app.Application
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavController
import androidx.test.rule.GrantPermissionRule
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.firebase.auth.FirebaseAuth
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import no.oslomet.travelbehavior.data.TripDao
import no.oslomet.travelbehavior.location.TrackingService
import no.oslomet.travelbehavior.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Instrumented UI test for [TrackingScreen].
 * Verifies that the UI correctly reacts to different tracking states and user interactions. */

@ExperimentalCoroutinesApi
@OptIn(ExperimentalPermissionsApi::class)
class TrackingScreenUITest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private val mockNavController = mockk<NavController>(relaxed = true)
    private val mockPermissionsState = mockk<MultiplePermissionsState>(relaxed = true)
    private val mockTripDao = mockk<TripDao>(relaxed = true)
    private val mockApplication = mockk<Application>(relaxed = true)
    private val isTrackingFlow = MutableStateFlow(false)

    @Before
    fun setup() {
        // Mocking static Android/Firebase components
        mockkStatic(FirebaseAuth::class)
        every { FirebaseAuth.getInstance() } returns mockk(relaxed = true)

        // Mocking the tracking service state provided to the ViewModel
        mockkObject(TrackingService.Companion)
        every { TrackingService.Companion.isTracking } returns isTrackingFlow
        every { TrackingService.Companion.pathPoints } returns MutableStateFlow(emptyList())
    }

    /** UI-01: Verifies that the "Start Tracking Route" button is displayed when
     * the system is not currently tracking a trip. */
    @Test
    fun whenNotTracking_showsStartButton() {
        // Arrange - Set state to inactive tracking
        isTrackingFlow.value = false
        val viewModel = TrackingViewModel(mockTripDao, mockApplication)

        // Act - Render the tracking screen content
        composeTestRule.setContent {
            TrackingScreenContent(
                viewModel = viewModel,
                navController = mockNavController,
                permissionState = mockPermissionsState
            )
        }

        // Assert - Confirm start button is visible and stop button is gone
        composeTestRule.onNodeWithText("Start Tracking Route").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Stop Tracking").assertCountEquals(0)
    }

    /** UI-02: Verifies that the "Stop Tracking" button is displayed when
     * a tracking session is active. */
    @Test
    fun whenTracking_showsStopButton() {
        // Arrange - Set state to active tracking
        isTrackingFlow.value = true
        val viewModel = TrackingViewModel(mockTripDao, mockApplication)

        // Act - Render the tracking screen content
        composeTestRule.setContent {
            TrackingScreenContent(
                viewModel = viewModel,
                navController = mockNavController,
                permissionState = mockPermissionsState
            )
        }

        // Assert - Confirm stop button is visible and start button is gone
        composeTestRule.onNodeWithText("Stop Tracking").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Start Tracking Route").assertCountEquals(0)
    }

    /** UI-03: Verifies that clicking start and stop buttons triggers the expected
     * ViewModel functions and navigation actions. */
    @Test
    fun startAndStopButtons_triggerCalls() {
        // Arrange - Prepare a ViewModel spy and capture navigation routes
        val viewModel = spyk(TrackingViewModel(mockTripDao, mockApplication))
        every { mockPermissionsState.allPermissionsGranted } returns true
        val routeSlot = slot<String>()
        every { mockNavController.navigate(capture(routeSlot)) } just runs

        // Start Tracking phase
        isTrackingFlow.value = false
        composeTestRule.setContent {
            TrackingScreenContent(
                viewModel = viewModel,
                navController = mockNavController,
                permissionState = mockPermissionsState
            )
        }
        
        // Act 1 - Perform click on start button
        composeTestRule.onNodeWithText("Start Tracking Route").performClick()

        // Verify 1 - Confirm startTracking was called
        verify { viewModel.startTracking() }

        // Stop Tracking phase
        isTrackingFlow.value = true
        val testTripId = "test-trip-123"
        every { viewModel.stopTracking() } returns testTripId

        // Act 2 - Perform click on stop button
        composeTestRule.onNodeWithText("Stop Tracking").performClick()

        // Verify 2 - Confirm stopTracking was called and navigation triggered to correct route
        verify { viewModel.stopTracking() }
        assertEquals(Screen.SaveTrip.createRoute(testTripId), routeSlot.captured)
    }
}
