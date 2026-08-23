package com.revenuecat.purchases.ui.revenuecatui.customercenter

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterConfigTestData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterState
import com.revenuecat.purchases.ui.revenuecatui.customercenter.viewmodel.CustomerCenterViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CustomerCenterCloseButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `close button is shown by default`() {
        composeTestRule.setContent {
            InternalCustomerCenter(
                viewModel = viewModel(successState()),
                onDismiss = {},
            )
        }

        composeTestRule.onNodeWithTag("customer_center_navigation_button").assertIsDisplayed()
    }

    @Test
    fun `close button is hidden when shouldShowCloseButton is false`() {
        composeTestRule.setContent {
            InternalCustomerCenter(
                shouldShowCloseButton = false,
                viewModel = viewModel(successState()),
                onDismiss = {},
            )
        }

        composeTestRule.onNodeWithTag("customer_center_navigation_button").assertDoesNotExist()
    }

    @Test
    fun `back button is shown even when shouldShowCloseButton is false`() {
        composeTestRule.setContent {
            InternalCustomerCenter(
                shouldShowCloseButton = false,
                viewModel = viewModel(
                    successState(navigationButtonType = CustomerCenterState.NavigationButtonType.BACK),
                ),
                onDismiss = {},
            )
        }

        composeTestRule.onNodeWithTag("customer_center_navigation_button").assertIsDisplayed()
    }

    @Test
    fun `options default to showing the close button`() {
        val options = CustomerCenterOptions.Builder().build()

        assertThat(options.shouldShowCloseButton).isTrue()
    }

    @Test
    fun `options builder sets shouldShowCloseButton`() {
        val options = CustomerCenterOptions.Builder()
            .setShouldShowCloseButton(false)
            .build()

        assertThat(options.shouldShowCloseButton).isFalse()
    }

    private fun viewModel(state: CustomerCenterState): CustomerCenterViewModel =
        mockk<CustomerCenterViewModel>(relaxed = true).also { viewModel ->
            every { viewModel.state } returns MutableStateFlow(state)
            every { viewModel.actionError } returns mutableStateOf(null)
        }

    private fun successState(
        navigationButtonType: CustomerCenterState.NavigationButtonType =
            CustomerCenterState.NavigationButtonType.CLOSE,
    ) = CustomerCenterState.Success(
        customerCenterConfigData = CustomerCenterConfigTestData.customerCenterData(),
        purchases = emptyList(),
        mainScreenPaths = emptyList(),
        detailScreenPaths = emptyList(),
        navigationButtonType = navigationButtonType,
    )
}
