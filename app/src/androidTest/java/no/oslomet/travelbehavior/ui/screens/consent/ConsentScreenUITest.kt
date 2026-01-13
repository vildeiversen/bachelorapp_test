package no.oslomet.travelbehavior.ui.screens.consent

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Instrumented UI test for [ConsentScreen].
 * Verifies the interaction logic of the consent screen, ensuring legal requirements are met. */

@ExperimentalCoroutinesApi
class ConsentScreenUITest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /** UI-04: Verifies that the "Accept & Continue" button is only enabled
     * after the user has checked the agreement checkbox. */
    @Test
    fun acceptButton_isEnabled_onlyWhenChecked() {
        // Act - Launch the ConsentScreen in a test environment with local state
        composeTestRule.setContent {
            var isChecked by remember { mutableStateOf(false) }

            ConsentScreen(
                agreeChecked = isChecked,
                onAgreeChange = { isChecked = it },
                onAccept = { },
                onDecline = { }
            )
        }

        val acceptButton = composeTestRule.onNodeWithText("Accept & Continue")
        val agreeCheckbox = composeTestRule.onNodeWithTag("agree_checkbox")

        // Assert 1 - Button should be disabled initially
        acceptButton.assertIsNotEnabled()

        // Act 2 - User clicks the checkbox
        agreeCheckbox.performClick()

        // Assert 2 - Button should now be enabled
        acceptButton.assertIsEnabled()
    }

    /** UI-05: Verifies that clicking the "Accept & Continue" button correctly
     * triggers the onAccept callback. */
    @Test
    fun acceptButtonTriggers_onAccept_action() {
        // Arrange - Setup a flag to track if the callback is triggered
        var onAcceptCalled = false

        composeTestRule.setContent {
            var isChecked by remember { mutableStateOf(false) }

            ConsentScreen(
                agreeChecked = isChecked,
                onAgreeChange = { isChecked = it },
                onAccept = { onAcceptCalled = true },
                onDecline = { }
            )
        }

        val acceptButton = composeTestRule.onNodeWithText("Accept & Continue")
        val agreeCheckbox = composeTestRule.onNodeWithTag("agree_checkbox")

        // Act - Click the checkbox, then the accept button
        agreeCheckbox.performClick()
        acceptButton.performClick()

        // Assert - Verify that the onAccept lambda was executed
        assertTrue("onAccept should have been called", onAcceptCalled)
    }

    /** UI-06: Verifies that clicking the "Decline" button correctly
     * triggers the onDecline callback. */
    @Test
    fun declineButtonTriggers_onDecline_action() {
        // Arrange - Setup a flag to track if the callback is triggered
        var onDeclineCalled = false

        composeTestRule.setContent {
            ConsentScreen(
                agreeChecked = false,
                onAgreeChange = { },
                onAccept = { },
                onDecline = { onDeclineCalled = true }
            )
        }

        val declineButton = composeTestRule.onNodeWithText("Decline")

        // Act - Click the decline button
        declineButton.performClick()

        // Assert - Verify that the onDecline lambda was executed
        assertTrue("onDecline should have been called", onDeclineCalled)
    }
}
