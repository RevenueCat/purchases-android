package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.data.LoginResult
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.random.Random

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class, ExperimentalCoroutinesApi::class)
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

        var result: LoginResult? = null
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
}
