package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PresentationSlotTest {

    private val slot = PresentationSlot()

    @Test
    fun `the slot is claimed by the first call only`() {
        val first = pendingCall("first")

        assertThat(slot.claim(first)).isTrue
        assertThat(slot.claim(pendingCall("second"))).isFalse
        assertThat(slot.with("first") { it }).isSameAs(first)
        assertThat(slot.with("second") { it }).isNull()
    }

    @Test
    fun `taking the call releases the slot only for a matching id`() {
        val call = pendingCall("call")
        slot.claim(call)

        assertThat(slot.take("other")).isNull()
        assertThat(slot.claim(pendingCall("second"))).isFalse

        assertThat(slot.take("call")).isSameAs(call)
        assertThat(slot.with("call") { it }).isNull()
        assertThat(slot.claim(pendingCall("second"))).isTrue
    }

    @Test
    fun `a stale id does not reach the call that replaced it`() {
        slot.claim(pendingCall("stale"))
        slot.take("stale")
        val current = pendingCall("current")
        slot.claim(current)

        val outcome = CheckpointFlowOutcome.Dismissed
        assertThat(slot.with("stale") { it.outcome = outcome }).isNull()
        assertThat(slot.take("stale")).isNull()
        assertThat(current.outcome).isNull()
        assertThat(slot.with("current") { it }).isSameAs(current)
    }

    private fun pendingCall(callId: String) = PresentationSlot.PendingCall(
        callId = callId,
        content = CheckpointFlowContent.OfferingFlow(mockk()),
        customVariables = emptyMap(),
        flowFinished = CompletableDeferred(),
    )
}
