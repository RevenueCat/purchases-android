package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.data.LogInResult
import com.revenuecat.purchases.models.StoreTransaction
import io.mockk.every
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


        var result1: Offerings? = null
        var result2: Offerings? = null
        var exception: Throwable? = null
        runCatching {
            result1 = purchases.awaitSyncAttributesAndOfferingsIfNeeded()
            result2 = purchases.awaitSyncAttributesAndOfferingsIfNeeded()
        }.onFailure {
            exception = it
        }

        verify(exactly = 1) {
            mockSubscriberAttributesManager.synchronizeSubscriberAttributesForAllUsers(
                currentAppUserID = any(),
                completion = any(),
            )
        }

        verify(exactly = 1) {
            mockOfferingsManager.getOfferings(
                appUserID = any(),
                appInBackground = any(),
                onError = any(),
                onSuccess = any(),
                fetchCurrent = true
            )
        }

        assertThat(result1).isNotNull
        assertThat(result2).isNull()
        assertThat(exception).isNotNull()
        assertThat(exception).isInstanceOf(PurchasesException::class.java)
        assertThat((exception as PurchasesException).code).isEqualTo(PurchasesErrorCode.SyncingAttributesRateLimitReached)
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

}
