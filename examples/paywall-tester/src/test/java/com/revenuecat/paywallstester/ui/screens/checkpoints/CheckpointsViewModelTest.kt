@file:OptIn(InviteOnlyCheckpointsAPI::class)

package com.revenuecat.paywallstester.ui.screens.checkpoints

import com.revenuecat.paywallstester.data.RecentCheckpointsStore
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.ui.revenuecatui.InviteOnlyCheckpointsAPI
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointParams
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointPassedCallback
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointResult
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.FlowPresentationMode
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.FlowResult
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.PaywallPresenter
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.checkpoint
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CheckpointsViewModelTest {
    private val callbacks = mutableListOf<CheckpointPassedCallback>()
    private val params = mutableListOf<CheckpointParams>()
    private lateinit var viewModel: CheckpointsViewModelImpl

    @Before
    fun setUp() {
        val purchases = mockk<Purchases>()
        mockkObject(Purchases.Companion)
        every { Purchases.sharedInstance } returns purchases
        mockkStatic("com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointsExtensionsKt")
        every { purchases.checkpoint(any(), any(), any()) } answers {
            params.add(arg(2))
            callbacks.add(arg(3))
        }
        val recents = mockk<RecentCheckpointsStore> {
            every { recents() } returns emptyList()
            every { recordUse(any()) } answers { listOf(firstArg()) }
        }
        viewModel = CheckpointsViewModelImpl(recents)
    }

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `content stays locked without a completion callback`() {
        assertFalse(viewModel.state.value.checkpointCompleted)

        viewModel.hit("hard_gate")

        assertFalse(viewModel.state.value.checkpointCompleted)
        assertEquals("hard_gate", viewModel.state.value.waitingFor)
        assertNull(viewModel.state.value.lastResult)
    }

    @Test
    fun `a soft checkpoint callback unlocks content without any grants`() {
        viewModel.hit("soft_gate")

        callbacks.single().onCheckpointPassed(result())

        assertTrue(viewModel.state.value.checkpointCompleted)
        assertNull(viewModel.state.value.waitingFor)
        assertNotNull(viewModel.state.value.lastResult)
    }

    @Test
    fun `a callback with no presented flow also unlocks content`() {
        viewModel.hit("no_action")

        callbacks.single().onCheckpointPassed(result(flow = null))

        assertTrue(viewModel.state.value.checkpointCompleted)
        assertTrue(viewModel.state.value.lastResult!!.raw.contains("flow=null"))
    }

    @Test
    fun `reset relocks content and clears diagnostics while preserving options and recents`() {
        viewModel.setPresentationMode(FlowPresentationMode.MODAL_SHEET)
        viewModel.setPresentWithAppPaywall(true)
        viewModel.hit("soft_gate")
        callbacks.single().onCheckpointPassed(result())

        viewModel.resetTest()

        assertFalse(viewModel.state.value.checkpointCompleted)
        assertNull(viewModel.state.value.waitingFor)
        assertNull(viewModel.state.value.lastResult)
        assertEquals(listOf("soft_gate"), viewModel.state.value.recents)
        assertEquals(FlowPresentationMode.MODAL_SHEET, viewModel.state.value.presentationMode)
        assertTrue(viewModel.state.value.presentWithAppPaywall)
    }

    @Test
    fun `a callback from before reset cannot unlock content`() {
        viewModel.hit("hard_gate")
        viewModel.resetTest()

        callbacks.single().onCheckpointPassed(result())

        assertFalse(viewModel.state.value.checkpointCompleted)
        assertNull(viewModel.state.value.lastResult)
    }

    @Test
    fun `starting a new attempt relocks previously completed content`() {
        viewModel.hit("soft_gate")
        callbacks.single().onCheckpointPassed(result())

        viewModel.hit("hard_gate")

        assertFalse(viewModel.state.value.checkpointCompleted)
        assertEquals("hard_gate", viewModel.state.value.waitingFor)
        assertNull(viewModel.state.value.lastResult)
    }

    @Test
    fun `an old callback cannot unlock a newer attempt`() {
        viewModel.hit("first")
        viewModel.hit("second")

        callbacks.first().onCheckpointPassed(result())

        assertFalse(viewModel.state.value.checkpointCompleted)
        assertEquals("second", viewModel.state.value.waitingFor)
        assertNull(viewModel.state.value.lastResult)
        callbacks.last().onCheckpointPassed(result())
        assertTrue(viewModel.state.value.checkpointCompleted)
    }

    @Test
    fun `an old callback cannot replace the latest completed result`() {
        viewModel.hit("first")
        viewModel.hit("second")
        callbacks.last().onCheckpointPassed(result(flow = null))
        val latestResult = viewModel.state.value.lastResult

        callbacks.first().onCheckpointPassed(result())

        assertTrue(viewModel.state.value.checkpointCompleted)
        assertEquals(latestResult, viewModel.state.value.lastResult)
    }

    @Test
    fun `a blank identifier does not relock completed content`() {
        viewModel.hit("soft_gate")
        callbacks.single().onCheckpointPassed(result())

        viewModel.hit("   ")

        assertTrue(viewModel.state.value.checkpointCompleted)
        assertEquals(1, callbacks.size)
    }

    @Test
    fun `backing out of an app-owned paywall leaves content locked`() {
        viewModel.setPresentWithAppPaywall(true)
        viewModel.hit("gate")
        val presenter = params.single().paywallPresenter!!
        presenter.present(mockk(), PaywallPresenter.Completion {})

        viewModel.paywallRequest.value!!.finish(PaywallPresenter.Completion.Result.NavigatedBack)

        assertFalse(viewModel.state.value.checkpointCompleted)
        assertNull(viewModel.state.value.waitingFor)
        assertNotNull(viewModel.state.value.lastResult)
    }

    @Test
    fun `backing out of a reset app-owned paywall does not restore old diagnostics`() {
        viewModel.setPresentWithAppPaywall(true)
        viewModel.hit("gate")
        val presenter = params.single().paywallPresenter!!
        presenter.present(mockk(), PaywallPresenter.Completion {})
        viewModel.resetTest()

        viewModel.paywallRequest.value!!.finish(PaywallPresenter.Completion.Result.NavigatedBack)

        assertFalse(viewModel.state.value.checkpointCompleted)
        assertNull(viewModel.state.value.lastResult)
    }

    @Test
    fun `an old app paywall cannot replace a newer attempt or its presentation`() {
        viewModel.setPresentWithAppPaywall(true)
        viewModel.hit("first")
        params.first().paywallPresenter!!.present(mockk(), PaywallPresenter.Completion {})
        val oldRequest = viewModel.paywallRequest.value!!
        viewModel.resetTest()
        viewModel.hit("second")
        params.last().paywallPresenter!!.present(mockk(), PaywallPresenter.Completion {})
        val newRequest = viewModel.paywallRequest.value!!

        oldRequest.finish(PaywallPresenter.Completion.Result.NavigatedBack)

        assertFalse(viewModel.state.value.checkpointCompleted)
        assertEquals("second", viewModel.state.value.waitingFor)
        assertNull(viewModel.state.value.lastResult)
        assertSame(newRequest, viewModel.paywallRequest.value)
    }

    private fun result(flow: FlowResult? = mockk {
        every { obtainedEntitlements } returns emptySet()
        every { obtainedProducts } returns emptySet()
    }): CheckpointResult {
        val customer = mockk<CustomerInfo> {
            every { entitlements.active } returns emptyMap()
            every { originalAppUserId } returns "test-user"
        }
        return mockk {
            every { this@mockk.flow } returns flow
            every { this@mockk.customer } returns customer
        }
    }
}
