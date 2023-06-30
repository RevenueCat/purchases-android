package com.revenuecat.purchases.amazon.handler

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amazon.device.iap.internal.model.PurchaseUpdatesResponseBuilder
import com.amazon.device.iap.internal.model.ReceiptBuilder
import com.amazon.device.iap.model.ProductType
import com.amazon.device.iap.model.PurchaseUpdatesResponse
import com.amazon.device.iap.model.Receipt
import com.amazon.device.iap.model.RequestId
import com.amazon.device.iap.model.UserData
import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.amazon.helpers.MockDeviceCache
import com.revenuecat.purchases.amazon.helpers.PurchasingServiceProviderForTest
import com.revenuecat.purchases.amazon.helpers.dummyUserData
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

@RunWith(AndroidJUnit4::class)
class PurchaseUpdatesHandlerTest {

    private lateinit var underTest: PurchaseUpdatesHandler
    private val apiKey = "api_key"
    private lateinit var cache: MockDeviceCache
    private lateinit var purchasingServiceProvider: PurchasingServiceProviderForTest

    private var receivedReceipts: List<Receipt>? = null
    private var receivedUserData: UserData? = null
    private var receivedError: PurchasesError? = null

    private var expectedOnSuccess: (List<Receipt>, UserData) -> Unit = { receipts, userData ->
        receivedReceipts = receipts
        receivedUserData = userData
    }
    private var unexpectedOnSuccess: (List<Receipt>, UserData) -> Unit = { _, _ ->
        fail("should be error")
    }

    private var expectedOnError: (PurchasesError) -> Unit = {
        receivedError = it
    }

    private var unexpectedOnError: (PurchasesError) -> Unit = {
        fail("should be success")
    }

    @Before
    fun setup() {
        cache = MockDeviceCache(mockk(), apiKey)
        purchasingServiceProvider = PurchasingServiceProviderForTest()
        underTest = PurchaseUpdatesHandler(purchasingServiceProvider)
    }

    @Test
    fun `successfully queries purchases`() {
        val dummyRequestId = "a_request_id"
        purchasingServiceProvider.getPurchaseUpdatesRequestId = dummyRequestId

        underTest.queryPurchases(
            expectedOnSuccess,
            unexpectedOnError
        )

        assertThat(purchasingServiceProvider.getPurchaseUpdatesCalled).isTrue

        val receipts = listOf(getReceipt("receipt_id_1"), getReceipt("receipt_id_2"))
        val response = getDummyPurchaseUpdatesResponse(dummyRequestId)

        underTest.onPurchaseUpdatesResponse(response)

        assertThat(receivedReceipts).isNotNull
        assertThat(receivedReceipts).isNotEmpty
        assertThat(receivedReceipts).hasSameSizeAs(receipts)

        receipts.forEach { expected ->
            val filteredReceipt = receivedReceipts!!.filter {
                it.isCanceled == expected.isCanceled &&
                    it.receiptId == expected.receiptId &&
                    it.cancelDate == expected.cancelDate &&
                    it.productType == expected.productType &&
                    it.purchaseDate == expected.purchaseDate &&
                    it.sku == expected.sku
            }
            assertThat(filteredReceipt).isNotNull
        }

        assertThat(receivedUserData).isEqualTo(response.userData)
    }

    @Test
    fun `if there are no receipts when successfully querying purchases, it returns an emtpy list`() {
        val dummyRequestId = "a_request_id"
        purchasingServiceProvider.getPurchaseUpdatesRequestId = dummyRequestId

        underTest.queryPurchases(
            expectedOnSuccess,
            unexpectedOnError
        )

        assertThat(purchasingServiceProvider.getPurchaseUpdatesCalled).isTrue

        val response = getDummyPurchaseUpdatesResponse(dummyRequestId, receipts = emptyList())
        underTest.onPurchaseUpdatesResponse(response)

        assertThat(receivedReceipts).isNotNull
        assertThat(receivedReceipts).isEmpty()
        assertThat(receivedUserData).isEqualTo(response.userData)
    }

    @Test
    fun `FAILED response status when querying purchases returns a store problem error`() {
        val dummyRequestId = "a_request_id"
        purchasingServiceProvider.getPurchaseUpdatesRequestId = dummyRequestId

        underTest.queryPurchases(
            unexpectedOnSuccess,
            expectedOnError
        )

        assertThat(purchasingServiceProvider.getPurchaseUpdatesCalled).isTrue

        val response = getDummyPurchaseUpdatesResponse(
            dummyRequestId,
            requestStatus = PurchaseUpdatesResponse.RequestStatus.FAILED,
            receipts = emptyList()
        )
        underTest.onPurchaseUpdatesResponse(response)

        assertStoreProblemError()
    }

    @Test
    fun `NOT_SUPPORTED response status when querying purchases returns a store problem error`() {
        val dummyRequestId = "a_request_id"
        purchasingServiceProvider.getPurchaseUpdatesRequestId = dummyRequestId

        underTest.queryPurchases(
            unexpectedOnSuccess,
            expectedOnError
        )

        assertThat(purchasingServiceProvider.getPurchaseUpdatesCalled).isTrue

        val response = getDummyPurchaseUpdatesResponse(
            dummyRequestId,
            requestStatus = PurchaseUpdatesResponse.RequestStatus.NOT_SUPPORTED,
            receipts = emptyList()
        )
        underTest.onPurchaseUpdatesResponse(response)

        assertStoreProblemError()
    }

    @Test
    fun `callbacks are invoked just once`() {
        val dummyRequestId = "a_request_id"
        purchasingServiceProvider.getPurchaseUpdatesRequestId = dummyRequestId

        var receivedCount = 0
        underTest.queryPurchases(
            onSuccess = { _, _ ->
                receivedCount++
            },
            unexpectedOnError
        )

        assertThat(purchasingServiceProvider.getPurchaseUpdatesCalled).isTrue

        val response = getDummyPurchaseUpdatesResponse(dummyRequestId)

        underTest.onPurchaseUpdatesResponse(response)
        underTest.onPurchaseUpdatesResponse(response)

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
        purchasingServiceProvider.getPurchaseUpdatesRequestId = dummyRequestId
        underTest.queryPurchases(
            onSuccess = { _, _ -> throw expectedException },
            unexpectedOnError
        )

        assertThat(purchasingServiceProvider.getPurchaseUpdatesCalled).isTrue

        val response = getDummyPurchaseUpdatesResponse(dummyRequestId)

        try {
            underTest.onPurchaseUpdatesResponse(response)
        } catch (e: Exception) {
            receivedException = e
        }

        assertThat(receivedException).isNotNull()
        assertThat(receivedLoggedException).isNotNull()
        assertThat(expectedException).isEqualTo(receivedException)
        assertThat(expectedException).isEqualTo(receivedLoggedException)
    }

    private fun assertStoreProblemError() {
        assertThat(receivedReceipts).isNull()
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
    }

    private fun getDummyPurchaseUpdatesResponse(
        expectedRequestId: String = "${System.currentTimeMillis()}",
        requestStatus: PurchaseUpdatesResponse.RequestStatus = PurchaseUpdatesResponse.RequestStatus.SUCCESSFUL,
        receipts: List<Receipt> = listOf(getReceipt("receipt_id_1"), getReceipt("receipt_id_2")),
    ): PurchaseUpdatesResponse {
        val userData = dummyUserData()

        val builder = PurchaseUpdatesResponseBuilder()
            .setRequestId(RequestId.fromString(expectedRequestId))
            .setRequestStatus(requestStatus)
            .setUserData(userData)
            .setReceipts(receipts)
            .setHasMore(false)

        return PurchaseUpdatesResponse(builder)
    }

    private fun getReceipt(
        receiptId: String = "receipt_id_1",
        sku: String = "sku"
    ): Receipt {
        return ReceiptBuilder()
            .setReceiptId(receiptId)
            .setCancelDate(Date())
            .setProductType(ProductType.SUBSCRIPTION)
            .setPurchaseDate(Date())
            .setSku(sku)
            .build()
    }
}
