package com.revenuecat.purchases.amazon;

import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.launchActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProxyAmazonBillingActivityTest {

    @Before
    fun setup() {
    }

    @Test
    fun `Activity onCreate`() {
        launchActivity<ProxyAmazonBillingActivity>().use { scenario ->
            scenario.moveToState(Lifecycle.State.CREATED)
        }
    }

}
