package com.revenuecat.purchases.ui.revenuecatui.customercenter

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterConfigTestData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterState
import com.revenuecat.purchases.ui.revenuecatui.customercenter.navigation.CustomerCenterDestination
import com.revenuecat.purchases.ui.revenuecatui.customercenter.navigation.CustomerCenterNavigationState
import com.revenuecat.purchases.ui.revenuecatui.customercenter.viewmodel.CustomerCenterViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CustomerCenterNavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `close button is shown by default`() {
        composeTestRule.setContent {
            InternalCustomerCenter(
                viewModel = viewModel(MutableStateFlow(successState())),
                onDismiss = {},
            )
        }

        composeTestRule.onNodeWithTag("customer_center_navigation_button").assertIsDisplayed()
    }

    @Test
    fun `close button is hidden when the top bar is hidden`() {
        composeTestRule.setContent {
            InternalCustomerCenter(
                options = optionsWith(
                    CustomerCenterNavigationOptions.Builder()
                        .setShouldShowTopBar(false)
                        .build(),
                ),
                viewModel = viewModel(MutableStateFlow(successState())),
                onDismiss = {},
            )
        }

        composeTestRule.onNodeWithTag("customer_center_navigation_button").assertDoesNotExist()
    }

    @Test
    fun `back button is hidden when the top bar is hidden`() {
        composeTestRule.setContent {
            InternalCustomerCenter(
                options = optionsWith(
                    CustomerCenterNavigationOptions.Builder()
                        .setShouldShowTopBar(false)
                        .build(),
                ),
                viewModel = viewModel(
                    MutableStateFlow(
                        successState(navigationButtonType = CustomerCenterState.NavigationButtonType.BACK),
                    ),
                ),
                onDismiss = {},
            )
        }

        composeTestRule.onNodeWithTag("customer_center_navigation_button").assertDoesNotExist()
    }

    @Test
    fun `back button is shown by default`() {
        composeTestRule.setContent {
            InternalCustomerCenter(
                viewModel = viewModel(
                    MutableStateFlow(
                        successState(navigationButtonType = CustomerCenterState.NavigationButtonType.BACK),
                    ),
                ),
                onDismiss = {},
            )
        }

        composeTestRule.onNodeWithTag("customer_center_navigation_button").assertIsDisplayed()
    }

    @Test
    fun `top bar is not shown when shouldShowTopBar is false`() {
        composeTestRule.setContent {
            InternalCustomerCenter(
                options = optionsWith(
                    CustomerCenterNavigationOptions.Builder()
                        .setShouldShowTopBar(false)
                        .build(),
                ),
                viewModel = viewModel(MutableStateFlow(successState())),
                onDismiss = {},
            )
        }

        composeTestRule.onNodeWithTag("customer_center_top_bar").assertDoesNotExist()
    }

    @Test
    fun `top bar is not shown when shouldShowTopBar is false and the screen has a title`() {
        composeTestRule.setContent {
            InternalCustomerCenter(
                options = optionsWith(
                    CustomerCenterNavigationOptions.Builder()
                        .setShouldShowTopBar(false)
                        .build(),
                ),
                viewModel = viewModel(MutableStateFlow(purchaseDetailState("Subscription details"))),
                onDismiss = {},
            )
        }

        composeTestRule.onNodeWithTag("customer_center_top_bar").assertDoesNotExist()
        composeTestRule.onNodeWithTag("customer_center_top_bar_title").assertDoesNotExist()
    }

    @Test
    fun `top bar shows the screen title by default`() {
        composeTestRule.setContent {
            InternalCustomerCenter(
                viewModel = viewModel(MutableStateFlow(purchaseDetailState("Subscription details"))),
                onDismiss = {},
            )
        }

        composeTestRule.onNodeWithTag("customer_center_top_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("customer_center_top_bar_title").assertIsDisplayed()
    }

    @Test
    fun `top bar is shown by default`() {
        composeTestRule.setContent {
            InternalCustomerCenter(
                viewModel = viewModel(MutableStateFlow(successState())),
                onDismiss = {},
            )
        }

        composeTestRule.onNodeWithTag("customer_center_top_bar").assertIsDisplayed()
    }

    @Test
    fun `navigation listener is notified when the customer center is displayed`() {
        val events = mutableListOf<Pair<Boolean, String?>>()

        composeTestRule.setContent {
            InternalCustomerCenter(
                options = optionsWith(
                    CustomerCenterNavigationOptions.Builder()
                        .setListener { canNavigateBack, title -> events.add(canNavigateBack to title) }
                        .build(),
                ),
                viewModel = viewModel(MutableStateFlow(successState())),
                onDismiss = {},
            )
        }
        composeTestRule.waitForIdle()

        assertThat(events).containsExactly(false to null)
    }

    @Test
    fun `navigation listener is notified when navigating to another screen`() {
        val events = mutableListOf<Pair<Boolean, String?>>()
        val stateFlow = MutableStateFlow(successState())

        composeTestRule.setContent {
            InternalCustomerCenter(
                options = optionsWith(
                    CustomerCenterNavigationOptions.Builder()
                        .setListener { canNavigateBack, title -> events.add(canNavigateBack to title) }
                        .build(),
                ),
                viewModel = viewModel(stateFlow),
                onDismiss = {},
            )
        }
        composeTestRule.waitForIdle()

        val pushedScreenTitle = "Subscription details"
        stateFlow.value = purchaseDetailState(pushedScreenTitle)
        composeTestRule.waitForIdle()

        assertThat(events).containsExactly(
            false to null,
            true to pushedScreenTitle,
        )
    }

    @Test
    fun `navigation listener attached after the first composition is notified`() {
        val events = mutableListOf<Pair<Boolean, String?>>()
        val optionsState = mutableStateOf(CustomerCenterOptions.Builder().build())

        composeTestRule.setContent {
            InternalCustomerCenter(
                options = optionsState.value,
                viewModel = viewModel(MutableStateFlow(successState())),
                onDismiss = {},
            )
        }
        composeTestRule.waitForIdle()

        optionsState.value = optionsWith(
            CustomerCenterNavigationOptions.Builder()
                .setListener { canNavigateBack, title -> events.add(canNavigateBack to title) }
                .build(),
        )
        composeTestRule.waitForIdle()

        assertThat(events).containsExactly(false to null)
    }

    @Test
    fun `navigation options default to showing the top bar and no listener`() {
        val navigationOptions = CustomerCenterOptions.Builder().build().navigationOptions

        assertThat(navigationOptions.shouldShowTopBar).isTrue()
        assertThat(navigationOptions.listener).isNull()
    }

    @Test
    fun `navigation options builder sets its values`() {
        val listener = CustomerCenterNavigationListener { _, _ -> }

        val navigationOptions = CustomerCenterNavigationOptions.Builder()
            .setShouldShowTopBar(false)
            .setListener(listener)
            .build()

        assertThat(navigationOptions.shouldShowTopBar).isFalse()
        assertThat(navigationOptions.listener).isEqualTo(listener)
    }

    private fun purchaseDetailState(title: String): CustomerCenterState = successState(
        navigationButtonType = CustomerCenterState.NavigationButtonType.BACK,
        navigationState = navigationStateShowing(
            CustomerCenterDestination.SelectedPurchaseDetail(
                purchaseInformation = CustomerCenterConfigTestData.purchaseInformationMonthlyRenewing,
                title = title,
            ),
        ),
    )

    private fun optionsWith(navigationOptions: CustomerCenterNavigationOptions) =
        CustomerCenterOptions.Builder()
            .setNavigationOptions(navigationOptions)
            .build()

    private fun viewModel(state: MutableStateFlow<CustomerCenterState>): CustomerCenterViewModel =
        mockk<CustomerCenterViewModel>(relaxed = true).also { viewModel ->
            every { viewModel.state } returns state
            every { viewModel.actionError } returns mutableStateOf(null)
        }

    private fun navigationStateShowing(destination: CustomerCenterDestination) =
        CustomerCenterNavigationState(
            showingActivePurchasesScreen = false,
            managementScreenTitle = null,
        ).push(destination)

    private fun successState(
        navigationButtonType: CustomerCenterState.NavigationButtonType =
            CustomerCenterState.NavigationButtonType.CLOSE,
        navigationState: CustomerCenterNavigationState = CustomerCenterNavigationState(
            showingActivePurchasesScreen = false,
            managementScreenTitle = null,
        ),
    ): CustomerCenterState = CustomerCenterState.Success(
        customerCenterConfigData = CustomerCenterConfigTestData.customerCenterData(),
        purchases = emptyList(),
        mainScreenPaths = emptyList(),
        detailScreenPaths = emptyList(),
        navigationState = navigationState,
        navigationButtonType = navigationButtonType,
    )
}
