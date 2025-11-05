package no.oslomet.travelbehavior

import android.app.Application
import android.content.Intent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.work.WorkManager
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
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
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class TrackingViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    // --- Mocks for Dependencies ---
    // We only need to mock what the ViewModel directly uses or receives.
    private lateinit var application: Application
    private lateinit var tripDao: TripDao // Direct dependency

    // --- Test Subject ---
    private lateinit var viewModel: TrackingViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // 1. Mock the dependencies we will pass into the constructor.
        application = mockk(relaxed = true)
        tripDao = mockk(relaxed = true)

        // 2. Mock static dependencies that are still being used directly.
        //    AppDatabase is no longer needed here.
        mockkStatic(WorkManager::class, FirebaseAuth::class, TrackingService::class)
        mockkObject(TripManager)

        // 3. Define the behavior of mocks.
        every { WorkManager.getInstance(any()) } returns mockk(relaxed = true)
        every { FirebaseAuth.getInstance() } returns mockk(relaxed = true)
        every { TripManager.getTripId(any()) } returns null
        every { TrackingService.Companion.isTracking } returns MutableStateFlow(false)
        every { TrackingService.Companion.pathPoints } returns MutableStateFlow(emptyList<LatLng>())
        every { application.startService(any()) } returns null

        // 4. Initialize the ViewModel with its direct dependencies.
        //    This is now clean and doesn't touch the real AppDatabase.
        viewModel = TrackingViewModel(tripDao, application)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun initial_state_is_correct() = runTest {
        val expectedState = TrackingUiState()
        Assert.assertEquals(expectedState, viewModel.uiState.value)
    }

    @Test
    fun startTracking_updates_state_and_starts_service() = runTest {
        // Arrange
        val intentSlot = slot<Intent>()
        every { application.startService(capture(intentSlot)) } returns null
        every { TripManager.saveTripId(any(), any()) } just runs

        // Act
        viewModel.startTracking()

        // Assert
        Assert.assertEquals(
            TrackingService.Companion.ACTION_START_SERVICE,
            intentSlot.captured.action
        )
        Assert.assertNotNull(viewModel.uiState.value.activeTripId)
        verify { TripManager.saveTripId(application, viewModel.uiState.value.activeTripId!!) }
    }
}
