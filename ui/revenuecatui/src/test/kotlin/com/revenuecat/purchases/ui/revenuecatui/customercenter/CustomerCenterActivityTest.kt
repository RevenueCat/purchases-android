package com.revenuecat.purchases.ui.revenuecatui.customercenter

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
class CustomerCenterActivityTest {

    @Test
    fun `activity finishes gracefully when not launched through SDK`() {
        // Arrange - launch without SDK extras (simulating Google automated testing)
        val intent = Intent(
            ApplicationProvider.getApplicationContext<Context>(),
            CustomerCenterActivity::class.java,
        )

        // Act - launch the activity (it should finish immediately in onCreate)
        val scenario = launchActivity<CustomerCenterActivity>(intent)

        // Assert - activity should be destroyed (finished gracefully without crashing)
        assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
    }
}
