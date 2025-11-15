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


@ExperimentalCoroutinesApi
class ConsentViewModelTest {

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

    @Test
    fun `initial state is loading and not agreed`() = runTest {
        val repo = FakeConsentRepository(startGiven = false, startVersion = 0)
        val viewModel = ConsentViewModel(repo)
        val initialState = viewModel.ui.value

        assertEquals(true, initialState.isLoading)
        assertEquals(false, initialState.agreeChecked)
    }

    @Test
    fun `consent is required if not accepted yet`() = runTest {
        val repo = FakeConsentRepository(startGiven = false, startVersion = 0)
        val viewModel = ConsentViewModel(repo)

        // Let the ViewModel process the initial state from the repo
        testDispatcher.scheduler.advanceUntilIdle()

        val finalState = viewModel.ui.value
        assertEquals(true, finalState.consentRequired)
        assertEquals(false, finalState.isLoading)
    }

    @Test
    fun `consent is not required if already accepted`() = runTest {
        val repo = FakeConsentRepository(
            startGiven = true,
            startVersion = ConsentViewModel.CURRENT_CONSENT_VERSION
        )
        val viewModel = ConsentViewModel(repo)

        testDispatcher.scheduler.advanceUntilIdle()

        val finalState = viewModel.ui.value
        assertFalse(finalState.consentRequired)
        assertFalse(finalState.isLoading)
    }

    @Test
    fun `checkbox state reflects setAgree`() = runTest {
        val repo = FakeConsentRepository(startGiven = false, startVersion = 0)
        val viewModel = ConsentViewModel(repo)

        assertFalse(viewModel.ui.value.agreeChecked)

        viewModel.setAgree(true)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(true, viewModel.ui.value.agreeChecked)
    }

    @Test
    fun `accept saves consent and calls onDone`() = runTest {
        val repo = FakeConsentRepository(startGiven = false, startVersion = 0)
        val viewModel = ConsentViewModel(repo)
        var onDoneCalled = false

        // User must agree first
        viewModel.setAgree(true)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.accept { onDoneCalled = true }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(true, onDoneCalled)
        // Verify the repo was updated correctly
        assertEquals(false, viewModel.ui.value.consentRequired)
    }

    @Test
    fun `accept does nothing if not agreed`() = runTest {
        val repo = FakeConsentRepository(startGiven = false, startVersion = 0)
        val viewModel = ConsentViewModel(repo)
        var onDoneCalled = false

        viewModel.accept { onDoneCalled = true }
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(onDoneCalled)
    }
}
