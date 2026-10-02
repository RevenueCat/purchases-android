// Created by Monika on 2026-10-02

@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.customercenter

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.paywalls.components.ButtonComponent
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class CustomerCenterPreviewPaywallTest {
    private val provider = mockk<CustomerCenterPreviewProvider>(relaxed = true)
    private val viewModel = mockk<PaywallViewModel>(relaxed = true)

    @Test
    fun `checkout uses SDK resolved URL and dismisses only after simulated action completes`() = runTest {
        val action = checkout(autoDismiss = true)
        val completed = CompletableDeferred<Unit>()
        every { viewModel.getWebCheckoutUrl(action) } returns "https://example.com/checkout?package=annual"
        coEvery { provider.handleAction(any()) } coAnswers { completed.await() }

        val job = launch { assertThat(interceptPreviewPaywallAction(action, provider, viewModel)).isTrue() }
        testScheduler.runCurrent()
        coVerify { provider.handleAction(CustomerCenterPreviewAction.OpenUrl("https://example.com/checkout?package=annual")) }
        verify(exactly = 0) { viewModel.closePaywall(any(), any()) }

        completed.complete(Unit)
        job.join()
        verify(exactly = 1) { viewModel.closePaywall(any(), any()) }
    }

    @Test
    fun `checkout without auto dismiss leaves preview visible`() = runTest {
        val action = checkout(autoDismiss = false)
        every { viewModel.getWebCheckoutUrl(action) } returns "https://example.com/offering"

        assertThat(interceptPreviewPaywallAction(action, provider, viewModel)).isTrue()

        coVerify { provider.handleAction(CustomerCenterPreviewAction.OpenUrl("https://example.com/offering")) }
        verify(exactly = 0) { viewModel.closePaywall(any(), any()) }
    }

    @Test
    fun `checkout without a URL is consumed without closing or launching anything`() = runTest {
        val action = checkout(autoDismiss = true)
        every { viewModel.getWebCheckoutUrl(action) } returns null

        assertThat(interceptPreviewPaywallAction(action, provider, viewModel)).isTrue()

        coVerify(exactly = 0) { provider.handleAction(any()) }
        verify(exactly = 0) { viewModel.closePaywall(any(), any()) }
    }

    @Test
    fun `URL actions are simulated and SDK purchase restore navigation actions are preserved`() = runTest {
        val action = PaywallAction.External.NavigateTo(
            PaywallAction.External.NavigateTo.Destination.Url("https://example.com", ButtonComponent.UrlMethod.DEEP_LINK),
        )
        assertThat(interceptPreviewPaywallAction(action, provider, viewModel)).isTrue()
        coVerify { provider.handleAction(CustomerCenterPreviewAction.OpenUrl("https://example.com")) }

        listOf(
            PaywallAction.External.RestorePurchases,
            PaywallAction.External.PurchasePackage(null),
            PaywallAction.External.NavigateBack,
            PaywallAction.External.CloseWorkflow,
        ).forEach { assertThat(interceptPreviewPaywallAction(it, provider, viewModel)).isFalse() }
    }

    @Test
    fun `missing workflow completes loading and allows the offering paywall fallback`() = runTest {
        val offering = Offering("preview", "", emptyMap(), emptyList())
        coEvery { provider.workflow(offering) } returns null

        assertThat(loadPreviewWorkflow(provider, offering)).isEqualTo(PreviewWorkflowState.Loaded(null))
    }

    @Test
    fun `published workflow is preserved in the loaded state`() = runTest {
        val offering = Offering("preview", "", emptyMap(), emptyList())
        val workflow = mockk<CustomerCenterPreviewWorkflow>()
        coEvery { provider.workflow(offering) } returns workflow

        val state = loadPreviewWorkflow(provider, offering) as PreviewWorkflowState.Loaded

        assertThat(state.workflow).isSameAs(workflow)
    }

    @Test
    fun `workflow configuration errors preserve the reason supplied by the provider`() = runTest {
        val offering = Offering("preview", "", emptyMap(), emptyList())
        val error = PurchasesError(PurchasesErrorCode.ConfigurationError, "Selected workflow is unavailable")
        coEvery { provider.workflow(offering) } throws PurchasesException(error)

        val state = loadPreviewWorkflow(provider, offering) as PreviewWorkflowState.Error

        assertThat(state.error).isSameAs(error)
    }

    @Test
    fun `unexpected workflow provider failures become recoverable preview errors`() = runTest {
        val offering = Offering("preview", "", emptyMap(), emptyList())
        coEvery { provider.workflow(offering) } throws IllegalStateException("Invalid provider response")

        val state = loadPreviewWorkflow(provider, offering) as PreviewWorkflowState.Error

        assertThat(state.error.code).isEqualTo(PurchasesErrorCode.ConfigurationError)
    }

    @Test
    fun `dismissing a loading workflow preserves cancellation`() = runTest {
        val offering = Offering("preview", "", emptyMap(), emptyList())
        val cancellation = CancellationException("Preview dismissed")
        coEvery { provider.workflow(offering) } throws cancellation

        val failure = runCatching { loadPreviewWorkflow(provider, offering) }.exceptionOrNull()

        assertThat(failure).isSameAs(cancellation)
    }

    private fun checkout(autoDismiss: Boolean) = PaywallAction.External.LaunchWebCheckout(
        customUrl = null,
        openMethod = ButtonComponent.UrlMethod.EXTERNAL_BROWSER,
        autoDismiss = autoDismiss,
        paramBehavior = PaywallAction.External.LaunchWebCheckout.ParamBehavior.DoNotAppend,
    )
}
