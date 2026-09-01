package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class CheckpointGateResultMappingTest {

    @Before
    fun setup() {
        mockkObject(Logger)
        every { Logger.e(any()) } just runs
    }

    @After
    fun tearDown() {
        unmockkObject(Logger)
    }

    @Test
    fun `every no-action reason maps to its gate counterpart`() {
        val reasons = listOf(
            CheckpointResult.NoAction.Reason.NO_MATCH to CheckpointGateResult.NoWorkflowReason.NO_MATCH,
            CheckpointResult.NoAction.Reason.HOLDOUT to CheckpointGateResult.NoWorkflowReason.HOLDOUT,
            CheckpointResult.NoAction.Reason.FREQUENCY_CAPPED to
                CheckpointGateResult.NoWorkflowReason.FREQUENCY_CAPPED,
            CheckpointResult.NoAction.Reason.CONFIGURATION_UNAVAILABLE to
                CheckpointGateResult.NoWorkflowReason.CONFIGURATION_UNAVAILABLE,
            CheckpointResult.NoAction.Reason.UNKNOWN_CHECKPOINT to
                CheckpointGateResult.NoWorkflowReason.UNKNOWN_CHECKPOINT,
            CheckpointResult.NoAction.Reason.INVALID_CHECKPOINT_IDENTIFIER to
                CheckpointGateResult.NoWorkflowReason.INVALID_CHECKPOINT_IDENTIFIER,
        )

        reasons.forEach { (reason, expected) ->
            val gateResult = CheckpointResult.NoAction(reason).toGateResult(activeEntitlementsBefore = null)

            assertThat(gateResult.noWorkflowReason).isEqualTo(expected)
            assertThat(gateResult.entitlements).isEmpty()
            assertThat(gateResult.virtualCurrencies).isEmpty()
            assertThat(gateResult.error).isNull()
        }
    }

    @Test
    fun `a received offering maps to an error and does not expose the offering`() {
        val gateResult = CheckpointResult.ReceivedOffering(mockk()).toGateResult(activeEntitlementsBefore = null)

        assertThat(gateResult.noWorkflowReason).isEqualTo(CheckpointGateResult.NoWorkflowReason.ERROR)
        assertThat(gateResult.error?.code).isEqualTo(PurchasesErrorCode.ConfigurationError)
        assertThat(gateResult.entitlements).isEmpty()
    }

    @Test
    fun `a purchase grants the entitlements that were not active before`() {
        val outcome = CheckpointPaywallOutcome.Purchased(customerInfoWithActive("pro", "plus"), mockk())

        val gateResult = CheckpointResult.PaywallPresented(outcome).toGateResult(setOf("plus"))

        assertThat(gateResult.entitlements).containsExactly(EntitlementGrant("pro", GrantMethod.PURCHASED))
        assertThat(gateResult.noWorkflowReason).isNull()
        assertThat(gateResult.error).isNull()
    }

    @Test
    fun `without a snapshot every active entitlement counts as granted, sorted`() {
        val outcome = CheckpointPaywallOutcome.Purchased(customerInfoWithActive("pro", "extra"), mockk())

        val gateResult = CheckpointResult.PaywallPresented(outcome).toGateResult(activeEntitlementsBefore = null)

        assertThat(gateResult.entitlements).containsExactly(
            EntitlementGrant("extra", GrantMethod.PURCHASED),
            EntitlementGrant("pro", GrantMethod.PURCHASED),
        )
    }

    @Test
    fun `a restore grants with the restored method`() {
        val outcome = CheckpointPaywallOutcome.Restored(customerInfoWithActive("pro"))

        val gateResult = CheckpointResult.PaywallPresented(outcome).toGateResult(emptySet())

        assertThat(gateResult.entitlements).containsExactly(EntitlementGrant("pro", GrantMethod.RESTORED))
    }

    @Test
    fun `a dismissed or web checkout outcome grants nothing but reports the workflow as presented`() {
        listOf(CheckpointPaywallOutcome.Dismissed, CheckpointPaywallOutcome.WebCheckoutOpened).forEach { outcome ->
            val gateResult = CheckpointResult.PaywallPresented(outcome).toGateResult(emptySet())

            assertThat(gateResult.entitlements).isEmpty()
            assertThat(gateResult.noWorkflowReason).isNull()
            assertThat(gateResult.error).isNull()
        }
    }

    @Test
    fun `an in-workflow error keeps the workflow presented and carries the error`() {
        val error = PurchasesError(PurchasesErrorCode.StoreProblemError, "Simulated.")

        val gateResult = CheckpointResult.PaywallPresented(CheckpointPaywallOutcome.Error(error))
            .toGateResult(emptySet())

        assertThat(gateResult.noWorkflowReason).isNull()
        assertThat(gateResult.error).isEqualTo(error)
        assertThat(gateResult.entitlements).isEmpty()
    }

    @Test
    fun `an unknown result maps to an error instead of crashing`() {
        val unknownResult = object : CheckpointResult() {}

        val gateResult = unknownResult.toGateResult(activeEntitlementsBefore = null)

        assertThat(gateResult.noWorkflowReason).isEqualTo(CheckpointGateResult.NoWorkflowReason.ERROR)
        assertThat(gateResult.error?.code).isEqualTo(PurchasesErrorCode.UnknownError)
    }

    @Test
    fun `an unknown paywall outcome grants nothing`() {
        val unknownOutcome = object : CheckpointPaywallOutcome() {}

        val gateResult = CheckpointResult.PaywallPresented(unknownOutcome).toGateResult(emptySet())

        assertThat(gateResult.entitlements).isEmpty()
        assertThat(gateResult.noWorkflowReason).isNull()
        assertThat(gateResult.error).isNull()
    }

    private fun customerInfoWithActive(vararg identifiers: String): CustomerInfo = mockk {
        every { entitlements.active } returns identifiers.associateWith { mockk() }
    }
}
