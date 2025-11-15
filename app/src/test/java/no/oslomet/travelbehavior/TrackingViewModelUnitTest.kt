package no.oslomet.travelbehavior

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
import no.oslomet.travelbehavior.ui.screens.tracking.TrackingUiState
import no.oslomet.travelbehavior.ui.screens.tracking.TrackingViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

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
        every { TrackingService.isTracking } returns isTrackingFlow
        every { TrackingService.pathPoints } returns pathPointsFlow

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

    @Test
    fun `initial uiState should be the default state`() = runTest {
        val expectedState = TrackingUiState()
        val actualState = viewModel.uiState.value
        assertEquals(expectedState, actualState)
    }

    @Test
    fun `startTracking should update state and save trip id`() = runTest {
        every { TripManager.saveTripId(any(), any()) } just runs
        every { application.startService(any()) } returns mockk()

        viewModel.startTracking()
        testDispatcher.scheduler.advanceUntilIdle()

        val newTripId = viewModel.uiState.value.activeTripId
        val nonNullTripId = requireNotNull(newTripId)

        // Verify we saved the trip id
        verify { TripManager.saveTripId(application, nonNullTripId) }

        // Verify that a service start was requested (we don't care about the Intent details here)
        verify { application.startService(any()) }
    }

    @Test
    fun `stopTracking should return trip id and send stop action to service`() = runTest {
        val activeTripId = "test-trip-123"

        // When stopTracking asks TripManager for the id, return this value
        every { TripManager.getTripId(any()) } returns activeTripId

        // We don't care about the real Intent, just that startService is called
        every { application.startService(any()) } returns mockk()

        // Call the function under test
        val returnedId = viewModel.stopTracking()
        testDispatcher.scheduler.advanceUntilIdle()

        // 1) It should return the trip id
        assertEquals(activeTripId, returnedId)

        // 2) It should read the id from TripManager
        verify { TripManager.getTripId(context = application) }

        // 3) It should request starting the service (with the stop action inside)
        verify { application.startService(any()) }
    }

    @Test
    fun `viewModel should restore active trip on init`() = runTest {
        val activeTripId = "test-trip-123"
        every { TripManager.getTripId(any()) } returns activeTripId

        val newViewModel = TrackingViewModel(tripDao, application)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(activeTripId, newViewModel.uiState.value.activeTripId)
    }

    @Test
    fun `uiState should update when TrackingService state changes`() = runTest {
        assertEquals(false, viewModel.uiState.value.isTracking)
        assertEquals(emptyList<LatLng>(), viewModel.uiState.value.pathPoints)

        isTrackingFlow.value = true
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(true, viewModel.uiState.value.isTracking)

        val testPoints = listOf(LatLng(1.0, 1.0), LatLng(2.0, 2.0))
        pathPointsFlow.value = testPoints
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(testPoints, viewModel.uiState.value.pathPoints)
    }
}

