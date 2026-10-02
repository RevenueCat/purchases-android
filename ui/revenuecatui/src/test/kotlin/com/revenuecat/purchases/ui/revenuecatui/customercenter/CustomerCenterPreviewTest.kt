// Created by Monika on 2026-10-02

@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.customercenter

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.actions.CustomerCenterAction
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterConfigTestData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterState
import com.revenuecat.purchases.ui.revenuecatui.customercenter.navigation.CustomerCenterDestination
import com.revenuecat.purchases.ui.revenuecatui.customercenter.viewmodel.CustomerCenterViewModel
import io.mockk.clearMocks
import io.mockk.verify
import io.mockk.coVerify
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.CompletableDeferred
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowLooper

@RunWith(AndroidJUnit4::class)
class CustomerCenterPreviewTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `preview renders without a configured SDK`() {
        val provider = mockk<CustomerCenterPreviewProvider>(relaxed = true)
        every { provider.customerInfoUpdates } returns null
        coEvery { provider.workflow(any()) } returns null
        every { provider.store } returns Store.APP_STORE
        coEvery { provider.configuration() } returns CustomerCenterConfigTestData.customerCenterData()
        coEvery { provider.customerInfo() } returns mockk<CustomerInfo>(relaxed = true)

        composeTestRule.setContent {
            MaterialTheme {
                CustomerCenterPreview(provider = provider, onDismiss = {})
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(hasText("No subscriptions found")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNode(hasText("No subscriptions found")).assertIsDisplayed()
    }

    @Test
    fun `preview paywall renders and restores through provider without a configured SDK`() {
        val provider = mockk<CustomerCenterPreviewProvider>(relaxed = true)
        every { provider.customerInfoUpdates } returns null
        coEvery { provider.workflow(any()) } returns null
        every { provider.store } returns Store.APP_STORE
        coEvery { provider.customerInfo() } returns mockk<CustomerInfo>(relaxed = true)
        coEvery { provider.restorePurchases() } returns mockk<CustomerInfo>(relaxed = true)
        composeTestRule.setContent {
            MaterialTheme {
                CustomerCenterPreviewPaywall(provider, TestData.template1Offering, isDarkMode = true, onDismiss = {})
            }
        }
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(hasText("Restore purchases", ignoreCase = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNode(hasText("Restore purchases", ignoreCase = true)).performClick()
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()
        coVerify(exactly = 1) { provider.restorePurchases() }
    }

    @Test
    fun `replacing provider cancels old scenario loading and renders the new session`() {
        val oldProvider = mockk<CustomerCenterPreviewProvider>(relaxed = true)
        val newProvider = mockk<CustomerCenterPreviewProvider>(relaxed = true)
        val started = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        coEvery { oldProvider.configuration() } coAnswers {
            started.complete(Unit)
            try {
                CompletableDeferred<Nothing>().await()
            } finally {
                cancelled.complete(Unit)
            }
        }
        every { newProvider.store } returns Store.APP_STORE
        coEvery { newProvider.configuration() } returns CustomerCenterConfigTestData.customerCenterData()
        coEvery { newProvider.customerInfo() } returns mockk<CustomerInfo>(relaxed = true)
        val selectedProvider = mutableStateOf(oldProvider)
        composeTestRule.setContent {
            MaterialTheme { CustomerCenterPreview(selectedProvider.value, onDismiss = {}) }
        }
        composeTestRule.waitUntil(timeoutMillis = 5_000) { started.isCompleted }

        composeTestRule.runOnIdle { selectedProvider.value = newProvider }

        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()
        assertThat(cancelled.isCompleted).isTrue()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(hasText("No subscriptions found")).fetchSemanticsNodes().isNotEmpty()
        }
        coVerify(exactly = 1) { oldProvider.configuration() }
        coVerify(exactly = 1) { newProvider.configuration() }
    }

    @Test
    fun `disposing preview paywall cancels an in flight restore`() {
        val provider = mockk<CustomerCenterPreviewProvider>(relaxed = true)
        every { provider.customerInfoUpdates } returns null
        coEvery { provider.workflow(any()) } returns null
        every { provider.store } returns Store.APP_STORE
        coEvery { provider.customerInfo() } returns mockk<CustomerInfo>(relaxed = true)
        val started = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        coEvery { provider.restorePurchases() } coAnswers {
            started.complete(Unit)
            try {
                CompletableDeferred<Nothing>().await()
            } finally {
                cancelled.complete(Unit)
            }
        }
        val visible = mutableStateOf(true)
        composeTestRule.setContent {
            MaterialTheme {
                if (visible.value) {
                    CustomerCenterPreviewPaywall(provider, TestData.template1Offering, onDismiss = {})
                }
            }
        }
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(hasText("Restore purchases", ignoreCase = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNode(hasText("Restore purchases", ignoreCase = true)).performClick()
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()
        composeTestRule.waitUntil(timeoutMillis = 5_000) { started.isCompleted }

        composeTestRule.runOnIdle { visible.value = false }

        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()
        assertThat(cancelled.isCompleted).isTrue()
        coVerify(exactly = 1) { provider.restorePurchases() }
    }

    @Test
    fun `replacing session reattaches lifecycle callbacks to the current view model`() {
        val oldModel = mockk<CustomerCenterViewModel>(relaxed = true)
        val newModel = mockk<CustomerCenterViewModel>(relaxed = true)
        listOf(oldModel, newModel).forEach { model ->
            every { model.state } returns MutableStateFlow(CustomerCenterState.Loading)
            every { model.actionError } returns mutableStateOf(null)
        }
        val owner = mockk<LifecycleOwner>()
        lateinit var lifecycle: LifecycleRegistry
        composeTestRule.runOnIdle {
            lifecycle = LifecycleRegistry(owner)
            every { owner.lifecycle } returns lifecycle
            lifecycle.currentState = Lifecycle.State.RESUMED
        }
        val selectedModel = mutableStateOf(oldModel)
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                MaterialTheme {
                    InternalCustomerCenter(viewModel = selectedModel.value, onDismiss = {})
                }
            }
        }
        composeTestRule.runOnIdle { selectedModel.value = newModel }
        composeTestRule.waitForIdle()
        verify(exactly = 1) { newModel.trackImpressionIfNeeded() }
        verify { newModel.refreshColors(any(), any()) }
        clearMocks(oldModel, newModel, answers = false)

        composeTestRule.runOnIdle {
            lifecycle.currentState = Lifecycle.State.CREATED
            lifecycle.currentState = Lifecycle.State.RESUMED
        }

        verify(exactly = 0) { oldModel.onActivityStopped(any()) }
        verify(exactly = 0) { oldModel.onActivityStarted() }
        verify(exactly = 0) { oldModel.onActivityResumed() }
        verify(exactly = 1) { newModel.onActivityStopped(any()) }
        verify(exactly = 1) { newModel.onActivityStarted() }
        verify(exactly = 1) { newModel.onActivityResumed() }
    }

    @Test
    fun `customer updates refresh the visible preview without replacing its provider`() {
        val customers = MutableStateFlow(mockk<CustomerInfo>(relaxed = true))
        val provider = mockk<CustomerCenterPreviewProvider>(relaxed = true)
        every { provider.customerInfoUpdates } returns customers
        every { provider.store } returns Store.APP_STORE
        coEvery { provider.configuration() } returns CustomerCenterConfigTestData.customerCenterData()
        coEvery { provider.customerInfo() } coAnswers { customers.value }
        composeTestRule.setContent {
            MaterialTheme { CustomerCenterPreview(provider, onDismiss = {}) }
        }
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(hasText("No subscriptions found")).fetchSemanticsNodes().isNotEmpty()
        }
        coVerify(exactly = 1) { provider.customerInfo() }
        composeTestRule.runOnIdle { customers.value = mockk<CustomerInfo>(relaxed = true) }
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()
        coVerify(exactly = 2) { provider.customerInfo() }
    }

    @Test
    fun `modal close can be hidden and embedded previews keep a root navigation button`() {
        val options = mutableStateOf(CustomerCenterPreviewOptions(showCloseButton = false))
        val actions = mutableListOf<CustomerCenterAction>()
        composeTestRule.setContent {
            MaterialTheme {
                InternalCustomerCenter(
                    state = CustomerCenterState.Loading,
                    previewOptions = options.value,
                    onAction = { actions.add(it) },
                )
            }
        }
        composeTestRule.onAllNodes(hasClickAction()).assertCountEquals(0)

        composeTestRule.runOnIdle {
            options.value = CustomerCenterPreviewOptions(usesExistingNavigation = true, showCloseButton = false)
        }
        composeTestRule.onAllNodes(hasClickAction()).assertCountEquals(1)
        composeTestRule.onNode(hasClickAction()).performClick()
        assertThat(actions).containsExactly(CustomerCenterAction.NavigationButtonPressed)
    }

    @Test
    fun `refreshing preview purchase details updates content without animating navigation`() {
        val purchase = CustomerCenterConfigTestData.purchaseInformationYearlyExpiring.copy(title = "Original purchase")
        val initial = CustomerCenterState.Success(
            customerCenterConfigData = CustomerCenterConfigTestData.customerCenterData(),
            purchases = listOf(purchase),
        )
        val state = mutableStateOf(initial.copy(
            navigationState = initial.navigationState.push(
                CustomerCenterDestination.SelectedPurchaseDetail(purchase, "Management"),
            ),
        ))
        composeTestRule.setContent {
            MaterialTheme {
                InternalCustomerCenter(state.value, previewOptions = CustomerCenterPreviewOptions(), onAction = {})
            }
        }
        composeTestRule.onNode(hasText("Original purchase")).assertIsDisplayed()
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.runOnIdle {
            val refreshed = purchase.copy(title = "Updated purchase")
            state.value = state.value.copy(
                purchases = listOf(refreshed),
                navigationState = state.value.navigationState.pop().push(
                    CustomerCenterDestination.SelectedPurchaseDetail(refreshed, "Management"),
                ),
            )
        }
        ShadowLooper.idleMainLooper()
        composeTestRule.mainClock.advanceTimeBy(64)
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodes(hasText("Original purchase")).assertCountEquals(0)
        composeTestRule.onNode(hasText("Updated purchase")).assertIsDisplayed()
    }

    @Test
    fun `nested balances render supplied scenario without creating a live SDK view model`() {
        val config = CustomerCenterConfigTestData.customerCenterData()
        val state = CustomerCenterState.Success(
            customerCenterConfigData = config,
            virtualCurrencies = CustomerCenterConfigTestData.fiveVirtualCurrencies,
        )
        val balancesState = state.copy(
            navigationState = state.navigationState.push(CustomerCenterDestination.VirtualCurrencyBalances("Balances")),
        )

        composeTestRule.setContent {
            MaterialTheme {
                InternalCustomerCenter(
                    state = balancesState,
                    previewOptions = CustomerCenterPreviewOptions(isDarkMode = true, showCloseButton = false),
                    onAction = {},
                )
            }
        }

        CustomerCenterConfigTestData.fiveVirtualCurrencies.all.values.forEach { currency ->
            composeTestRule.onNode(hasScrollAction()).performScrollToNode(hasText("${currency.name} (${currency.code})"))
            composeTestRule.onNode(hasText("${currency.name} (${currency.code})")).assertIsDisplayed()
        }
    }
}
