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

@ExperimentalCoroutinesApi
class ConsentScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun acceptButton_isEnabled_only_when_checkbox_is_checked() {
        // ACT - Launch the ConsentScreen in a test environment.
        composeTestRule.setContent {
            // We use a local mutable state to simulate the checkbox behavior.
            var isChecked by remember { mutableStateOf(false) }

            ConsentScreen(
                agreeChecked = isChecked,
                onAgreeChange = { isChecked = it },
                onAccept = { },
                onDecline = { }
            )
        }

        // FIND the nodes we need to interact with.
        val acceptButton = composeTestRule.onNodeWithText("Accept & Continue")
        val agreeCheckbox = composeTestRule.onNodeWithTag("agree_checkbox")

        // ASSERT 1 - Initially, the button should be disabled.
        acceptButton.assertIsNotEnabled()

        // ACT 2 - Simulate a user click on the checkbox itself.
        agreeCheckbox.performClick()

        // ASSERT 2 - After clicking, the button should now be enabled.
        acceptButton.assertIsEnabled()
    }

    @Test
    fun acceptButton_triggers_onAccept_action() {
        // ARRANGE - A flag to verify the lambda is called.
        var onAcceptCalled = false

        composeTestRule.setContent {
            var isChecked by remember { mutableStateOf(false) }

            ConsentScreen(
                agreeChecked = isChecked,
                onAgreeChange = { isChecked = it },
                onAccept = { onAcceptCalled = true }, // Set the flag when onAccept is called
                onDecline = { }
            )
        }

        val acceptButton = composeTestRule.onNodeWithText("Accept & Continue")
        val agreeCheckbox = composeTestRule.onNodeWithTag("agree_checkbox")

        // ACT - Click the checkbox, then click the button.
        agreeCheckbox.performClick()
        acceptButton.performClick()

        // ASSERT - Verify that the onAccept lambda was executed.
        assertTrue("onAccept should have been called", onAcceptCalled)
    }

    @Test
    fun declineButton_triggers_onDecline_action() {
        // ARRANGE - A flag to verify the lambda is called.
        var onDeclineCalled = false

        composeTestRule.setContent {
            ConsentScreen(
                agreeChecked = false,
                onAgreeChange = { },
                onAccept = { },
                onDecline = { onDeclineCalled = true } // Set the flag when onDecline is called
            )
        }

        val declineButton = composeTestRule.onNodeWithText("Decline")

        // ACT - Click the decline button.
        declineButton.performClick()

        // ASSERT - Verify that the onDecline lambda was executed.
        assertTrue("onDecline should have been called", onDeclineCalled)
    }
}
