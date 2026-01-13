package no.oslomet.travelbehavior.ui.screens.consent

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [ConsentViewModel].
 * Uses a [FakeConsentRepository] to isolate the ViewModel's logic from persistent storage.
 */
@ExperimentalCoroutinesApi
class ConsentViewModelUnitTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** UT-06: Verifies that the initial state is correctly set to loading and not agreed. */
    @Test
    fun initialStateIsLoadingAndNotAgreed() = runTest {
        // Initialize with a repository state where no data is loaded yet
        val repo = FakeConsentRepository(startGiven = false, startVersion = 0)
        val viewModel = ConsentViewModel(repo)
        val initialState = viewModel.ui.value

        // Check if loading is active and agreement is false
        assertEquals(true, initialState.isLoading)
        assertEquals(false, initialState.agreeChecked)
    }

    /** UT-07: Verifies that consent is required if it has not been accepted yet. */
    @Test
    fun consentIsRequiredIfNotAcceptedYet() = runTest {
        // Setup repository for a new user
        val repo = FakeConsentRepository(startGiven = false, startVersion = 0)
        val viewModel = ConsentViewModel(repo)

        // Advance dispatcher to allow the ViewModel to process repo data
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify that consent is now required and loading finished
        val finalState = viewModel.ui.value
        assertEquals(true, finalState.consentRequired)
        assertEquals(false, finalState.isLoading)
    }

    /** UT-08: Verifies that consent is NOT required if it was already accepted. */
    @Test
    fun consentIsNotRequiredIfAlreadyAccepted() = runTest {
        // Setup repository for a user who already accepted current version
        val repo = FakeConsentRepository(
            startGiven = true,
            startVersion = ConsentViewModel.CURRENT_CONSENT_VERSION
        )
        val viewModel = ConsentViewModel(repo)

        // Process initial data load
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify that consent is not requested
        val finalState = viewModel.ui.value
        assertFalse(finalState.consentRequired)
        assertFalse(finalState.isLoading)
    }

    /** UT-09: Verifies that the checkbox state in the UI reflects calls to setAgree(). */
    @Test
    fun checkboxStateReflectsSetAgree() = runTest {
        // Initialize ViewModel
        val repo = FakeConsentRepository(startGiven = false, startVersion = 0)
        val viewModel = ConsentViewModel(repo)

        // Assert initial unchecked state
        assertFalse(viewModel.ui.value.agreeChecked)

        // Simulate user checking the box
        viewModel.setAgree(true)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify the state updated
        assertEquals(true, viewModel.ui.value.agreeChecked)
    }

    /** UT-10: Verifies that accepting saves the consent and triggers the onDone callback. */
    @Test
    fun acceptSavesConsentAndCallsOnDone() = runTest {
        // Setup for a new user
        val repo = FakeConsentRepository(startGiven = false, startVersion = 0)
        val viewModel = ConsentViewModel(repo)
        var onDoneCalled = false

        // User must check the agreement box before they can accept
        viewModel.setAgree(true)
        testDispatcher.scheduler.advanceUntilIdle()

        // Click the accept button
        viewModel.accept { onDoneCalled = true }
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify navigation callback was triggered and consent status updated
        assertEquals(true, onDoneCalled)
        assertEquals(false, viewModel.ui.value.consentRequired)
    }

    /** UT-11: Verifies that the accept action does nothing if the agreement box is not checked. */
    @Test
    fun acceptDoesNothingIfNotAgreed() = runTest {
        // Setup for a user who hasn't checked the box
        val repo = FakeConsentRepository(startGiven = false, startVersion = 0)
        val viewModel = ConsentViewModel(repo)
        var onDoneCalled = false

        // Attempt to accept without checking the agreement box
        viewModel.accept { onDoneCalled = true }
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify that the navigation callback was NOT called
        assertFalse(onDoneCalled)
    }
}
