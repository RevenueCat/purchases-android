@file:OptIn(ExperimentalCoroutinesApi::class)

package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.launchActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.checkpoints.CheckpointResolution
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CheckpointWorkflowActivityTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private var managerSlot: Any? = null
    private lateinit var mockPurchases: Purchases

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        mockPurchases = mockk {
            every { checkpointManagerSlot } answers { managerSlot }
            every { checkpointManagerSlot = any() } answers { managerSlot = firstArg() }
        }
        mockkObject(Purchases)
        every { Purchases.isConfigured } returns true
        every { Purchases.sharedInstance } returns mockPurchases
    }

    @After
    fun tearDown() {
        unmockkObject(Purchases)
        Dispatchers.resetMain()
    }

    @Test
    fun `activity finishes gracefully when the checkpoint call is unknown`() {
        // Simulates process death: the task is restored with the original callId extra, but the in-process
        // pending call died with the process.
        val scenario = launchActivity<CheckpointWorkflowActivity>(
            intentFor("call-id-from-a-previous-process"),
        )

        assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
    }

    @Test
    fun `activity finishes gracefully when launched without a callId`() {
        val intent = Intent(
            ApplicationProvider.getApplicationContext<Context>(),
            CheckpointWorkflowActivity::class.java,
        )

        val scenario = launchActivity<CheckpointWorkflowActivity>(intent)

        assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
    }

    @Test
    fun `activity finishes gracefully when the SDK is not configured`() {
        every { Purchases.isConfigured } returns false

        val scenario = launchActivity<CheckpointWorkflowActivity>(intentFor("any-call-id"))

        assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
    }

    @Test
    fun `an activity restored with a stale callId does not release the pending call that replaced it`() {
        val liveCallId = startPendingCall()

        launchActivity<CheckpointWorkflowActivity>(intentFor("call-id-from-a-previous-process"))

        assertThat(mockPurchases.checkpointsManager.presentation(liveCallId)).isNotNull
    }

    private fun intentFor(callId: String) = Intent(
        ApplicationProvider.getApplicationContext<Context>(),
        CheckpointWorkflowActivity::class.java,
    ).putExtra(CheckpointWorkflowActivity.EXTRA_CALL_ID, callId)

    // Drives a real checkpoint through the instance's manager so a genuine pending call exists, and returns
    // the callId that manager put on the launch Intent.
    private fun startPendingCall(): String {
        val launchedIntent = slot<Intent>()
        val launchingActivity = mockk<Activity>(relaxed = true) {
            every { startActivity(capture(launchedIntent)) } just runs
        }
        every { mockPurchases.currentActivity } returns launchingActivity
        coEvery { mockPurchases.resolveCheckpoint(any(), any()) } returns
            CheckpointResolution.MatchedWorkflow(mockk(), mockk(), mockk(), checkpointRuleId = null)

        CoroutineScope(dispatcher).launch {
            mockPurchases.checkpointsManager.checkpoint(mockPurchases, "test_checkpoint", null)
        }

        verify { launchingActivity.startActivity(any()) }
        return launchedIntent.captured.getStringExtra(CheckpointWorkflowActivity.EXTRA_CALL_ID)!!
    }
}
