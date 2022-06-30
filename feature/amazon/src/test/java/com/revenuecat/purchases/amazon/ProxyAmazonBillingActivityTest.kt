package com.revenuecat.purchases.amazon;

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.ResultReceiver
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.launchActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amazon.device.iap.model.RequestId
import com.revenuecat.purchases.amazon.helpers.PurchasingServiceProviderForTest
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProxyAmazonBillingActivityTest {

    val mockHandler = mockk<Handler>()

    @Test
    fun `Activity onCreate sends request id to result receiver`() {
        var receivedRequestId: RequestId? = null
        val expectedRequestIdString = "purchase_request_id"

        val purchasingServiceProviderForTest = PurchasingServiceProviderForTest().also {
            it.getPurchaseRequestId = expectedRequestIdString
        }
        val intent = Intent(ApplicationProvider.getApplicationContext(), ProxyAmazonBillingActivity::class.java).also {
            it.putExtra("result_receiver", object : ResultReceiver(mockHandler) {
                override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                    receivedRequestId = resultData?.get("request_id") as? RequestId
                }
            })
            it.putExtra("sku", "product_sku")
            it.putExtra("service_provider", purchasingServiceProviderForTest)
        }

        launchActivity<ProxyAmazonBillingActivity>(intent).use { scenario ->
            scenario.moveToState(Lifecycle.State.CREATED)
        }
        assertThat(receivedRequestId).isNotNull
        assertThat(receivedRequestId).isEqualTo(RequestId.fromString(expectedRequestIdString))
    }

    @Test
    fun `Activity registers BroadcastReceiver`() {
        val applicationContext = ApplicationProvider.getApplicationContext<Context>()
        val expectedRequestIdString = "purchase_request_id"
        val purchasingServiceProviderForTest = PurchasingServiceProviderForTest().also {
            it.getPurchaseRequestId = expectedRequestIdString
        }

        val intent = Intent(applicationContext, ProxyAmazonBillingActivity::class.java).also {
            it.putExtra("result_receiver", object : ResultReceiver(mockHandler) {
                override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                }
            })
            it.putExtra("sku", "product_sku")
            it.putExtra("service_provider", purchasingServiceProviderForTest)
        }

        val broadcastIntent = Intent().also {
            it.action = "purchase_finished"
            it.setPackage(applicationContext.packageName)
        }

        launchActivity<ProxyAmazonBillingActivity>(intent).use { scenario ->
            scenario.moveToState(Lifecycle.State.CREATED)
            applicationContext.sendBroadcast(broadcastIntent)
            scenario.onActivity { activity ->
                assertThat(activity.broadcastReceiver).isNotNull
                assertThat(activity.broadcastReceiver!!.onReceiveCalled).isTrue
            }
        }
    }
}
