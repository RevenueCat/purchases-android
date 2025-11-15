package com.revenuecat.purchases.ui.revenuecatui.customercenter.views

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CreateSupportTicketData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterConfigTestData
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CreateSupportTicketViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockLocalization = CustomerCenterConfigTestData.customerCenterData().localization

    @Test
    fun `initial state has empty fields and disabled submit button`() {
        val mockData = CreateSupportTicketData(
            onSubmit = { _, _, _, _ -> },
            onCancel = { },
            onClose = { },
        )

        composeTestRule.setContent {
            CreateSupportTicketView(
                data = mockData,
                localization = mockLocalization,
            )
        }

        composeTestRule.onNodeWithText(
            mockLocalization.commonLocalizedString(
                CustomerCenterConfigData.Localization.CommonLocalizedString.SUBMIT_TICKET
            )
        ).assertIsNotEnabled()
    }

    @Test
    fun `submit button enabled when both fields are filled`() {
        val mockData = CreateSupportTicketData(
            onSubmit = { _, _, _, _ -> },
            onCancel = { },
            onClose = { },
        )

        composeTestRule.setContent {
            CreateSupportTicketView(
                data = mockData,
                localization = mockLocalization,
            )
        }

        // Fill email field
        composeTestRule.onNodeWithTag("email_field").performTextInput("test@example.com")

        // Fill description field
        composeTestRule.onNodeWithTag("description_field").performTextInput("I need help with my subscription")

        // Submit button should now be enabled
        composeTestRule.onNodeWithText(
            mockLocalization.commonLocalizedString(
                CustomerCenterConfigData.Localization.CommonLocalizedString.SUBMIT_TICKET
            )
        ).assertIsEnabled()
    }

    @Test
    fun `submit button disabled when only email is filled`() {
        val mockData = CreateSupportTicketData(
            onSubmit = { _, _, _, _ -> },
            onCancel = { },
            onClose = { },
        )

        composeTestRule.setContent {
            CreateSupportTicketView(
                data = mockData,
                localization = mockLocalization,
            )
        }

        // Fill only email field
        composeTestRule.onNodeWithTag("email_field").performTextInput("test@example.com")

        // Submit button should still be disabled
        composeTestRule.onNodeWithText(
            mockLocalization.commonLocalizedString(
                CustomerCenterConfigData.Localization.CommonLocalizedString.SUBMIT_TICKET
            )
        ).assertIsNotEnabled()
    }

    @Test
    fun `submit button disabled when only description is filled`() {
        val mockData = CreateSupportTicketData(
            onSubmit = { _, _, _, _ -> },
            onCancel = { },
            onClose = { },
        )

        composeTestRule.setContent {
            CreateSupportTicketView(
                data = mockData,
                localization = mockLocalization,
            )
        }

        // Fill only description field
        composeTestRule.onNodeWithTag("description_field").performTextInput("I need help with my subscription")

        // Submit button should still be disabled
        composeTestRule.onNodeWithText(
            mockLocalization.commonLocalizedString(
                CustomerCenterConfigData.Localization.CommonLocalizedString.SUBMIT_TICKET
            )
        ).assertIsNotEnabled()
    }

    @Test
    fun `clicking submit button calls onSubmit with correct parameters`() {
        val emailSlot = slot<String>()
        val descriptionSlot = slot<String>()
        val onSuccessSlot = slot<() -> Unit>()
        val onErrorSlot = slot<() -> Unit>()

        val mockOnSubmit = mockk<(String, String, () -> Unit, () -> Unit) -> Unit>(relaxed = true)
        val mockData = CreateSupportTicketData(
            onSubmit = mockOnSubmit,
            onCancel = { },
            onClose = { },
        )

        composeTestRule.setContent {
            CreateSupportTicketView(
                data = mockData,
                localization = mockLocalization,
            )
        }

        val testEmail = "test@example.com"
        val testDescription = "I need help"

        // Fill both fields
        composeTestRule.onNodeWithTag("email_field").performTextInput(testEmail)
        composeTestRule.onNodeWithTag("description_field").performTextInput(testDescription)

        // Click submit
        composeTestRule.onNodeWithText(
            mockLocalization.commonLocalizedString(
                CustomerCenterConfigData.Localization.CommonLocalizedString.SUBMIT_TICKET
            )
        ).performClick()

        // Verify onSubmit was called with correct parameters
        verify {
            mockOnSubmit(
                capture(emailSlot),
                capture(descriptionSlot),
                capture(onSuccessSlot),
                capture(onErrorSlot)
            )
        }

        assert(emailSlot.captured == testEmail)
        assert(descriptionSlot.captured == testDescription)
    }

    @Test
    fun `success state calls onSuccess callback`() {
        var successCalled = false
        val mockOnSubmit: (String, String, () -> Unit, () -> Unit) -> Unit = { _, _, onSuccess, _ ->
            successCalled = true
            onSuccess()
        }

        val mockData = CreateSupportTicketData(
            onSubmit = mockOnSubmit,
            onCancel = { },
            onClose = { },
        )

        composeTestRule.setContent {
            CreateSupportTicketView(
                data = mockData,
                localization = mockLocalization,
            )
        }

        // Fill both fields
        composeTestRule.onNodeWithTag("email_field").performTextInput("test@example.com")
        composeTestRule.onNodeWithTag("description_field").performTextInput("I need help")

        // Click submit
        composeTestRule.onNodeWithText(
            mockLocalization.commonLocalizedString(
                CustomerCenterConfigData.Localization.CommonLocalizedString.SUBMIT_TICKET
            )
        ).performClick()

        // Verify success callback was called
        assert(successCalled)
    }

    @Test
    fun `error state shows snackbar and keeps fields enabled`() {
        val mockOnSubmit: (String, String, () -> Unit, () -> Unit) -> Unit = { _, _, _, onError ->
            onError()
        }

        val mockData = CreateSupportTicketData(
            onSubmit = mockOnSubmit,
            onCancel = { },
            onClose = { },
        )

        composeTestRule.setContent {
            CreateSupportTicketView(
                data = mockData,
                localization = mockLocalization,
            )
        }

        // Fill both fields
        composeTestRule.onNodeWithTag("email_field").performTextInput("test@example.com")
        composeTestRule.onNodeWithTag("description_field").performTextInput("I need help")

        // Click submit
        composeTestRule.onNodeWithText(
            mockLocalization.commonLocalizedString(
                CustomerCenterConfigData.Localization.CommonLocalizedString.SUBMIT_TICKET
            )
        ).performClick()

        // Wait for snackbar to appear
        composeTestRule.waitForIdle()

        // Verify snackbar error message is shown
        composeTestRule.onNodeWithText(
            mockLocalization.commonLocalizedString(
                CustomerCenterConfigData.Localization.CommonLocalizedString.SUPPORT_TICKET_FAILED
            )
        ).assertExists()

        // Submit button should still be enabled for retry
        composeTestRule.onNodeWithText(
            mockLocalization.commonLocalizedString(
                CustomerCenterConfigData.Localization.CommonLocalizedString.SUBMIT_TICKET
            )
        ).assertIsEnabled()
    }

    @Test
    fun `can retry submission after error`() {
        var submitCount = 0
        val mockOnSubmit: (String, String, () -> Unit, () -> Unit) -> Unit = { _, _, _, onError ->
            submitCount++
            onError()
        }

        val mockData = CreateSupportTicketData(
            onSubmit = mockOnSubmit,
            onCancel = { },
            onClose = { },
        )

        composeTestRule.setContent {
            CreateSupportTicketView(
                data = mockData,
                localization = mockLocalization,
            )
        }

        // Fill both fields
        composeTestRule.onNodeWithTag("email_field").performTextInput("test@example.com")
        composeTestRule.onNodeWithTag("description_field").performTextInput("I need help")

        // Click submit twice
        composeTestRule.onNodeWithText(
            mockLocalization.commonLocalizedString(
                CustomerCenterConfigData.Localization.CommonLocalizedString.SUBMIT_TICKET
            )
        ).performClick()

        composeTestRule.onNodeWithText(
            mockLocalization.commonLocalizedString(
                CustomerCenterConfigData.Localization.CommonLocalizedString.SUBMIT_TICKET
            )
        ).performClick()

        // Verify onSubmit was called twice
        assert(submitCount == 2)
    }

    @Test
    fun `localization strings are displayed correctly`() {
        val mockData = CreateSupportTicketData(
            onSubmit = { _, _, _, _ -> },
            onCancel = { },
            onClose = { },
        )

        composeTestRule.setContent {
            CreateSupportTicketView(
                data = mockData,
                localization = mockLocalization,
            )
        }

        // Verify email label exists
        composeTestRule.onNodeWithText(
            mockLocalization.commonLocalizedString(
                CustomerCenterConfigData.Localization.CommonLocalizedString.EMAIL
            )
        ).assertExists()

        // Verify description label exists
        composeTestRule.onNodeWithText(
            mockLocalization.commonLocalizedString(
                CustomerCenterConfigData.Localization.CommonLocalizedString.DESCRIPTION
            )
        ).assertExists()

        // Verify submit button text exists
        composeTestRule.onNodeWithText(
            mockLocalization.commonLocalizedString(
                CustomerCenterConfigData.Localization.CommonLocalizedString.SUBMIT_TICKET
            )
        ).assertExists()
    }
}
