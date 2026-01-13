package no.oslomet.travelbehavior.ui.screens.tracking

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.work.WorkManager
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import no.oslomet.travelbehavior.data.TripDao
import no.oslomet.travelbehavior.data.TripManager
import no.oslomet.travelbehavior.location.TrackingService
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Unit tests for the [TrackingViewModel].
 * These tests run locally on the JVM and use MockK to isolate the ViewModel from Android dependencies. */

@ExperimentalCoroutinesApi
class TrackingViewModelUnitTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var tripDao: TripDao
    private lateinit var application: Application
    private lateinit var viewModel: TrackingViewModel

    private lateinit var isTrackingFlow: MutableStateFlow<Boolean>
    private lateinit var pathPointsFlow: MutableStateFlow<List<LatLng>>

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        isTrackingFlow = MutableStateFlow(false)
        pathPointsFlow = MutableStateFlow(emptyList())

        // Mock static Java methods that would otherwise hit Android / network
        mockkStatic(WorkManager::class, FirebaseAuth::class, Log::class)
        every { WorkManager.getInstance(any()) } returns mockk(relaxed = true)
        every { FirebaseAuth.getInstance() } returns mockk(relaxed = true)
        every { Log.d(any(), any()) } returns 0

        // Mock Kotlin singletons / objects
        mockkObject(TripManager)
        every { TripManager.getTripId(any()) } returns null

        // Mock the companion object flows in TrackingService
        mockkObject(TrackingService.Companion)
        every { TrackingService.Companion.isTracking } returns isTrackingFlow
        every { TrackingService.Companion.pathPoints } returns pathPointsFlow

        // Mock Intent constructor so real Android setAction() is never called
        mockkConstructor(Intent::class)
        every { anyConstructed<Intent>().setAction(any()) } returns mockk(relaxed = true)

        // Relaxed mocks for other dependencies
        tripDao = mockk(relaxed = true)
        application = mockk(relaxed = true)

        // ViewModel with mocked dependencies
        viewModel = TrackingViewModel(tripDao, application)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    /** UT-01: Verifies that the initial state of the ViewModel is correct. */
    @Test
    fun initialStateIsDefault() = runTest {
        // Compare current state with a default TrackingUiState object
        val expectedState = TrackingUiState()
        val actualState = viewModel.uiState.value
        Assert.assertEquals(expectedState, actualState)
    }

    /** UT-02: Verifies that starting tracking correctly updates the UI state,
     * saves the generated trip ID, and triggers the tracking service.*/
    @Test
    fun startTrackingUpdatesStateAndSavesTripId() = runTest {
        // Prepare mocks for service start and ID storage
        every { TripManager.saveTripId(any(), any()) } just runs
        every { application.startService(any()) } returns mockk()

        // Trigger the start action
        viewModel.startTracking()
        testDispatcher.scheduler.advanceUntilIdle()

        // Capture the generated trip ID from the state
        val newTripId = viewModel.uiState.value.activeTripId
        val nonNullTripId = requireNotNull(newTripId)

        // Verify the ID was persisted and the service was called
        verify { TripManager.saveTripId(application, nonNullTripId) }
        verify { application.startService(any()) }
    }

    /** UT-03: Verifies that stopping tracking returns the correct trip ID
     * and sends the stop action to the tracking service. */
    @Test
    fun stopTrackingReturnsTripIdAndSendsStopAction() = runTest {
        // Arrange an active trip ID
        val activeTripId = "test-trip-123"
        every { TripManager.getTripId(any()) } returns activeTripId
        every { application.startService(any()) } returns mockk()

        // Execute stop tracking
        val returnedId = viewModel.stopTracking()
        testDispatcher.scheduler.advanceUntilIdle()

        // Check if the returned ID matches and service intent was sent
        Assert.assertEquals(activeTripId, returnedId)
        verify { TripManager.getTripId(context = application) }
        verify { application.startService(any()) }
    }

    /** UT-04: Verifies that the ViewModel correctly restores an ongoing trip
     * from [TripManager] during initialization. */
    @Test
    fun viewModelRestoresActiveTripOnInit() = runTest {
        // Setup TripManager to return an existing ID
        val activeTripId = "test-trip-123"
        every { TripManager.getTripId(any()) } returns activeTripId

        // Create a new instance of the ViewModel
        val newViewModel = TrackingViewModel(tripDao, application)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify the state reflects the restored ID
        Assert.assertEquals(activeTripId, newViewModel.uiState.value.activeTripId)
    }

    /** UT-05: Verifies that the ViewModel's UI state reactively updates
     * when the [TrackingService] state (tracking status and path points) changes. */
    @Test
    fun uiStateUpdatesWhenTrackingServiceStateChanges() = runTest {
        // Check baseline state
        Assert.assertEquals(false, viewModel.uiState.value.isTracking)
        Assert.assertEquals(emptyList<LatLng>(), viewModel.uiState.value.pathPoints)

        // Simulate service starting tracking
        isTrackingFlow.value = true
        testDispatcher.scheduler.advanceUntilIdle()
        Assert.assertEquals(true, viewModel.uiState.value.isTracking)

        // Simulate arrival of new location points
        val testPoints = listOf(LatLng(1.0, 1.0), LatLng(2.0, 2.0))
        pathPointsFlow.value = testPoints
        testDispatcher.scheduler.advanceUntilIdle()
        Assert.assertEquals(testPoints, viewModel.uiState.value.pathPoints)
    }
}
