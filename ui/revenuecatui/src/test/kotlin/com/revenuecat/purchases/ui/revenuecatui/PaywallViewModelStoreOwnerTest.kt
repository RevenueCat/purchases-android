package com.revenuecat.purchases.ui.revenuecatui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.DangerousSettings
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchases.common.workflows.WorkflowResolution
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModel
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PaywallViewModelStoreOwnerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        val purchases = mockk<Purchases>()
        mockkObject(Purchases)
        every { Purchases.sharedInstance } returns purchases
        every { purchases.purchasesAreCompletedBy } returns PurchasesAreCompletedBy.REVENUECAT
        every { purchases.storefrontCountryCode } returns "US"
        every { purchases.preferredUILocaleOverride } returns null
        every { purchases.track(any()) } just Runs
        every { purchases.currentConfiguration } returns mockk {
            every { dangerousSettings } returns DangerousSettings()
        }
        coEvery { purchases.resolveWorkflow(any()) } returns WorkflowResolution.NoWorkflow
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `removing presentation from live parent clears its ViewModel`() {
        val parentOwner = TestViewModelStoreOwner()
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)
        var showPresentation by mutableStateOf(true)
        var capturedViewModel: TrackingViewModel? = null

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides parentOwner,
                LocalLifecycleOwner provides lifecycleOwner,
            ) {
                if (showPresentation) {
                    val owner = rememberPaywallViewModelStoreOwner("paywall")
                    capturedViewModel = ViewModelProvider(owner)[TrackingViewModel::class.java]
                }
            }
        }
        composeTestRule.waitForIdle()
        val firstViewModel = checkNotNull(capturedViewModel)

        composeTestRule.runOnIdle { showPresentation = false }
        composeTestRule.waitForIdle()

        assertThat(firstViewModel.clearCount).isEqualTo(1)

        composeTestRule.runOnIdle { showPresentation = true }
        composeTestRule.waitForIdle()

        assertThat(capturedViewModel).isNotSameAs(firstViewModel)
    }

    @Test
    fun `configuration recreation retains presentation ViewModel`() {
        val parentOwner = TestViewModelStoreOwner()
        val firstLifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)
        var lifecycleOwner by mutableStateOf(firstLifecycleOwner)
        var showPresentation by mutableStateOf(true)
        var capturedViewModel: TrackingViewModel? = null

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides parentOwner,
                LocalLifecycleOwner provides lifecycleOwner,
            ) {
                if (showPresentation) {
                    val owner = rememberPaywallViewModelStoreOwner("paywall")
                    capturedViewModel = ViewModelProvider(owner)[TrackingViewModel::class.java]
                }
            }
        }
        composeTestRule.waitForIdle()
        val firstViewModel = checkNotNull(capturedViewModel)

        composeTestRule.runOnIdle {
            firstLifecycleOwner.moveTo(Lifecycle.State.DESTROYED)
            showPresentation = false
        }
        composeTestRule.waitForIdle()

        assertThat(firstViewModel.clearCount).isZero()

        composeTestRule.runOnIdle {
            lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)
            showPresentation = true
        }
        composeTestRule.waitForIdle()

        assertThat(capturedViewModel).isSameAs(firstViewModel)
    }

    @Test
    fun `getPaywallViewModel creates a new instance for a new presentation`() {
        val parentOwner = TestViewModelStoreOwner()
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)
        val options = PaywallOptions.Builder(dismissRequest = {})
            .setOffering(TestData.template1Offering)
            .build()
        var showPresentation by mutableStateOf(true)
        var capturedViewModel: PaywallViewModel? = null

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides parentOwner,
                LocalLifecycleOwner provides lifecycleOwner,
            ) {
                if (showPresentation) {
                    capturedViewModel = getPaywallViewModel(options)
                }
            }
        }
        composeTestRule.waitForIdle()
        val firstViewModel = checkNotNull(capturedViewModel)

        composeTestRule.runOnIdle { showPresentation = false }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle { showPresentation = true }
        composeTestRule.waitForIdle()

        assertThat(capturedViewModel).isNotSameAs(firstViewModel)
    }

    @Test
    fun `identical options at separate call sites use separate ViewModels`() {
        val parentOwner = TestViewModelStoreOwner()
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)
        val options = PaywallOptions.Builder(dismissRequest = {})
            .setOffering(TestData.template1Offering)
            .build()
        var firstViewModel: PaywallViewModel? = null
        var secondViewModel: PaywallViewModel? = null

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides parentOwner,
                LocalLifecycleOwner provides lifecycleOwner,
            ) {
                firstViewModel = getPaywallViewModel(options)
                secondViewModel = getPaywallViewModel(options)
            }
        }
        composeTestRule.waitForIdle()

        assertThat(secondViewModel).isNotSameAs(firstViewModel)
    }

    private class TestViewModelStoreOwner : ViewModelStoreOwner {
        override val viewModelStore = ViewModelStore()
    }

    private class TestLifecycleOwner(initialState: Lifecycle.State) : LifecycleOwner {
        private val registry = LifecycleRegistry(this).apply {
            currentState = initialState
        }

        override val lifecycle: Lifecycle = registry

        fun moveTo(state: Lifecycle.State) {
            registry.currentState = state
        }
    }

    class TrackingViewModel : ViewModel() {
        var clearCount = 0
            private set

        override fun onCleared() {
            clearCount++
        }
    }
}
