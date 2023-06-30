package com.revenuecat.purchases.amazon.handler

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.ResultReceiver
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amazon.device.iap.internal.model.PurchaseResponseBuilder
import com.amazon.device.iap.model.PurchaseResponse
import com.amazon.device.iap.model.Receipt
import com.amazon.device.iap.model.RequestId
import com.amazon.device.iap.model.UserData
import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.amazon.purchasing.ProxyAmazonBillingActivity
import com.revenuecat.purchases.amazon.purchasing.ProxyAmazonBillingActivityBroadcastReceiver
import com.revenuecat.purchases.amazon.helpers.PurchasingServiceProviderForTest
import com.revenuecat.purchases.amazon.helpers.dummyAmazonProduct
import com.revenuecat.purchases.amazon.helpers.dummyReceipt
import com.revenuecat.purchases.amazon.helpers.dummyUserData
import com.revenuecat.purchases.amazon.toStoreProduct
import com.revenuecat.purchases.models.StoreProduct
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.Exception
import java.lang.RuntimeException
import java.util.Date
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class PurchaseHandlerTest {

    private lateinit var underTest: PurchaseHandler
    private lateinit var purchasingServiceProvider: PurchasingServiceProviderForTest

    private var receivedReceipt: Receipt? = null
    private var receivedUserData: UserData? = null
    private var receivedError: PurchasesError? = null

    private var expectedOnSuccess: (Receipt, UserData) -> Unit = { receipt, userData ->
        receivedReceipt = receipt
        receivedUserData = userData
    }
    private var unexpectedOnSuccess: (Receipt, UserData) -> Unit = { _, _ ->
        Assertions.fail("should be error")
    }

    private var expectedOnError: (PurchasesError) -> Unit = {
        receivedError = it
    }

    private var unexpectedOnError: (PurchasesError) -> Unit = {
        Assertions.fail("should be success")
    }

    private var mockApplicationContext = mockk<Context>()
    private var mockHandler = mockk<Handler>()
    private var mockActivity = mockk<Activity>()
    private var broadcastIntentSlot = slot<Intent>()
    private var activityIntentSlot = slot<Intent>()
    private val packageName = UUID.randomUUID().toString()

    @Before
    fun setup() {
        purchasingServiceProvider = PurchasingServiceProviderForTest()
        underTest = PurchaseHandler(purchasingServiceProvider, mockApplicationContext)

        every {
            mockApplicationContext.packageName
        } returns packageName
        every {
            mockApplicationContext.sendBroadcast(capture(broadcastIntentSlot))
        } just runs

        every {
            mockActivity.startActivity(capture(activityIntentSlot))
        } just runs
        every {
            mockActivity.packageName
        } returns packageName
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `making a successful purchase returns the right values`() {
        val dummyRequestId = "a_request_id"
        purchasingServiceProvider.getPurchaseRequestId = dummyRequestId

        underTest.purchase(
            mockHandler,
            mockActivity,
            appUserID = "app_user_id",
            storeProduct = dummyStoreProduct(),
            presentedOfferingIdentifier = null,
            expectedOnSuccess,
            unexpectedOnError
        )

        verifyActivityIsStartedAndFakeRequestId(dummyRequestId)

        val response = getDummyPurchaseResponse(dummyRequestId)
        underTest.onPurchaseResponse(response)

        assertThat(receivedReceipt).isNotNull
        assertThat(receivedReceipt).isEqualTo(response.receipt)
        assertThat(receivedUserData).isEqualTo(response.userData)
    }

    @Test
    fun `purchase failed sends a cancelled error`() {
        val dummyRequestId = "a_request_id"
        purchasingServiceProvider.getPurchaseRequestId = dummyRequestId

        underTest.purchase(
            mockHandler,
            mockActivity,
            appUserID = "app_user_id",
            storeProduct = dummyStoreProduct(),
            presentedOfferingIdentifier = null,
            unexpectedOnSuccess,
            expectedOnError
        )

        verifyActivityIsStartedAndFakeRequestId(dummyRequestId)

        val response = getDummyPurchaseResponse(dummyRequestId, PurchaseResponse.RequestStatus.FAILED)
        underTest.onPurchaseResponse(response)

        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.PurchaseCancelledError)
    }

    @Test
    fun `INVALID_SKU response when making a purchase sends appropriate error`() {
        val dummyRequestId = "a_request_id"
        purchasingServiceProvider.getPurchaseRequestId = dummyRequestId

        underTest.purchase(
            mockHandler,
            mockActivity,
            appUserID = "app_user_id",
            storeProduct = dummyStoreProduct(),
            presentedOfferingIdentifier = null,
            unexpectedOnSuccess,
            expectedOnError
        )

        verifyActivityIsStartedAndFakeRequestId(dummyRequestId)

        val response = getDummyPurchaseResponse(dummyRequestId, PurchaseResponse.RequestStatus.INVALID_SKU)
        underTest.onPurchaseResponse(response)

        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.ProductNotAvailableForPurchaseError)
    }

    @Test
    fun `ALREADY_PURCHASED response when making a purchase sends appropriate error`() {
        val dummyRequestId = "a_request_id"
        purchasingServiceProvider.getPurchaseRequestId = dummyRequestId

        underTest.purchase(
            mockHandler,
            mockActivity,
            appUserID = "app_user_id",
            storeProduct = dummyStoreProduct(),
            presentedOfferingIdentifier = null,
            unexpectedOnSuccess,
            expectedOnError
        )

        verifyActivityIsStartedAndFakeRequestId(dummyRequestId)

        val response = getDummyPurchaseResponse(dummyRequestId, PurchaseResponse.RequestStatus.ALREADY_PURCHASED)
        underTest.onPurchaseResponse(response)

        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.ProductAlreadyPurchasedError)
    }

    @Test
    fun `NOT_SUPPORTED response when making a purchase sends appropriate error`() {
        val dummyRequestId = "a_request_id"
        purchasingServiceProvider.getPurchaseRequestId = dummyRequestId

        underTest.purchase(
            mockHandler,
            mockActivity,
            appUserID = "app_user_id",
            storeProduct = dummyStoreProduct(),
            presentedOfferingIdentifier = null,
            unexpectedOnSuccess,
            expectedOnError
        )

        verifyActivityIsStartedAndFakeRequestId(dummyRequestId)

        val response = getDummyPurchaseResponse(dummyRequestId, PurchaseResponse.RequestStatus.NOT_SUPPORTED)
        underTest.onPurchaseResponse(response)

        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
    }

    @Test
    fun `callbacks are invoked just once`() {
        val dummyRequestId = "a_request_id"
        purchasingServiceProvider.getPurchaseRequestId = dummyRequestId

        var receivedCount = 0

        underTest.purchase(
            mockHandler,
            mockActivity,
            appUserID = "app_user_id",
            storeProduct = dummyStoreProduct(),
            presentedOfferingIdentifier = null,
            onSuccess = { _, _ ->
                receivedCount++
            },
            unexpectedOnError
        )

        verifyActivityIsStartedAndFakeRequestId(dummyRequestId)

        val response = getDummyPurchaseResponse(dummyRequestId)
        underTest.onPurchaseResponse(response)
        underTest.onPurchaseResponse(response)

        assertThat(receivedCount).isOne
    }

    @Test
    fun `Exceptions are logged so they are not swallowed by Amazon`() {
        val expectedException = RuntimeException("")
        var receivedException: Throwable? = null
        var receivedLoggedException: Throwable? = null
        Purchases.logHandler = object : LogHandler {
            override fun v(tag: String, msg: String) {
            }

            override fun d(tag: String, msg: String) {
            }

            override fun i(tag: String, msg: String) {
            }

            override fun w(tag: String, msg: String) {
            }

            override fun e(tag: String, msg: String, throwable: Throwable?) {
                receivedLoggedException = throwable
            }
        }
        val dummyRequestId = "a_request_id"
        purchasingServiceProvider.getPurchaseRequestId = dummyRequestId

        underTest.purchase(
            mockHandler,
            mockActivity,
            appUserID = "app_user_id",
            storeProduct = dummyStoreProduct(),
            presentedOfferingIdentifier = null,
            onSuccess = { _, _ -> throw expectedException },
            unexpectedOnError
        )

        verifyActivityIsStartedAndFakeRequestId(dummyRequestId)

        val response = getDummyPurchaseResponse(dummyRequestId)
        try {
            underTest.onPurchaseResponse(response)
        } catch (e: Exception) {
            receivedException = e
        }

        assertThat(receivedException).isNotNull()
        assertThat(receivedLoggedException).isNotNull()
        assertThat(expectedException).isEqualTo(receivedException)
        assertThat(expectedException).isEqualTo(receivedLoggedException)
    }

    @Test
    fun `broadcast is sent after finishing a successful purchase`() {
        val dummyRequestId = "a_request_id"
        purchasingServiceProvider.getPurchaseRequestId = dummyRequestId

        underTest.purchase(
            mockHandler,
            mockActivity,
            appUserID = "app_user_id",
            storeProduct = dummyStoreProduct(),
            presentedOfferingIdentifier = null,
            expectedOnSuccess,
            unexpectedOnError
        )

        verifyActivityIsStartedAndFakeRequestId(dummyRequestId)

        val response = getDummyPurchaseResponse(dummyRequestId)
        underTest.onPurchaseResponse(response)

        assertThat(receivedReceipt).isNotNull
        assertThat(broadcastIntentSlot.isCaptured).isTrue
        val captured = broadcastIntentSlot.captured
        assertThat(captured.`package`).isEqualTo(packageName)
        assertThat(captured.action).isEqualTo(ProxyAmazonBillingActivityBroadcastReceiver.PURCHASE_FINISHED_ACTION)
    }

    @Test
    fun `broadcast is sent after finishing a failed purchase`() {
        val dummyRequestId = "a_request_id"
        purchasingServiceProvider.getPurchaseRequestId = dummyRequestId

        underTest.purchase(
            mockHandler,
            mockActivity,
            appUserID = "app_user_id",
            storeProduct = dummyStoreProduct(),
            presentedOfferingIdentifier = null,
            unexpectedOnSuccess,
            expectedOnError
        )

        verifyActivityIsStartedAndFakeRequestId(dummyRequestId)

        val response = getDummyPurchaseResponse(dummyRequestId, PurchaseResponse.RequestStatus.FAILED)
        underTest.onPurchaseResponse(response)

        assertThat(receivedError).isNotNull
        assertThat(broadcastIntentSlot.isCaptured).isTrue
        val captured = broadcastIntentSlot.captured
        assertThat(captured.`package`).isEqualTo(packageName)
        assertThat(captured.action).isEqualTo(ProxyAmazonBillingActivityBroadcastReceiver.PURCHASE_FINISHED_ACTION)
    }

    private fun dummyStoreProduct(): StoreProduct {
        return dummyAmazonProduct().toStoreProduct("US")!!
    }

    private fun getDummyPurchaseResponse(
        expectedRequestId: String = "${System.currentTimeMillis()}",
        requestStatus: PurchaseResponse.RequestStatus = PurchaseResponse.RequestStatus.SUCCESSFUL
    ): PurchaseResponse {
        val userData = dummyUserData()

        val receipt = dummyReceipt(purchaseDate = Date())

        val builder = PurchaseResponseBuilder()
            .setRequestId(RequestId.fromString(expectedRequestId))
            .setRequestStatus(requestStatus)
            .setUserData(userData)
            .setReceipt(receipt)

        return PurchaseResponse(builder)
    }

    private fun verifyActivityIsStartedAndFakeRequestId(dummyRequestId: String) {
        assertThat(activityIntentSlot.isCaptured).isTrue
        val resultReceiver = activityIntentSlot.captured.getParcelableExtra<ResultReceiver>("result_receiver")
        val bundle = Bundle().apply {
            putParcelable(ProxyAmazonBillingActivity.EXTRAS_REQUEST_ID, RequestId.fromString(dummyRequestId))
        }
        assertThat(resultReceiver).isNotNull
        resultReceiver!!.send(0, bundle)
    }
}
