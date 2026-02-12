package com.revenuecat.purchases.amazon.purchasing;

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.ResultReceiver
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amazon.device.iap.model.RequestId
import com.revenuecat.purchases.amazon.PurchasingServiceProvider
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProxyAmazonBillingDelegateTest {

    private lateinit var underTest: ProxyAmazonBillingDelegate
    private lateinit var mockActivity: Activity
    private lateinit var mockApplicationContext: Context
    private lateinit var mockResultReceiver: ResultReceiver
    private lateinit var mockPurchasingServiceProvider: PurchasingServiceProvider
    private lateinit var fakePurchaseRequestId: RequestId

    private lateinit var registeredBroadcastReceiverSlot: CapturingSlot<ProxyAmazonBillingActivityBroadcastReceiver>
    private lateinit var unregisteredBroadcastReceiverSlot: CapturingSlot<ProxyAmazonBillingActivityBroadcastReceiver>
    private lateinit var intentFilterSlot: CapturingSlot<IntentFilter>
    private lateinit var sentBundleSlot: CapturingSlot<Bundle>

    private val expectedSku = "sku"

    @Before
    fun setup() {
        mockActivity = mockk()
        mockApplicationContext = mockk()
        mockResultReceiver = mockk()
        mockPurchasingServiceProvider = mockk()

        fakePurchaseRequestId = RequestId.fromString("${System.currentTimeMillis()}")

        intentFilterSlot = slot()
        registeredBroadcastReceiverSlot = slot()
        unregisteredBroadcastReceiverSlot = slot()
        sentBundleSlot = slot()

        mockActivity()

        every {
            mockPurchasingServiceProvider.purchase(expectedSku)
        } returns fakePurchaseRequestId

        every {
            mockResultReceiver.send(0, capture(sentBundleSlot))
        } just runs

        underTest = ProxyAmazonBillingDelegate()
    }

    @Test
    fun `onCreate registers BroadcastReceiver`() {
        underTest.onCreate(mockActivity, null)
        assertThat(registeredBroadcastReceiverSlot.isCaptured).isTrue
        assertThat(registeredBroadcastReceiverSlot.captured).isEqualTo(underTest.broadcastReceiver)
    }

    @Test
    fun `onDestroy unregisters BroadcastReceiver`() {
        underTest.onCreate(mockActivity, null)
        assertThat(underTest.broadcastReceiver).isNotNull

        underTest.onDestroy(mockActivity)
        assertThat(unregisteredBroadcastReceiverSlot.isCaptured).isTrue
        assertThat(registeredBroadcastReceiverSlot.captured).isEqualTo(unregisteredBroadcastReceiverSlot.captured)
        assertThat(underTest.broadcastReceiver).isNull()
    }

    @Test
    fun `onCreate starts purchase`() {
        underTest.onCreate(mockActivity, null)
        verify(exactly = 1) {
            mockPurchasingServiceProvider.purchase(expectedSku)
        }
    }

    @Test
    fun `onCreate sends RequestId`() {
        underTest.onCreate(mockActivity, null)
        assertThat(sentBundleSlot.isCaptured).isTrue

        val sentRequestId =
            sentBundleSlot.captured.getParcelable<RequestId>(ProxyAmazonBillingActivity.EXTRAS_REQUEST_ID)
        assertThat(sentRequestId).isEqualTo(fakePurchaseRequestId)
    }

    @Test
    fun `onCreate finishes activity if sku is missing`() {
        val newStartIntent = ProxyAmazonBillingActivity.newStartIntent(
            mockActivity,
            mockResultReceiver,
            expectedSku,
            mockPurchasingServiceProvider
        ).also {
            it.removeExtra(ProxyAmazonBillingActivity.EXTRAS_SKU)
        }
        every {
            mockActivity.intent
        } returns newStartIntent

        underTest.onCreate(mockActivity, null)
        assertThat(sentBundleSlot.isCaptured).isFalse
        verify(exactly = 1) {
            mockActivity.finish()
        }
        verify(exactly = 0) {
            mockPurchasingServiceProvider.purchase(any())
        }
    }

    @Test
    fun `onCreate finishes activity if ResultReceiver is missing`() {
        val newStartIntent = ProxyAmazonBillingActivity.newStartIntent(
            mockActivity,
            mockResultReceiver,
            expectedSku,
            mockPurchasingServiceProvider
        ).also {
            it.removeExtra(ProxyAmazonBillingActivity.EXTRAS_RESULT_RECEIVER)
        }
        every {
            mockActivity.intent
        } returns newStartIntent

        underTest.onCreate(mockActivity, null)
        assertThat(sentBundleSlot.isCaptured).isFalse
        verify(exactly = 1) {
            mockActivity.finish()
        }
        verify(exactly = 0) {
            mockPurchasingServiceProvider.purchase(any())
        }
    }

    @Test
    fun `onCreate finishes activity if PurchasingServiceProvider is missing`() {
        val newStartIntent = ProxyAmazonBillingActivity.newStartIntent(
            mockActivity,
            mockResultReceiver,
            expectedSku,
            mockPurchasingServiceProvider
        ).also {
            it.removeExtra(ProxyAmazonBillingActivity.EXTRAS_PURCHASING_SERVICE_PROVIDER)
        }
        every {
            mockActivity.intent
        } returns newStartIntent

        underTest.onCreate(mockActivity, null)
        assertThat(sentBundleSlot.isCaptured).isFalse
        verify(exactly = 1) {
            mockActivity.finish()
        }
        verify(exactly = 0) {
            mockPurchasingServiceProvider.purchase(any())
        }
    }

    @Test
    fun `onCreate starts purchase and sends RequestId`() {
        underTest.onCreate(mockActivity, null)
        assertThat(sentBundleSlot.isCaptured).isTrue

        val sentRequestId =
            sentBundleSlot.captured.getParcelable<RequestId>(ProxyAmazonBillingActivity.EXTRAS_REQUEST_ID)
        assertThat(sentRequestId).isEqualTo(fakePurchaseRequestId)
    }

    @Test
    fun `onCreate does not start purchase if savedInstanceState is not null`() {
        underTest.onCreate(mockActivity, Bundle())
        verify(exactly = 0) {
            mockPurchasingServiceProvider.purchase(any())
        }
    }

    private fun mockActivity() {
        every {
            mockActivity.finish()
        } just runs
        every {
            mockActivity.applicationContext
        } returns mockApplicationContext
        every {
            mockActivity.packageName
        } returns "package.name"
        every {
            mockActivity.intent
        } returns ProxyAmazonBillingActivity.newStartIntent(
            mockActivity,
            mockResultReceiver,
            expectedSku,
            mockPurchasingServiceProvider
        )
        every {
            mockActivity.registerReceiver(
                capture(registeredBroadcastReceiverSlot),
                capture(intentFilterSlot),
                Context.RECEIVER_EXPORTED,
            )
        } returns Intent()
        every {
            mockActivity.unregisterReceiver(capture(unregisteredBroadcastReceiverSlot))
        } just runs
    }
}
