package com.revenuecat.purchases.amazon.purchasing;

import android.app.Activity
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProxyAmazonBillingActivityBroadcastReceiverTest {

    private lateinit var underTest: ProxyAmazonBillingActivityBroadcastReceiver
    private lateinit var mockActivity: Activity

    @Before
    fun setup() {
        mockActivity = mockk()
        every {
            mockActivity.finish()
        } just runs
        underTest = ProxyAmazonBillingActivityBroadcastReceiver(mockActivity)
    }

    @Test
    fun `ProxyAmazonBillingActivityBroadcastReceiver finishes Activity onReceive`() {
        underTest.onReceive(ApplicationProvider.getApplicationContext(), Intent())
        verify {
            mockActivity.finish()
        }
    }

}
