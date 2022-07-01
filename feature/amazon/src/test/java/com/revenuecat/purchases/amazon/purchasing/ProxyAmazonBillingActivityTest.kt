package com.revenuecat.purchases.amazon.purchasing;

import android.os.Bundle
import android.os.Handler
import android.os.ResultReceiver
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.launchActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.amazon.helpers.PurchasingServiceProviderForTest
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProxyAmazonBillingActivityTest {

    val mockHandler = mockk<Handler>()

    @Test
    fun `Activity onCreate creates delegate`() {
        val expectedRequestIdString = "purchase_request_id"

        val purchasingServiceProviderForTest = PurchasingServiceProviderForTest().also {
            it.getPurchaseRequestId = expectedRequestIdString
        }
        val resultReceiver = object : ResultReceiver(mockHandler) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
            }
        }
        val intent = ProxyAmazonBillingActivity.newStartIntent(
            ApplicationProvider.getApplicationContext(),
            resultReceiver,
            "product_sku",
            purchasingServiceProviderForTest
        )

        launchActivity<ProxyAmazonBillingActivity>(intent).use { scenario ->
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.onActivity { activity ->
                assertThat(activity.proxyAmazonBillingDelegate).isNotNull
                // Testing onCreate is called
                assertThat(activity.proxyAmazonBillingDelegate?.broadcastReceiver).isNotNull
            }
        }
    }

    @Test
    fun `Activity onDestroy destroys delegate`() {
        val expectedRequestIdString = "purchase_request_id"

        val purchasingServiceProviderForTest = PurchasingServiceProviderForTest().also {
            it.getPurchaseRequestId = expectedRequestIdString
        }
        val resultReceiver = object : ResultReceiver(mockHandler) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
            }
        }
        val intent = ProxyAmazonBillingActivity.newStartIntent(
            ApplicationProvider.getApplicationContext(),
            resultReceiver,
            "product_sku",
            purchasingServiceProviderForTest
        )

        launchActivity<ProxyAmazonBillingActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                scenario.moveToState(Lifecycle.State.DESTROYED)
                assertThat(activity.proxyAmazonBillingDelegate).isNull()
                // Testing onCreate is called
                assertThat(activity.proxyAmazonBillingDelegate?.broadcastReceiver).isNull()
            }
        }
    }
}
