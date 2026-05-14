package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.data.LogInResult
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.utils.STUB_PRODUCT_IDENTIFIER
import com.revenuecat.purchases.utils.stubOfferings
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
internal class PurchasesCoroutinesTest : BasePurchasesTest() {

    // region awaitCustomerInfo
    @Test
    fun `retrieve customer info - Success`() = runTest {
        mockCustomerInfoHelper()

        val result = purchases.awaitCustomerInfo()

        verify(exactly = 1) {
            mockCustomerInfoHelper.retrieveCustomerInfo(
                appUserId,
                any(),
                any(),
                any(),
                true,
                any(),
            )
        }
        assertThat(result).isNotNull
    }

    @Test
    fun `retrieve customer info - Success - customer info matches expectations`() = runTest {
        mockCustomerInfoHelper()

        val result = purchases.awaitCustomerInfo()

        verify(exactly = 1) {
            mockCustomerInfoHelper.retrieveCustomerInfo(
                appUserId,
                any(),
                any(),
                any(),
                true,
                any(),
            )
        }
        assertThat(result).isNotNull
        assertThat(result).isEqualTo(mockInfo)
    }

    @Test
    fun `retrieve customer info - CustomerInfoError`() = runTest {
        mockCustomerInfoHelper(PurchasesError(PurchasesErrorCode.CustomerInfoError, "Customer info error"))

        var result: CustomerInfo? = null
        var exception: Throwable? = null
        runCatching {
            result = purchases.awaitCustomerInfo()
        }.onFailure {
            exception = it
        }

        verify(exactly = 1) {
            mockCustomerInfoHelper.retrieveCustomerInfo(
                appUserId,
                any(),
                any(),
                any(),
                true,
                any(),
            )
        }

        assertThat(result).isNull()
        assertThat(exception).isNotNull
        assertThat(exception).isInstanceOf(PurchasesException::class.java)
        assertThat((exception as PurchasesException).code).isEqualTo(PurchasesErrorCode.CustomerInfoError)
    }

    // endregion

    // region awaitLogIn
    @Test
    fun `logIn - Success`() = runTest {
        val newAppUserId = "newFakeUserID"
        val mockCreated = Random.nextBoolean()
        every {
            mockIdentityManager.logIn(newAppUserId, onSuccess = captureLambda(), any())
        } answers {
            lambda<(CustomerInfo, Boolean) -> Unit>().captured.invoke(mockInfo, mockCreated)
        }
        mockOfferingsManagerFetchOfferings(newAppUserId)

        val result = purchases.awaitLogIn(newAppUserId)

        verify(exactly = 1) {
            mockIdentityManager.logIn(
                newAppUserId,
                any(),
                any(),
            )
        }
        assertThat(result).isNotNull
    }

    @Test
    fun `logIn - Success - customer info matches expectations`() = runTest {
        val newAppUserId = "newFakeUserID"
        val mockCreated = Random.nextBoolean()
        every {
            mockIdentityManager.logIn(newAppUserId, onSuccess = captureLambda(), any())
        } answers {
            lambda<(CustomerInfo, Boolean) -> Unit>().captured.invoke(mockInfo, mockCreated)
        }
        mockOfferingsManagerFetchOfferings(newAppUserId)

        val result = purchases.awaitLogIn(newAppUserId)

        verify(exactly = 1) {
            mockIdentityManager.logIn(
                newAppUserId,
                any(),
                any(),
            )
        }
        assertThat(result).isNotNull
        assertThat(result.customerInfo).isEqualTo(mockInfo)
        assertThat(result.created).isEqualTo(mockCreated)
    }

    @Test
    fun `logIn - CustomerInfoError`() = runTest {
        val error = PurchasesError(PurchasesErrorCode.CustomerInfoError, "Customer info error")
        val newAppUserId = "newFakeUserID"
        every {
            mockIdentityManager.logIn(newAppUserId, any(), onError = captureLambda())
        } answers {
            lambda<(PurchasesError) -> Unit>().captured.invoke(error)
        }
        mockOfferingsManagerFetchOfferings(newAppUserId)

        var result: LogInResult? = null
        var exception: Throwable? = null
        runCatching {
            result = purchases.awaitLogIn(newAppUserId)
        }.onFailure {
            exception = it
        }

        verify(exactly = 1) {
            mockIdentityManager.logIn(
                newAppUserId,
                any(),
                any(),
            )
        }

        assertThat(result).isNull()
        assertThat(exception).isNotNull
        assertThat(exception).isInstanceOf(PurchasesException::class.java)
        assertThat((exception as PurchasesException).code).isEqualTo(PurchasesErrorCode.CustomerInfoError)
    }

    // endregion

    // region awaitLogOut
    @Test
    fun `logOut - Success`() = runTest {
        every {
            mockIdentityManager.logOut(captureLambda())
        } answers {
            lambda<(PurchasesError?) -> Unit>().captured.invoke(null)
        }
        mockOfferingsManagerFetchOfferings(appUserId)

        val result = purchases.awaitLogOut()

        verify(exactly = 1) {
            mockIdentityManager.logOut(
                any(),
            )
        }
        assertThat(result).isNotNull
    }

    @Test
    fun `logOut - Success - customer info matches expectations`() = runTest {
        val afterLogOutCustomerInfo = mockk<CustomerInfo>()
        every {
            mockIdentityManager.logOut(captureLambda())
        } answers {
            lambda<(PurchasesError?) -> Unit>().captured.invoke(null)
        }
        mockOfferingsManagerFetchOfferings(appUserId)
        mockCustomerInfoHelper(mockedCustomerInfo = afterLogOutCustomerInfo)
        val result = purchases.awaitLogOut()

        verify(exactly = 1) {
            mockIdentityManager.logOut(
                any(),
            )
        }
        assertThat(result).isNotNull
        assertThat(result).isEqualTo(afterLogOutCustomerInfo)
    }

    @Test
    fun `logOut - CustomerInfoError`() = runTest {
        val error = PurchasesError(PurchasesErrorCode.CustomerInfoError, "Customer info error")
        val newAppUserId = "newFakeUserID"
        every {
            mockIdentityManager.logOut(captureLambda())
        } answers {
            lambda<(PurchasesError?) -> Unit>().captured.invoke(error)
        }
        mockOfferingsManagerFetchOfferings(newAppUserId)

        var result: CustomerInfo? = null
        var exception: Throwable? = null
        runCatching {
            result = purchases.awaitLogOut()
        }.onFailure {
            exception = it
        }

        verify(exactly = 1) {
            mockIdentityManager.logOut(
                any(),
            )
        }

        assertThat(result).isNull()
        assertThat(exception).isNotNull
        assertThat(exception).isInstanceOf(PurchasesException::class.java)
        assertThat((exception as PurchasesException).code).isEqualTo(PurchasesErrorCode.CustomerInfoError)
    }

    // endregion

    // region awaitSyncAttributesAndOfferingsIfNeeded

    @Test
    fun `sync attributes and offerings if needed - Success`() = runTest {
        every {
            mockSubscriberAttributesManager.synchronizeSubscriberAttributesForAllUsers(any(), captureLambda())
        } answers {
            lambda<() -> Unit>().captured.invoke()
        }

        every {
            mockOfferingsManager.getOfferings(any(), any(), any(), captureLambda(), any())
        } answers {
            lambda<(Offerings?) -> Unit>().captured.invoke(mockOfferings)
        }


        var result: Offerings? = null
        var exception: Throwable? = null
        runCatching {
            result = purchases.awaitSyncAttributesAndOfferingsIfNeeded()
        }.onFailure {
            exception = it
        }

        verify(exactly = 1) {
            mockSubscriberAttributesManager.synchronizeSubscriberAttributesForAllUsers(
                currentAppUserID = any(),
                completion = captureLambda(),
            )
        }

        verify(exactly = 1) {
            mockOfferingsManager.getOfferings(
                appUserID = any(),
                appInBackground = any(),
                onError = any(),
                onSuccess = captureLambda(),
                fetchCurrent = true
            )
        }

        assertThat(result).isNotNull
        assertThat(exception).isNull()
    }

    @Test
    fun `sync attributes and offerings if needed - SyncingAttributesRateLimitReached`() = runTest {
        every {
            mockSubscriberAttributesManager.synchronizeSubscriberAttributesForAllUsers(any(), captureLambda())
        } answers {
            lambda<() -> Unit>().captured.invoke()
        }

        every {
            mockOfferingsManager.getOfferings(any(), any(), any(), captureLambda(), any())
        } answers {
            lambda<(Offerings?) -> Unit>().captured.invoke(mockOfferings)
        }


        val result1 = purchases.awaitSyncAttributesAndOfferingsIfNeeded()
        val result2 = purchases.awaitSyncAttributesAndOfferingsIfNeeded()
        val result3 = purchases.awaitSyncAttributesAndOfferingsIfNeeded()
        val result4 = purchases.awaitSyncAttributesAndOfferingsIfNeeded()
        val result5 = purchases.awaitSyncAttributesAndOfferingsIfNeeded()
        val result6 = purchases.awaitSyncAttributesAndOfferingsIfNeeded()

        verify(exactly = 5) {
            mockSubscriberAttributesManager.synchronizeSubscriberAttributesForAllUsers(
                currentAppUserID = any(),
                completion = any(),
            )
        }

        verify(exactly = 5) {
            mockOfferingsManager.getOfferings(
                appUserID = any(),
                appInBackground = any(),
                onError = any(),
                onSuccess = any(),
                fetchCurrent = true
            )
        }

        verify(exactly = 1) {
            mockOfferingsManager.getOfferings(
                appUserID = any(),
                appInBackground = any(),
                onError = any(),
                onSuccess = any(),
                fetchCurrent = false
            )
        }
    }

    // endregion

    // region awaitSyncPurchases

    @Test
    fun `sync purchases - Success`() = runTest {
        val afterSyncingCustomerInfo = mockk<CustomerInfo>(relaxed = true)
        every {
            mockSyncPurchasesHelper.syncPurchases(
                isRestore = false,
                appInBackground = false,
                onSuccess = captureLambda(),
                onError = any()
            )
        } answers {
            lambda<(CustomerInfo) -> Unit>().captured.also {
                it.invoke(afterSyncingCustomerInfo)
            }
        }

        val result: CustomerInfo = purchases.awaitSyncPurchases()

        verify(exactly = 1) {
            mockSyncPurchasesHelper.syncPurchases(
                isRestore = false,
                appInBackground = false,
                onSuccess = captureLambda(),
                onError = any()
            )
        }
        assertThat(result).isNotNull
    }

    @Test
    fun `sync purchases - Success - customer info matches expectations`() = runTest {
        val afterSyncingCustomerInfo = mockk<CustomerInfo>(relaxed = true)
        every {
            mockSyncPurchasesHelper.syncPurchases(
                isRestore = false,
                appInBackground = false,
                onSuccess = captureLambda(),
                onError = any()
            )
        } answers {
            lambda<(CustomerInfo) -> Unit>().captured.also {
                it.invoke(afterSyncingCustomerInfo)
            }
        }

        val result: CustomerInfo = purchases.awaitSyncPurchases()

        verify(exactly = 1) {
            mockSyncPurchasesHelper.syncPurchases(
                isRestore = false,
                appInBackground = false,
                onSuccess = captureLambda(),
                onError = any()
            )
        }
        assertThat(result).isNotNull
        assertThat(result).isEqualTo(afterSyncingCustomerInfo)
    }

    @Test
    fun `sync purchases - CustomerInfoError`() = runTest {
        val error = PurchasesError(PurchasesErrorCode.CustomerInfoError, "Customer info error")
        every {
            mockSyncPurchasesHelper.syncPurchases(
                isRestore = false,
                appInBackground = false,
                onSuccess = any(),
                onError = captureLambda()
            )
        } answers {
            lambda<(PurchasesError) -> Unit>().captured.also {
                it.invoke(error)
            }
        }


        var result: CustomerInfo? = null
        var exception: Throwable? = null
        runCatching {
            result = purchases.awaitSyncPurchases()
        }.onFailure {
            exception = it
        }

        verify(exactly = 1) {
            mockSyncPurchasesHelper.syncPurchases(
                isRestore = false,
                appInBackground = false,
                onSuccess = captureLambda(),
                onError = any()
            )
        }

        assertThat(result).isNull()
        assertThat(exception).isNotNull
        assertThat(exception).isInstanceOf(PurchasesException::class.java)
        assertThat((exception as PurchasesException).code).isEqualTo(PurchasesErrorCode.CustomerInfoError)
    }
    
    // endregion

    // region awaitSetAppstackAttributionParams

    @Test
    fun `setAppstackAttributionParams - Success`() = runTest {
        val data = mapOf("appstack_id" to "test_id")
        every {
            mockSubscriberAttributesManager.setAppstackAttributionParams(appUserId, data, any())
        } just Runs

        every {
            mockSubscriberAttributesManager.synchronizeSubscriberAttributesForAllUsers(any(), captureLambda())
        } answers {
            lambda<() -> Unit>().captured.invoke()
        }

        every {
            mockOfferingsManager.getOfferings(any(), any(), any(), captureLambda(), any())
        } answers {
            lambda<(Offerings?) -> Unit>().captured.invoke(mockOfferings)
        }

        val result = purchases.awaitSetAppstackAttributionParams(data)

        verify(exactly = 1) {
            mockSubscriberAttributesManager.setAppstackAttributionParams(appUserId, data, any())
        }
        assertThat(result).isNotNull
        assertThat(result).isEqualTo(mockOfferings)
    }

    @Test
    fun `setAppstackAttributionParams - Error`() = runTest {
        val data = mapOf("appstack_id" to "test_id")
        val error = PurchasesError(PurchasesErrorCode.UnknownBackendError, "Backend error")
        every {
            mockSubscriberAttributesManager.setAppstackAttributionParams(appUserId, data, any())
        } just Runs

        every {
            mockSubscriberAttributesManager.synchronizeSubscriberAttributesForAllUsers(any(), captureLambda())
        } answers {
            lambda<() -> Unit>().captured.invoke()
        }

        every {
            mockOfferingsManager.getOfferings(any(), any(), captureLambda(), any(), any())
        } answers {
            lambda<(PurchasesError) -> Unit>().captured.invoke(error)
        }

        var result: Offerings? = null
        var exception: Throwable? = null
        runCatching {
            result = purchases.awaitSetAppstackAttributionParams(data)
        }.onFailure {
            exception = it
        }

        verify(exactly = 1) {
            mockSubscriberAttributesManager.setAppstackAttributionParams(appUserId, data, any())
        }
        assertThat(result).isNull()
        assertThat(exception).isNotNull
        assertThat(exception).isInstanceOf(PurchasesException::class.java)
        assertThat((exception as PurchasesException).code).isEqualTo(PurchasesErrorCode.UnknownBackendError)
    }

    // endregion

    // region awaitGetAmazonLWAConsentStatus

    @Test
    fun `getAmazonLWAConsentStatus - success`() = runTest {
        every {
            mockBillingAbstract.getAmazonLWAConsentStatus(
                onSuccess = captureLambda(),
                onError = any()
            )
        } answers {
            lambda<(AmazonLWAConsentStatus) -> Unit>().captured.also {
                it.invoke(AmazonLWAConsentStatus.CONSENTED)
            }
        }

        var result: AmazonLWAConsentStatus? = null
        var exception: Throwable? = null
        runCatching {
            result = purchases.getAmazonLWAConsentStatus()
        }.onFailure {
            exception = it
        }
        assertThat(result).isEqualTo(AmazonLWAConsentStatus.CONSENTED)
        assertThat(exception).isNull()
    }

    @Test
    fun `getAmazonLWAConsentStatus - Error`() = runTest {
        val error = PurchasesError(PurchasesErrorCode.StoreProblemError, "Store Problem Error")
        every {
            mockBillingAbstract.getAmazonLWAConsentStatus(
                onSuccess = any(),
                onError = captureLambda()
            )
        } answers {
            lambda<(PurchasesError) -> Unit>().captured.also {
                it.invoke(error)
            }
        }

        var result: AmazonLWAConsentStatus? = null
        var exception: Throwable? = null
        runCatching {
            result = purchases.getAmazonLWAConsentStatus()
        }.onFailure {
            exception = it
        }
        assertThat(result).isNull()
        assertThat(exception).isNotNull
        assertThat(exception).isInstanceOf(PurchasesException::class.java)
        assertThat((exception as PurchasesException).code).isEqualTo(PurchasesErrorCode.StoreProblemError)
    }

    // endregion

    // region awaitCurrentOffering

    @Test
    fun `awaitCurrentOffering returns current offering`() = runTest {
        val (_, offerings) = stubOfferings(STUB_PRODUCT_IDENTIFIER)
        mockOfferingsManagerGetOfferings(offerings = offerings)
        val result = Purchases.sharedInstance.awaitCurrentOffering()
        assertThat(result).isEqualTo(offerings.current)
    }

    @Test
    fun `awaitCurrentOffering returns null when no current offering`() = runTest {
        val emptyOfferings = Offerings(null, emptyMap())
        mockOfferingsManagerGetOfferings(offerings = emptyOfferings)
        val result = Purchases.sharedInstance.awaitCurrentOffering()
        assertThat(result).isNull()
    }

    @Test
    fun `awaitCurrentOffering throws PurchasesException on error`() = runTest {
        val error = PurchasesError(PurchasesErrorCode.NetworkError, "fail")
        mockOfferingsManagerGetOfferings(errorGettingOfferings = error)

        var result: Offering? = null
        var exception: Throwable? = null
        runCatching {
            result = Purchases.sharedInstance.awaitCurrentOffering()
        }.onFailure {
            exception = it
        }

        assertThat(result).isNull()
        assertThat(exception).isNotNull
        assertThat(exception).isInstanceOf(PurchasesException::class.java)
        assertThat((exception as PurchasesException).code).isEqualTo(PurchasesErrorCode.NetworkError)
    }

    // endregion

    // region awaitOffering

    @Test
    fun `awaitOffering returns matching offering`() = runTest {
        val (_, offerings) = stubOfferings(STUB_PRODUCT_IDENTIFIER)
        mockOfferingsManagerGetOfferings(offerings = offerings)
        val id = offerings.all.keys.first()
        val result = Purchases.sharedInstance.awaitOffering(id)
        assertThat(result).isEqualTo(offerings[id])
    }

    @Test
    fun `awaitOffering returns null for nonexistent id`() = runTest {
        val (_, offerings) = stubOfferings(STUB_PRODUCT_IDENTIFIER)
        mockOfferingsManagerGetOfferings(offerings = offerings)
        val result = Purchases.sharedInstance.awaitOffering("nonexistent_id")
        assertThat(result).isNull()
    }

    @Test
    fun `awaitOffering throws PurchasesException on error`() = runTest {
        val error = PurchasesError(PurchasesErrorCode.NetworkError, "fail")
        mockOfferingsManagerGetOfferings(errorGettingOfferings = error)

        var result: Offering? = null
        var exception: Throwable? = null
        runCatching {
            result = Purchases.sharedInstance.awaitOffering("any_id")
        }.onFailure {
            exception = it
        }

        assertThat(result).isNull()
        assertThat(exception).isNotNull
        assertThat(exception).isInstanceOf(PurchasesException::class.java)
        assertThat((exception as PurchasesException).code).isEqualTo(PurchasesErrorCode.NetworkError)
    }

    // endregion

}
