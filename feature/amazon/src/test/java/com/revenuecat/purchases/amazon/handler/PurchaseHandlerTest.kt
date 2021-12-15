package com.revenuecat.purchases.amazon.handler

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amazon.device.iap.internal.model.PurchaseResponseBuilder
import com.amazon.device.iap.model.PurchaseResponse
import com.amazon.device.iap.model.Receipt
import com.amazon.device.iap.model.RequestId
import com.amazon.device.iap.model.UserData
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.amazon.helpers.PurchasingServiceProviderForTest
import com.revenuecat.purchases.amazon.helpers.dummyAmazonProduct
import com.revenuecat.purchases.amazon.helpers.dummyReceipt
import com.revenuecat.purchases.amazon.helpers.dummyUserData
import com.revenuecat.purchases.amazon.toStoreProduct
import com.revenuecat.purchases.models.StoreProduct
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

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

    @Before
    fun setup() {
        purchasingServiceProvider = PurchasingServiceProviderForTest()
        underTest = PurchaseHandler(purchasingServiceProvider)
    }

    @Test
    fun `making a successful purchase returns the right values`() {
        val dummyRequestId = "a_request_id"
        purchasingServiceProvider.getPurchaseRequestId = dummyRequestId

        underTest.purchase(
            appUserID = "app_user_id",
            storeProduct = dummyStoreProduct(),
            presentedOfferingIdentifier = null,
            expectedOnSuccess,
            unexpectedOnError
        )

        assertThat(purchasingServiceProvider.purchaseCalled).isTrue

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
            appUserID = "app_user_id",
            storeProduct = dummyStoreProduct(),
            presentedOfferingIdentifier = null,
            unexpectedOnSuccess,
            expectedOnError
        )

        assertThat(purchasingServiceProvider.purchaseCalled).isTrue

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
            appUserID = "app_user_id",
            storeProduct = dummyStoreProduct(),
            presentedOfferingIdentifier = null,
            unexpectedOnSuccess,
            expectedOnError
        )

        assertThat(purchasingServiceProvider.purchaseCalled).isTrue

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
            appUserID = "app_user_id",
            storeProduct = dummyStoreProduct(),
            presentedOfferingIdentifier = null,
            unexpectedOnSuccess,
            expectedOnError
        )

        assertThat(purchasingServiceProvider.purchaseCalled).isTrue

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
            appUserID = "app_user_id",
            storeProduct = dummyStoreProduct(),
            presentedOfferingIdentifier = null,
            unexpectedOnSuccess,
            expectedOnError
        )

        assertThat(purchasingServiceProvider.purchaseCalled).isTrue

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
            appUserID = "app_user_id",
            storeProduct = dummyStoreProduct(),
            presentedOfferingIdentifier = null,
            onSuccess = { _, _ ->
                receivedCount++
            },
            unexpectedOnError
        )

        assertThat(purchasingServiceProvider.purchaseCalled).isTrue

        val response = getDummyPurchaseResponse(dummyRequestId)
        underTest.onPurchaseResponse(response)
        underTest.onPurchaseResponse(response)

        assertThat(receivedCount).isOne
    }

    private fun dummyStoreProduct(): StoreProduct {
        return dummyAmazonProduct().toStoreProduct("US")
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
}
