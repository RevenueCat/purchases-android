package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class CheckpointGateResultMappingTest {

    @Test
    fun `every no-action reason maps to its gate counterpart`() {
        val reasons = listOf(
            CheckpointResult.NoAction.Reason.NO_MATCH to CheckpointGateResult.NoActionReason.NO_MATCH,
            CheckpointResult.NoAction.Reason.HOLDOUT to CheckpointGateResult.NoActionReason.HOLDOUT,
            CheckpointResult.NoAction.Reason.FREQUENCY_CAPPED to
                CheckpointGateResult.NoActionReason.FREQUENCY_CAPPED,
            CheckpointResult.NoAction.Reason.CONFIGURATION_UNAVAILABLE to
                CheckpointGateResult.NoActionReason.CONFIGURATION_UNAVAILABLE,
            CheckpointResult.NoAction.Reason.UNKNOWN_CHECKPOINT to
                CheckpointGateResult.NoActionReason.UNKNOWN_CHECKPOINT,
            CheckpointResult.NoAction.Reason.INVALID_CHECKPOINT_IDENTIFIER to
                CheckpointGateResult.NoActionReason.INVALID_CHECKPOINT_IDENTIFIER,
        )

        reasons.forEach { (reason, expected) ->
            val gateResult = CheckpointResult.NoAction(reason).toGateResult(activeEntitlementsBefore = null)

            assertThat(gateResult.noActionReason).isEqualTo(expected)
            assertThat(gateResult.entitlements).isEmpty()
            assertThat(gateResult.error).isNull()
        }
    }

    /**
     * The reason mapping cannot be compiler-exhaustive over a value-based constant class, so this guards the
     * seam instead: a [CheckpointResult.NoAction.Reason] constant added without a declared
     * [CheckpointGateResult.NoActionReason] counterpart falls into the mapping's pass-through branch and
     * produces a value that is not among the declared constants, failing here.
     */
    @Test
    fun `every declared no-action reason maps to a declared no-action gate reason`() {
        val declaredReasons = declaredConstants<CheckpointResult.NoAction.Reason>()
        val declaredNoActionReasons = declaredConstants<CheckpointGateResult.NoActionReason>()
        assertThat(declaredReasons).isNotEmpty

        declaredReasons.forEach { reason ->
            val gateResult = CheckpointResult.NoAction(reason).toGateResult(activeEntitlementsBefore = null)

            assertThat(declaredNoActionReasons).contains(gateResult.noActionReason)
        }
    }

    @Test
    fun `a purchase grants the entitlements that were not active before`() {
        val outcome = CheckpointPaywallOutcome.Purchased(customerInfoWithActive("pro", "plus"), mockk())

        val gateResult = CheckpointResult.PaywallPresented(outcome).toGateResult(setOf("plus"))

        assertThat(gateResult.entitlements).containsExactly(EntitlementGrant("pro"))
        assertThat(gateResult.noActionReason).isNull()
        assertThat(gateResult.error).isNull()
    }

    @Test
    fun `without a snapshot every active entitlement counts as granted, sorted`() {
        val outcome = CheckpointPaywallOutcome.Purchased(customerInfoWithActive("pro", "extra"), mockk())

        val gateResult = CheckpointResult.PaywallPresented(outcome).toGateResult(activeEntitlementsBefore = null)

        assertThat(gateResult.entitlements).containsExactly(
            EntitlementGrant("extra"),
            EntitlementGrant("pro"),
        )
    }

    @Test
    fun `a restore grants the entitlements that were not active before`() {
        val outcome = CheckpointPaywallOutcome.Restored(customerInfoWithActive("pro"))

        val gateResult = CheckpointResult.PaywallPresented(outcome).toGateResult(emptySet())

        assertThat(gateResult.entitlements).containsExactly(EntitlementGrant("pro"))
    }

    @Test
    fun `a finished app-owned presentation grants the entitlements that were not active before`() {
        val outcome = CheckpointPaywallOutcome.Finished(customerInfoWithActive("pro", "plus"))

        val gateResult = CheckpointResult.PaywallPresented(outcome).toGateResult(setOf("plus"))

        assertThat(gateResult.entitlements).containsExactly(EntitlementGrant("pro"))
        assertThat(gateResult.noActionReason).isNull()
        assertThat(gateResult.error).isNull()
    }

    @Test
    fun `a dismissed or web checkout outcome grants nothing but reports the workflow as presented`() {
        listOf(CheckpointPaywallOutcome.Dismissed, CheckpointPaywallOutcome.WebCheckoutOpened).forEach { outcome ->
            val gateResult = CheckpointResult.PaywallPresented(outcome).toGateResult(emptySet())

            assertThat(gateResult.entitlements).isEmpty()
            assertThat(gateResult.noActionReason).isNull()
            assertThat(gateResult.error).isNull()
        }
    }

    @Test
    fun `an in-workflow error keeps the workflow presented and carries the error`() {
        val error = PurchasesError(PurchasesErrorCode.StoreProblemError, "Simulated.")

        val gateResult = CheckpointResult.PaywallPresented(CheckpointPaywallOutcome.Error(error))
            .toGateResult(emptySet())

        assertThat(gateResult.noActionReason).isNull()
        assertThat(gateResult.error).isEqualTo(error)
        assertThat(gateResult.entitlements).isEmpty()
    }

    @Test
    fun `an unknown result maps to an error instead of crashing`() {
        val unknownResult = object : CheckpointResult() {}

        val gateResult = unknownResult.toGateResult(activeEntitlementsBefore = null)

        assertThat(gateResult.noActionReason).isEqualTo(CheckpointGateResult.NoActionReason.ERROR)
        assertThat(gateResult.error?.code).isEqualTo(PurchasesErrorCode.UnknownError)
    }

    @Test
    fun `an unknown paywall outcome grants nothing`() {
        val unknownOutcome = object : CheckpointPaywallOutcome() {}

        val gateResult = CheckpointResult.PaywallPresented(unknownOutcome).toGateResult(emptySet())

        assertThat(gateResult.entitlements).isEmpty()
        assertThat(gateResult.noActionReason).isNull()
        assertThat(gateResult.error).isNull()
    }

    private fun customerInfoWithActive(vararg identifiers: String): CustomerInfo = mockk {
        every { entitlements.active } returns identifiers.associateWith { mockk() }
    }

    // The constants are @JvmField vals in the companion, compiled to public static fields on the class.
    private inline fun <reified T : Any> declaredConstants(): List<T> =
        T::class.java.fields.filter { it.type == T::class.java }.map { field -> field.get(null) as T }
}
