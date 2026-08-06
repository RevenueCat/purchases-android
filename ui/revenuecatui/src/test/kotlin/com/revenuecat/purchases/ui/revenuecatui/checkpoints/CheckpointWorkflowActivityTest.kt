package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import android.content.Context
import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.launchActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CheckpointWorkflowActivityTest {

    @Test
    fun `activity finishes gracefully when the checkpoint call is unknown`() {
        // Simulates process death: the task is restored with the original callId extra, but the in-process
        // pending call died with the process.
        val intent = Intent(
            ApplicationProvider.getApplicationContext<Context>(),
            CheckpointWorkflowActivity::class.java,
        ).putExtra(CheckpointWorkflowActivity.EXTRA_CALL_ID, "call-id-from-a-previous-process")

        val scenario = launchActivity<CheckpointWorkflowActivity>(intent)

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
}
