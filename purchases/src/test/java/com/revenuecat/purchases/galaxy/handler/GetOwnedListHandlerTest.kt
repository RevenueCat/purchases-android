package com.revenuecat.purchases.galaxy.handler

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.galaxy.GalaxyStrings
import com.revenuecat.purchases.galaxy.IAPHelperProvider
import com.revenuecat.purchases.galaxy.constants.GalaxyErrorCode
import com.revenuecat.purchases.galaxy.utils.GalaxySerialOperation
import com.samsung.android.sdk.iap.lib.vo.ErrorVo
import com.samsung.android.sdk.iap.lib.vo.OwnedProductVo
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.fail
import org.assertj.core.api.Assertions.assertThat
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GetOwnedListHandlerTest {

    private lateinit var iapHelperProvider: IAPHelperProvider
    private lateinit var getOwnedListHandler: GetOwnedListHandler

    private val unexpectedOnSuccess: (ArrayList<OwnedProductVo>) -> Unit = { fail("Expected onError to be called") }
    private val unexpectedOnError: (PurchasesError) -> Unit = { fail("Expected onSuccess to be called") }

    @BeforeTest
    fun setup() {
        iapHelperProvider = mockk(relaxed = true)
        getOwnedListHandler = GetOwnedListHandler(iapHelperProvider)
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `getOwnedList dispatches request with converted subscription product type`() {
        val productTypeSlot = slot<String>()
        every { iapHelperProvider.getOwnedList(capture(productTypeSlot), any()) } returns true

        val onSuccess = mockk<(ArrayList<OwnedProductVo>) -> Unit>(relaxed = true)
        val onError = mockk<(PurchasesError) -> Unit>(relaxed = true)

        getOwnedListHandler.getOwnedList(
            productType = ProductType.SUBS,
            onSuccess = onSuccess,
            onError = onError,
        )

        verify(exactly = 1) { iapHelperProvider.getOwnedList(any(), getOwnedListHandler) }
        assertThat(productTypeSlot.captured).isEqualTo("subscription")
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `getOwnedList dispatches request with converted one-time purchase product type`() {
        val productTypeSlot = slot<String>()
        every { iapHelperProvider.getOwnedList(capture(productTypeSlot), any()) } returns true

        val onSuccess = mockk<(ArrayList<OwnedProductVo>) -> Unit>(relaxed = true)
        val onError = mockk<(PurchasesError) -> Unit>(relaxed = true)

        getOwnedListHandler.getOwnedList(
            productType = ProductType.INAPP,
            onSuccess = onSuccess,
            onError = onError,
        )

        verify(exactly = 1) { iapHelperProvider.getOwnedList(any(), getOwnedListHandler) }
        assertThat(productTypeSlot.captured).isEqualTo("item")
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `getOwnedList dispatches request with all product types when product type is null`() {
        val productTypeSlot = slot<String>()
        every { iapHelperProvider.getOwnedList(capture(productTypeSlot), any()) } returns true

        getOwnedListHandler.getOwnedList(
            productType = null,
            onSuccess = mockk(relaxed = true),
            onError = unexpectedOnError,
        )

        verify(exactly = 1) { iapHelperProvider.getOwnedList(any(), getOwnedListHandler) }
        assertThat(productTypeSlot.captured).isEqualTo("all")
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `getOwnedList errors when another request is in flight request`() {
        every { iapHelperProvider.getOwnedList(any(), any()) } returns true

        getOwnedListHandler.getOwnedList(
            productType = ProductType.INAPP,
            onSuccess = mockk(relaxed = true),
            onError = unexpectedOnError,
        )

        var receivedError: PurchasesError? = null
        getOwnedListHandler.getOwnedList(
            productType = ProductType.SUBS,
            onSuccess = unexpectedOnSuccess,
            onError = { receivedError = it },
        )

        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.OperationAlreadyInProgressError)
        verify(exactly = 1) { iapHelperProvider.getOwnedList(any(), any()) }
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `getOwnedList errors when product type is invalid`() {
        var receivedError: PurchasesError? = null

        getOwnedListHandler.getOwnedList(
            productType = ProductType.UNKNOWN,
            onSuccess = unexpectedOnSuccess,
            onError = { receivedError = it },
        )

        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.UnsupportedError)
        assertThat(receivedError?.underlyingErrorMessage)
            .isEqualTo(GalaxyStrings.REQUEST_OWNED_LIST_INVALID_PRODUCT_TYPE)
        verify(exactly = 0) { iapHelperProvider.getOwnedList(any(), any()) }
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `getOwnedList errors when Galaxy Store rejects request and clears in-flight request`() {
        every { iapHelperProvider.getOwnedList(any(), any()) } returnsMany listOf(false, true)
        var receivedError: PurchasesError? = null

        getOwnedListHandler.getOwnedList(
            productType = ProductType.SUBS,
            onSuccess = unexpectedOnSuccess,
            onError = { receivedError = it },
        )

        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
        assertThat(receivedError?.underlyingErrorMessage)
            .isEqualTo(GalaxyStrings.GALAXY_STORE_FAILED_TO_ACCEPT_OWNED_LIST_REQUEST)
        verify(exactly = 1) { iapHelperProvider.getOwnedList(any(), any()) }

        getOwnedListHandler.getOwnedList(
            productType = ProductType.SUBS,
            onSuccess = mockk(relaxed = true),
            onError = unexpectedOnError,
        )

        verify(exactly = 2) { iapHelperProvider.getOwnedList(any(), any()) }
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `onGetOwnedProducts success forwards non-null products and clears in-flight request`() {
        every { iapHelperProvider.getOwnedList(any(), any()) } returnsMany listOf(true, true)
        val ownedProduct1 = mockk<OwnedProductVo>()
        val ownedProduct2 = mockk<OwnedProductVo>()
        var receivedProducts: ArrayList<OwnedProductVo>? = null

        getOwnedListHandler.getOwnedList(
            productType = ProductType.INAPP,
            onSuccess = { receivedProducts = it },
            onError = unexpectedOnError,
        )

        val successErrorVo = mockk<ErrorVo> {
            every { errorCode } returns GalaxyErrorCode.IAP_ERROR_NONE.code
        }
        getOwnedListHandler.onGetOwnedProducts(
            successErrorVo,
            arrayListOf(ownedProduct1, null, ownedProduct2),
        )

        assertThat(receivedProducts).containsExactly(ownedProduct1, ownedProduct2)

        getOwnedListHandler.getOwnedList(
            productType = ProductType.INAPP,
            onSuccess = mockk(relaxed = true),
            onError = unexpectedOnError,
        )

        verify(exactly = 2) { iapHelperProvider.getOwnedList(any(), any()) }
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `onGetOwnedProducts error forwards mapped error and clears in-flight request`() {
        every { iapHelperProvider.getOwnedList(any(), any()) } returnsMany listOf(true, true)
        var receivedError: PurchasesError? = null

        getOwnedListHandler.getOwnedList(
            productType = ProductType.SUBS,
            onSuccess = unexpectedOnSuccess,
            onError = { receivedError = it },
        )

        val failingErrorVo = mockk<ErrorVo> {
            every { errorCode } returns GalaxyErrorCode.IAP_ERROR_NETWORK_NOT_AVAILABLE.code
            every { errorString } returns "Network unavailable"
        }

        getOwnedListHandler.onGetOwnedProducts(failingErrorVo, arrayListOf())

        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.NetworkError)
        assertThat(receivedError?.underlyingErrorMessage).isEqualTo("Network unavailable")

        getOwnedListHandler.getOwnedList(
            productType = ProductType.SUBS,
            onSuccess = mockk(relaxed = true),
            onError = unexpectedOnError,
        )

        verify(exactly = 2) { iapHelperProvider.getOwnedList(any(), any()) }
    }
}
