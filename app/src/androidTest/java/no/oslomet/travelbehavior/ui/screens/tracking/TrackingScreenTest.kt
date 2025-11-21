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

@ExperimentalCoroutinesApi
@OptIn(ExperimentalPermissionsApi::class)
class TrackingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    // Mocks for the Composables and ViewModel
    private val mockNavController = mockk<NavController>(relaxed = true)
    private val mockPermissionsState = mockk<MultiplePermissionsState>(relaxed = true)
    private val mockTripDao = mockk<TripDao>(relaxed = true)
    private val mockApplication = mockk<Application>(relaxed = true)

    // Fake flows to control state observed by the ViewModel
    private val isTrackingFlow = MutableStateFlow(false)

    @Before
    fun setup() {
        // Firebase anonymous login used in VM init
        mockkStatic(FirebaseAuth::class)
        every { FirebaseAuth.getInstance() } returns mockk(relaxed = true)

        // Provide fake service state to the VM
        mockkObject(TrackingService.Companion)
        every { TrackingService.isTracking } returns isTrackingFlow
        every { TrackingService.pathPoints } returns MutableStateFlow(emptyList())
    }

    @Test
    fun whenNotTracking_showsStartButton() {
        // Arrange
        isTrackingFlow.value = false
        val viewModel = TrackingViewModel(mockTripDao, mockApplication)

        // Act
        composeTestRule.setContent {
            TrackingScreenContent(
                viewModel = viewModel,
                navController = mockNavController,
                permissionState = mockPermissionsState
            )
        }

        // Assert
        composeTestRule.onNodeWithText("Start Tracking Route").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Stop Tracking").assertCountEquals(0)
    }

    @Test
    fun whenTracking_showsStopButton() {
        // Arrange
        isTrackingFlow.value = true
        val viewModel = TrackingViewModel(mockTripDao, mockApplication)

        // Act
        composeTestRule.setContent {
            TrackingScreenContent(
                viewModel = viewModel,
                navController = mockNavController,
                permissionState = mockPermissionsState
            )
        }

        // Assert
        composeTestRule.onNodeWithText("Stop Tracking").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Start Tracking Route").assertCountEquals(0)
    }

    @Test
    fun startAndStopButtons_triggerCorrectViewModelAndNavigationCalls() {
        // ARRANGE
        // Use a spy to both execute the real VM logic and verify calls to it
        val viewModel = spyk(TrackingViewModel(mockTripDao, mockApplication))
        every { mockPermissionsState.allPermissionsGranted } returns true

        val routeSlot = slot<String>()
        every { mockNavController.navigate(capture(routeSlot)) } just runs

        // ACT 1: Start Tracking
        isTrackingFlow.value = false // Ensure we are in the "not tracking" state
        composeTestRule.setContent {
            TrackingScreenContent(
                viewModel = viewModel,
                navController = mockNavController,
                permissionState = mockPermissionsState
            )
        }
        composeTestRule.onNodeWithText("Start Tracking Route").performClick()

        // VERIFY 1
        verify { viewModel.startTracking() }

        // ACT 2: Stop Tracking
        // Simulate the state change that would occur when tracking starts
        isTrackingFlow.value = true
        // Mock the return value of stopTracking before it is called
        val testTripId = "test-trip-123"
        every { viewModel.stopTracking() } returns testTripId

        composeTestRule.onNodeWithText("Stop Tracking").performClick()

        // VERIFY 2
        verify { viewModel.stopTracking() }
        assertEquals(Screen.SaveTrip.createRoute(testTripId), routeSlot.captured)
    }
}
