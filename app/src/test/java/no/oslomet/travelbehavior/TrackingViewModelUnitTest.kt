package no.oslomet.travelbehavior

import android.app.Application
import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import no.oslomet.travelbehavior.data.TripDao
import no.oslomet.travelbehavior.data.TripManager
import no.oslomet.travelbehavior.ui.screens.tracking.TrackingUiState
import no.oslomet.travelbehavior.ui.screens.tracking.TrackingViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Local unit test for the TrackingViewModel.
 *
 * This test class verifies the logic of the [TrackingViewModel] in isolation,
 * using mocks for its dependencies. It runs on the local JVM and does not require
 * an Android device or emulator.
 */
@ExperimentalCoroutinesApi
class TrackingViewModelUnitTest {

    // This rule swaps the background executor used by the Architecture Components with a different one which executes each task synchronously.
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    // A test dispatcher for controlling coroutines execution in tests.
    private val testDispatcher = StandardTestDispatcher()

    // Mocks for the ViewModel's dependencies.
    private lateinit var tripDao: TripDao
    private lateinit var application: Application

    // The ViewModel under test.
    private lateinit var viewModel: TrackingViewModel

    @Before
    fun setup() {
        // Sets the main coroutines dispatcher to our test dispatcher.
        Dispatchers.setMain(testDispatcher)

        // Mock static Android framework calls before the ViewModel is initialized
        mockkStatic(WorkManager::class, FirebaseAuth::class, Log::class)
        mockkObject(TripManager) // Mock TripManager
        every { WorkManager.getInstance(any()) } returns mockk(relaxed = true)
        every { FirebaseAuth.getInstance() } returns mockk(relaxed = true)
        every { Log.d(any(), any()) } returns 0 // Mock Log.d to prevent crash
        every { TripManager.getTripId(any()) } returns null // Ensure it returns null initially

        // Create relaxed mocks for the other dependencies.
        tripDao = mockk(relaxed = true)
        application = mockk(relaxed = true)

        // Initialize the ViewModel with the mocked dependencies.
        viewModel = TrackingViewModel(tripDao, application)
    }

    @After
    fun tearDown() {
        // Resets the main dispatcher to the original one.
        Dispatchers.resetMain()
        // Clears all mocks.
        unmockkAll()
    }

    @Test
    fun `initial uiState should be the default state`() = runTest {
        // Arrange: The expected initial state.
        val expectedState = TrackingUiState()

        // Act: Get the current state from the ViewModel.
        val actualState = viewModel.uiState.value

        // Assert: The actual state should match the expected initial state.
        assertEquals(expectedState, actualState)
    }

    // You can add more tests here. For example:
    // - A test to verify that calling startTracking() updates the uiState correctly.
    // - A test to verify that the correct trip is loaded from the dao.
}
